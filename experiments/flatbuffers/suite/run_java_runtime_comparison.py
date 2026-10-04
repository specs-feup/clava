#!/usr/bin/env python3
"""Collect matched test-only timings for the fixed Java parser suite.

The Text and Protobuf checkouts are historical controls. The eager checkout
must be an isolated Clava tree, such as eager-release-build/clava. This script
precompiles each stage, then runs the same 116-test Gradle selection three
times in a rotated serial order. It never starts performance runs in parallel.

JUnit testcase durations are the test-only measure. Gradle wall time is recorded
separately and includes Gradle configuration and test-worker startup.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import statistics
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from typing import Any


SCRIPT_ROOT = Path(__file__).resolve().parent
EXPERIMENT_ROOT = SCRIPT_ROOT.parent
RESULTS_ROOT = SCRIPT_ROOT / "results"
PINNED_TEST_IDS_FILE = SCRIPT_ROOT / "java-runtime-test-ids.json"
VALIDATION_ROOT = Path.home() / ".cache" / "ast-flatbuffers-release-validation"
DEFAULT_CHECKOUTS = {
    "eager": VALIDATION_ROOT / "eager-release-build" / "clava",
    "text": VALIDATION_ROOT / "text-build" / "clava",
    "protobuf": VALIDATION_ROOT / "protobuf-build" / "clava",
}
STAGE_ORDER = (
    (1, "text"), (1, "protobuf"), (1, "eager"),
    (2, "protobuf"), (2, "eager"), (2, "text"),
    (3, "eager"), (3, "text"), (3, "protobuf"),
)
EXPECTED_TESTS = 116
EXPECTED_EAGER_RELEASE_TAG = "v18.1.8_5-rc3"
EXPECTED_EAGER_NATIVE_TOOL_SHA256 = "2349d4ec4ad0926249ad6db2d665c7ec3fc253bf3d241e035c1e428c977249e0"
COMPILE_TASK_NAMES = {
    "classes", "compileJava", "compileTestJava", "generateCompleteWire",
    "generateWireBindingInventory", "generateWireReflection", "processResources",
    "processTestResources", "resolveWireRelease", "testClasses",
}
UP_TO_DATE_TASK_STATES = {"UP-TO-DATE", "FROM-CACHE", "NO-SOURCE", "SKIPPED"}
WORKER_AGENT_PREFIXES = ("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun")
WORKER_GC_PREFIXES = ("-Xlog:gc", "-Xloggc", "-verbose:gc")
WORKER_HEAP_PREFIXES = ("-Xms", "-Xmx", "-Xmn")
ISSUE15_FIXTURE = "cxx/issues/clava_issue15.cpp.txt"
DECLARED_ISSUE15_VARIANCE = {
    "text": "movl $$$$10, %eax;movl $$$$20, %ebx",
    "protobuf": "movl $$$$10, %eax;movl $$$$20, %ebx",
    "eager": "movl $10, %eax;movl $20, %ebx",
}
TIMING_BOUNDARY = {
    "test_only": "sum of JUnit XML testcase time attributes for the 116 selected parser tests",
    "gradle_wall": "monotonic elapsed time around one Gradle test process; includes Gradle configuration and test-worker startup",
    "preflight": "testClasses compilation and classpath fingerprinting run before all measured observations",
    "resource_cache": "verified parser resources are symlinked into the run-local JVM temp root before Gradle test timing; no release download or cache staging is included",
}
EMPTY_LOCAL_OPTIONS = (
    b"<SimpleDataStore>\n"
    b"  <name>ClangAstParser Local Options</name>\n"
    b"  <values/>\n"
    b"  <strict>false</strict>\n"
    b"</SimpleDataStore>"
)

sys.dont_write_bytecode = True
sys.path.insert(0, str(EXPERIMENT_ROOT))
sys.path.insert(0, str(EXPERIMENT_ROOT / "validation"))
from benchmark_environment import make_path_without_ccache
from artifact_metadata import installed_tool_metadata


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--eager-checkout", type=Path, default=DEFAULT_CHECKOUTS["eager"])
    parser.add_argument("--text-checkout", type=Path, default=DEFAULT_CHECKOUTS["text"])
    parser.add_argument("--protobuf-checkout", type=Path, default=DEFAULT_CHECKOUTS["protobuf"])
    parser.add_argument("--output-root", type=Path,
                        help="New directory under suite/results; defaults to a UTC timestamped directory.")
    return parser.parse_args()


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_sha256(value: Any) -> str:
    return sha256_bytes(json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8"))


def load_pinned_test_ids() -> list[str]:
    ids = json.loads(PINNED_TEST_IDS_FILE.read_text(encoding="utf-8"))
    if (not isinstance(ids, list) or len(ids) != EXPECTED_TESTS
            or any(not isinstance(test_id, str) or "#" not in test_id for test_id in ids)
            or ids != sorted(set(ids))):
        raise RuntimeError(f"pinned Java test IDs must contain {EXPECTED_TESTS} sorted unique test identities")
    return ids


def tree_manifest(root: Path) -> dict[str, str]:
    if not root.is_dir():
        raise RuntimeError(f"required source tree is missing: {root}")
    return {
        path.relative_to(root).as_posix(): sha256_file(path)
        for path in sorted(root.rglob("*"))
        if path.is_file()
    }


def git(root: Path, *arguments: str) -> str:
    result = subprocess.run(["git", "-C", str(root), *arguments], text=True,
                            capture_output=True, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"git {' '.join(arguments)} failed in {root}: {result.stderr.strip()}")
    return result.stdout.strip()


def git_snapshot(root: Path) -> dict[str, Any] | None:
    if not root.is_dir() or not (root / ".git").exists():
        return None
    patch = subprocess.run(["git", "-C", str(root), "diff", "--binary", "HEAD"],
                           capture_output=True, check=False)
    if patch.returncode != 0:
        raise RuntimeError(f"could not hash working-tree patch in {root}")
    status = git(root, "status", "--porcelain=v1", "--untracked-files=all").splitlines()
    untracked = subprocess.run(
        ["git", "-C", str(root), "ls-files", "--others", "--exclude-standard", "-z"],
        capture_output=True,
        check=False,
    )
    if untracked.returncode != 0:
        raise RuntimeError(f"could not inventory untracked files in {root}")
    untracked_manifest = {}
    for raw_path in untracked.stdout.split(b"\0"):
        if not raw_path:
            continue
        path = root / os.fsdecode(raw_path)
        if path.is_file():
            untracked_manifest[path.relative_to(root).as_posix()] = sha256_file(path)
    return {
        "root": str(root.resolve()),
        "revision": git(root, "rev-parse", "HEAD"),
        "dirty": bool(status),
        "status": status,
        "patch_sha256": sha256_bytes(patch.stdout),
        "untracked_files": untracked_manifest,
        "untracked_manifest_sha256": canonical_sha256(untracked_manifest),
    }


def stage_roots(checkout: Path) -> dict[str, Path]:
    checkout = checkout.resolve()
    return {
        "clava": checkout,
        "specs-java-libs": checkout.parent / "specs-java-libs",
        "lara-framework": checkout.parent / "lara-framework",
        "clang-dumper": checkout.parent / "clang-dumper",
    }


def issue15_line(fixture_root: Path) -> str:
    path = fixture_root / ISSUE15_FIXTURE
    if not path.is_file():
        raise RuntimeError(f"missing assembly golden: {path}")
    matches = [line.strip() for line in path.read_text(encoding="utf-8").splitlines()
               if "movl " in line and "%eax" in line and "%ebx" in line]
    if len(matches) != 1:
        raise RuntimeError(f"expected one Issue 15 assembly line in {path}, found {len(matches)}")
    return matches[0]


def selected_release(stage: str, checkout: Path) -> dict[str, Any]:
    tag_file = checkout / "ClangAstParser" / "clang-dumper-release.tag"
    if not tag_file.is_file():
        raise RuntimeError(f"missing clang-dumper release tag: {tag_file}")
    tag = tag_file.read_text(encoding="utf-8").strip()
    selected = {
        "tag": tag,
        "tag_file_sha256": sha256_file(tag_file),
        "tag_file": str(tag_file.resolve()),
    }
    local_root = Path(tag)
    if not local_root.is_absolute():
        selected["kind"] = "published_release_tag"
        return selected
    if not local_root.is_dir():
        raise RuntimeError(f"local release directory from {tag_file} does not exist: {local_root}")

    manifest_path = local_root / "clang-dumper-release-manifest.json"
    if not manifest_path.is_file():
        if stage == "eager":
            raise RuntimeError(f"eager local release directory has no manifest: {manifest_path}")
        tool_path = next((local_root / name for name in ("tool", "tool.exe")
                          if (local_root / name).is_file()), None)
        if tool_path is None:
            raise RuntimeError(f"historical local release has no tool executable: {local_root}")
        selected.update({
            "kind": "legacy_local_build_without_manifest",
            "release_root": str(local_root.resolve()),
            "manifest_available": False,
            "tool_path": str(tool_path.resolve()),
            "tool_sha256": sha256_file(tool_path),
        })
        return selected
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    tool_assets = [asset for asset in manifest.get("assets", []) if asset.get("kind") == "tool"]
    if not tool_assets:
        raise RuntimeError(f"local release manifest has no tool asset: {manifest_path}")
    system = {"linux": "linux", "darwin": "macos", "windows": "windows"}.get(platform.system().lower())
    machine = platform.machine().lower()
    if system is None:
        raise RuntimeError(f"unsupported host platform for clang-dumper assets: {platform.system()}")
    arch = "arm64" if machine in {"aarch64", "arm64"} else "x86_64" if system == "windows" else "x64"
    selected_asset = next((asset for asset in tool_assets
                           if asset.get("platform") == system and asset.get("arch") == arch), None)
    if selected_asset is None:
        raise RuntimeError(f"local release has no {system}/{arch} tool asset: {manifest_path}")
    tool_path = local_root / selected_asset["filename"]
    if not tool_path.is_file():
        raise RuntimeError(f"local release tool is missing: {tool_path}")
    actual_tool_sha = sha256_file(tool_path)
    if actual_tool_sha.lower() != str(selected_asset.get("sha256", "")).lower():
        raise RuntimeError(f"local release tool hash differs from its manifest: {tool_path}")
    selected.update({
        "kind": "local_build",
        "release_root": str(local_root.resolve()),
        "manifest_sha256": sha256_file(manifest_path),
        "tool_asset": selected_asset,
        "tool_path": str(tool_path.resolve()),
        "tool_sha256": actual_tool_sha,
    })
    return selected


def stage_snapshot(name: str, checkout: Path) -> dict[str, Any]:
    paths = stage_roots(checkout)
    parser_root = checkout / "ClangAstParser"
    source_trees = {
        key: tree_manifest(parser_root / key)
        for key in ("src", "test", "test-resources")
    }
    return {
        "stage": name,
        "checkout": str(checkout.resolve()),
        "repositories": {key: git_snapshot(path) for key, path in paths.items()},
        "clang_ast_parser_source_tree_sha256": canonical_sha256(source_trees["src"]),
        "clang_ast_parser_test_tree_sha256": canonical_sha256(source_trees["test"]),
        "fixture_manifest": source_trees["test-resources"],
        "fixture_manifest_sha256": canonical_sha256(source_trees["test-resources"]),
        "issue15_assembly_line": issue15_line(parser_root / "test-resources"),
        "selected_release": selected_release(name, checkout),
    }


def stable_source_signature(snapshot: dict[str, Any]) -> str:
    return canonical_sha256({
        "repositories": snapshot["repositories"],
        "source_sha256": snapshot["clang_ast_parser_source_tree_sha256"],
        "test_sha256": snapshot["clang_ast_parser_test_tree_sha256"],
        "fixture_sha256": snapshot["fixture_manifest_sha256"],
        "release": snapshot["selected_release"],
    })


def compare_fixture_maps(snapshots: dict[str, dict[str, Any]]) -> dict[str, Any]:
    text_fixtures = snapshots["text"]["fixture_manifest"]
    protobuf_fixtures = snapshots["protobuf"]["fixture_manifest"]
    if text_fixtures != protobuf_fixtures:
        raise RuntimeError("historical Text and Protobuf controls do not have identical test-resource fixtures")

    eager_fixtures = snapshots["eager"]["fixture_manifest"]
    paths = sorted(set(text_fixtures) | set(eager_fixtures))
    eager_differences = [path for path in paths if text_fixtures.get(path) != eager_fixtures.get(path)]
    issue15_lines = {name: snapshots[name]["issue15_assembly_line"] for name in snapshots}
    for name, fragment in DECLARED_ISSUE15_VARIANCE.items():
        if fragment not in issue15_lines[name]:
            raise RuntimeError(f"unexpected Issue 15 assembly golden in {name}: {issue15_lines[name]!r}")

    return {
        "text_vs_protobuf_differences": [],
        "eager_vs_historical_differences": eager_differences,
        "issue15_source_fidelity_change": {
            "file": f"ClangAstParser/test-resources/{ISSUE15_FIXTURE}",
            "reason": "Eager FlatBuffers preserves the inline-assembly source operands as $10/$20; the historical Text and Protobuf goldens retain the escaped $$$$10/$$$$20 rendering.",
            "assembly_line_by_stage": issue15_lines,
            "sha256_by_stage": {
                name: snapshots[name]["fixture_manifest"].get(ISSUE15_FIXTURE)
                for name in snapshots
            },
        },
    }


def make_output_root(requested: Path | None) -> Path:
    if requested is None:
        timestamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        requested = RESULTS_ROOT / f"java-runtime-{timestamp}"
    root = requested.resolve()
    results = RESULTS_ROOT.resolve()
    if root != results and results not in root.parents:
        raise SystemExit(f"output must be under {results}: {root}")
    if root.exists():
        raise SystemExit(f"refusing to reuse an existing output directory: {root}")
    root.mkdir(parents=True)
    return root


def validate_checkouts(checkouts: dict[str, Path]) -> dict[str, Path]:
    resolved = {name: path.resolve() for name, path in checkouts.items()}
    if len(set(resolved.values())) != len(resolved):
        raise SystemExit("eager, Text, and Protobuf checkouts must be distinct")
    for name, checkout in resolved.items():
        if not checkout.is_dir():
            raise SystemExit(f"{name} checkout does not exist: {checkout}")
        if not (checkout / "ClangAstParser" / "build.gradle").is_file():
            raise SystemExit(f"{name} checkout has no ClangAstParser Gradle project: {checkout}")
        for repository_name, repository in stage_roots(checkout).items():
            if repository_name != "clang-dumper" and not (repository / ".git").exists():
                raise SystemExit(f"{name} checkout is missing its {repository_name} Git checkout: {repository}")
        if not (checkout / "gradlew").is_file() and shutil.which("gradle") is None:
            raise SystemExit("Gradle is required on PATH")
    return resolved


def base_environment(path: str, temp_root: Path, run_root: Path | None = None) -> dict[str, str]:
    environment = os.environ.copy()
    environment["PATH"] = path
    environment["CCACHE_DISABLE"] = "true"
    for key in tuple(environment):
        if key.startswith("CCACHE_") and key != "CCACHE_DISABLE":
            environment.pop(key, None)
    environment["TMPDIR"] = str(temp_root.resolve())
    environment["TMP"] = str(temp_root.resolve())
    environment["TEMP"] = str(temp_root.resolve())
    if run_root is not None:
        environment["JAVA_RUNTIME_MATRIX_RUN_DIR"] = str(run_root.resolve())
    else:
        environment.pop("JAVA_RUNTIME_MATRIX_RUN_DIR", None)
    environment.pop("AST_WIRE_FLAT", None)
    environment.pop("AST_WIRE_DENSE_TEXT", None)
    return environment


def stage_build_environment(stage: str, checkout: Path, environment: dict[str, str]) -> dict[str, Any]:
    """Pin any historical schema-generation inputs to that control checkout."""
    build_file = checkout / "ClangAstParser" / "build.gradle"
    build_text = build_file.read_text(encoding="utf-8")
    if "wireNative" not in build_text:
        environment.pop("FLAT_NATIVE", None)
        environment.pop("FLATBUFFERS_ROOT", None)
        return {"FLAT_NATIVE": None, "FLATBUFFERS_ROOT": None}

    native = checkout.parent / "clang-dumper"
    generator = native / "scripts" / "generate_complete_wire.py"
    schema_root = native / "wire" / "v2"
    if not generator.is_file() or not schema_root.is_dir():
        raise RuntimeError(f"{stage} schema-generation inputs are missing from its source checkout: {native}")
    sdk = Path(environment.get("FLATBUFFERS_ROOT") or
                (Path.home() / ".cache" / "ast-flatbuffers-planning" / "flatbuffers")).resolve()
    flatc = sdk / "build-make" / "flatc"
    if not flatc.is_file():
        raise RuntimeError(f"{stage} schema-generation flatc is missing: {flatc}")
    environment["FLAT_NATIVE"] = str(native.resolve())
    environment["FLATBUFFERS_ROOT"] = str(sdk)
    schema_manifest = tree_manifest(schema_root)
    return {
        "FLAT_NATIVE": str(native.resolve()),
        "generator_sha256": sha256_file(generator),
        "schema_manifest_sha256": canonical_sha256(schema_manifest),
        "schema_files": schema_manifest,
        "FLATBUFFERS_ROOT": str(sdk),
        "flatc_sha256": sha256_file(flatc),
    }


def java_runtime_properties(path: str) -> dict[str, str]:
    java = shutil.which("java", path=path)
    if java is None:
        raise RuntimeError("java is missing from the comparison PATH")
    environment = os.environ.copy()
    for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
        environment.pop(name, None)
    result = subprocess.run([java, "-XshowSettings:properties", "-version"], env=environment,
                            capture_output=True, text=True, check=True)
    properties = {}
    for line in result.stdout.splitlines() + result.stderr.splitlines():
        key, separator, value = line.strip().partition("=")
        if separator:
            properties[key.strip()] = value.strip()
    required = ("java.version", "java.io.tmpdir", "user.name")
    if any(name not in properties for name in required):
        raise RuntimeError(f"Java did not report required runtime properties: {required}")
    return {
        "java_binary": str(Path(java).resolve()),
        **{name: properties[name] for name in required},
    }


def runtime_resource_cache_snapshot(java_runtime: dict[str, str]) -> dict[str, Any]:
    source_root = (Path(java_runtime["java.io.tmpdir"])
                   / f"clang_ast_exe_{java_runtime['user.name']}" / "clang-dumper").resolve()
    if not source_root.is_dir():
        raise RuntimeError(f"the warmed Clava parser resource cache is missing: {source_root}")
    release_root = source_root / "releases" / EXPECTED_EAGER_RELEASE_TAG
    installed = installed_tool_metadata(release_root)
    if installed is None:
        raise RuntimeError(f"the eager release is absent from the warmed parser resource cache: {release_root}")
    if installed["native_tool_sha256"] != EXPECTED_EAGER_NATIVE_TOOL_SHA256:
        raise RuntimeError("the warmed eager resource cache has the wrong native tool hash")
    files = tree_manifest(source_root)
    return {
        "source_root": str(source_root),
        "cache_tree_sha256": canonical_sha256(files),
        "cache_file_count": len(files),
        "release_root": str(release_root),
        "release_manifest_sha256": installed["release_manifest_sha256"],
        "native_tool_path": installed["native_tool_path"],
        "native_tool_sha256": installed["native_tool_sha256"],
        "wire_schema_sha256": installed["wire_schema_sha256"],
        "flatbuffers": installed["flatbuffers"],
    }


def resource_cache_tree_sha256(cache_root: Path) -> str:
    return canonical_sha256(tree_manifest(cache_root))


def stage_runtime_resource_cache(run_tmp: Path, java_runtime: dict[str, str],
                                 cache: dict[str, Any]) -> dict[str, Any]:
    home = run_tmp / f"clang_ast_exe_{java_runtime['user.name']}"
    home.mkdir(parents=True, exist_ok=True)
    link = home / "clang-dumper"
    if link.exists() or link.is_symlink():
        raise RuntimeError(f"refusing to replace an existing per-run parser cache: {link}")
    source = Path(cache["source_root"])
    before = resource_cache_tree_sha256(source)
    if before != cache["cache_tree_sha256"]:
        raise RuntimeError("the warmed parser resource cache changed after preflight")
    link.symlink_to(source, target_is_directory=True)
    if link.resolve(strict=True) != source:
        raise RuntimeError(f"per-run parser cache does not resolve to the pinned cache: {link}")
    return {
        "staged_path": str(link),
        "staged_target": str(link.resolve()),
        "cache_tree_sha256_before": before,
    }


def prepare_empty_local_options(checkout: Path, classpath: dict[str, Any]) -> dict[str, Any]:
    """Seed the runtime's default writable-JAR options file before hashing the classpath."""
    main_classes = (checkout / "ClangAstParser" / "build" / "classes" / "java" / "main").resolve()
    classpath_paths = {Path(path).resolve() for path in classpath["test_classpath"]}
    if main_classes not in classpath_paths:
        raise RuntimeError(f"ClangAstParser main classes are absent from the test classpath: {main_classes}")
    options_file = main_classes / "local_options.xml"
    created = not options_file.exists()
    if created:
        options_file.write_bytes(EMPTY_LOCAL_OPTIONS)
    elif options_file.read_bytes() != EMPTY_LOCAL_OPTIONS:
        raise RuntimeError(f"refusing to benchmark with non-default local parser options: {options_file}")
    return {
        "path": str(options_file),
        "sha256": sha256_file(options_file),
        "created_before_classpath_fingerprint": created,
        "content": "empty ClangAstParser Local Options default",
    }


def jvm_environment_audit(environment: dict[str, str]) -> dict[str, str]:
    names = ("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "JAVA_OPTS", "GRADLE_OPTS")
    observed = {name: environment.get(name, "") for name in names}
    for name, value in observed.items():
        args = value.split()
        injected = [argument for argument in args
                    if argument.startswith(WORKER_AGENT_PREFIXES)
                    or argument.startswith(WORKER_GC_PREFIXES)
                    or argument.startswith(WORKER_HEAP_PREFIXES)]
        if injected:
            raise RuntimeError(f"remove JVM timing options from {name} before comparison: {injected}")
    return observed


def run_process(command: list[str], cwd: Path, environment: dict[str, str], log_path: Path) -> dict[str, Any]:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    started = time.perf_counter()
    with log_path.open("w", encoding="utf-8") as log:
        process = subprocess.run(command, cwd=cwd, env=environment, stdout=log,
                                  stderr=subprocess.STDOUT, text=True, check=False)
    return {"return_code": process.returncode, "wall_s": time.perf_counter() - started}


def gradle_base_command(init_script: Path) -> list[str]:
    return ["gradle", "--no-daemon", "--offline", "--no-build-cache", "--max-workers=1", "--console=plain",
            "--init-script", str(init_script)]


def run_preflight(stage: str, checkout: Path, output_root: Path, no_cache_path: str) -> dict[str, Any]:
    stage_root = output_root / "preflight" / stage
    temp_root = stage_root / "tmp"
    temp_root.mkdir(parents=True)
    environment = base_environment(no_cache_path, temp_root)
    build_environment = stage_build_environment(stage, checkout, environment)
    command = gradle_base_command(SCRIPT_ROOT / "java-suite.init.gradle")
    command += ["-p", "ClangAstParser", "testClasses"]
    compile_log = stage_root / "test-classes.log"
    compile_result = run_process(command, checkout, environment, compile_log)
    if compile_result["return_code"] != 0:
        raise RuntimeError(f"{stage} testClasses preflight failed; see {compile_log}")

    classpath_log = stage_root / "classpath.log"
    classpath_command = gradle_base_command(SCRIPT_ROOT / "java-suite.init.gradle")
    classpath_command += ["-p", "ClangAstParser", "printJavaRuntimeMatrixTestClasspath"]
    classpath_result = run_process(classpath_command, checkout, environment, classpath_log)
    if classpath_result["return_code"] != 0:
        raise RuntimeError(f"{stage} test classpath preflight failed; see {classpath_log}")
    classpath_data = parse_runtime_metadata(classpath_log)
    prepared_runtime_file = prepare_empty_local_options(checkout, classpath_data)
    classpath_fingerprint = fingerprint_test_classpath(
        classpath_data["test_classes_dirs"], classpath_data["test_classpath"])
    validate_worker(classpath_data["worker"])
    resolved_release_path = checkout / "ClangAstParser" / "build" / "wire" / "selected-release.json"
    if not resolved_release_path.is_file():
        if stage == "eager":
            raise RuntimeError(f"{stage} testClasses preflight did not resolve its wire release")
        resolved_release = {
            "source": "historical parser release tag and exact local executable hash",
            "selected_release": selected_release(stage, checkout),
        }
        resolved_release_hash = None
        resolved_release_location = None
    else:
        resolved_release = json.loads(resolved_release_path.read_text(encoding="utf-8"))
        resolved_release_hash = sha256_file(resolved_release_path)
        resolved_release_location = str(resolved_release_path.resolve())
    return {
        "test_classes_command": command,
        "test_classes_log": str(compile_log),
        "test_classes_wall_s": compile_result["wall_s"],
        "classpath_command": classpath_command,
        "classpath_log": str(classpath_log),
        "classpath_wall_s": classpath_result["wall_s"],
        "test_classes_dirs": classpath_data["test_classes_dirs"],
        "test_classpath": classpath_data["test_classpath"],
        "worker": classpath_data["worker"],
        "worker_java_io_tmpdir": worker_java_tmpdir(classpath_data["worker"]),
        "classpath_fingerprint": classpath_fingerprint,
        "classpath_artifact_sha256": classpath_fingerprint["sha256"],
        "build_environment": build_environment,
        "prepared_runtime_file": prepared_runtime_file,
        "resolved_release_path": resolved_release_location,
        "resolved_release_sha256": resolved_release_hash,
        "resolved_release": resolved_release,
    }


def parse_runtime_metadata(log_path: Path) -> dict[str, Any]:
    test_classes: list[str] = []
    classpath: list[str] = []
    worker = None
    for line in log_path.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith("JAVA_RUNTIME_MATRIX_TEST_CLASSES\t"):
            test_classes.append(line.split("\t", 1)[1])
        elif line.startswith("JAVA_RUNTIME_MATRIX_TEST_CLASSPATH\t"):
            classpath.append(line.split("\t", 1)[1])
        elif line.startswith("JAVA_RUNTIME_MATRIX_WORKER\t"):
            worker = json.loads(line.split("\t", 1)[1])
    if not test_classes or not classpath or worker is None:
        raise RuntimeError(f"Gradle did not emit complete test classpath metadata: {log_path}")
    return {"test_classes_dirs": test_classes, "test_classpath": classpath, "worker": worker}


def artifact_fingerprint(raw_path: str) -> dict[str, Any]:
    path = Path(raw_path).resolve(strict=True)
    if path.is_file():
        return {"path": str(path), "kind": "file", "files": 1, "sha256": sha256_file(path)}
    if not path.is_dir():
        raise RuntimeError(f"test classpath entry is not a file or directory: {path}")
    digest = hashlib.sha256()
    count = 0
    for file in sorted(item for item in path.rglob("*") if item.is_file()):
        relative = file.relative_to(path).as_posix().encode("utf-8")
        digest.update(relative + b"\0" + bytes.fromhex(sha256_file(file)) + b"\0")
        count += 1
    return {"path": str(path), "kind": "directory", "files": count, "sha256": digest.hexdigest()}


def fingerprint_test_classpath(test_classes_dirs: list[str], classpath: list[str]) -> dict[str, Any]:
    classes = [artifact_fingerprint(path) for path in test_classes_dirs]
    entries = [artifact_fingerprint(path) for path in classpath]
    body = {"test_classes_dirs": classes, "test_classpath": entries}
    return {**body, "sha256": canonical_sha256(body)}


def classpath_fingerprint_errors(
    preflight_sha256: str, pre_run_sha256: str, post_run_sha256: str,
) -> list[str]:
    errors = []
    if pre_run_sha256 != preflight_sha256:
        errors.append("test classpath content differs from the preflight fingerprint before timing")
    if post_run_sha256 != pre_run_sha256:
        errors.append("test classpath content changed during the observation")
    return errors


def validate_worker(worker: dict[str, Any]) -> None:
    if worker.get("jacoco_enabled") is not False:
        raise RuntimeError(f"JaCoCo must be disabled for direct test timing: {worker}")
    if worker.get("max_heap_size") != "512m" or worker.get("max_parallel_forks") != 1:
        raise RuntimeError(f"Java test worker settings differ from the fixed baseline: {worker}")
    args = worker.get("jvm_args", [])
    injected = [arg for arg in args if arg.startswith(WORKER_AGENT_PREFIXES)
                or arg.startswith(WORKER_GC_PREFIXES)
                or arg.startswith(("-Xms", "-Xmn"))
                or arg.startswith("-Xmx") and arg != "-Xmx512m"]
    if injected:
        raise RuntimeError(f"test JVM has profiler or GC options enabled: {injected}")


def worker_java_tmpdir(worker: dict[str, Any]) -> str:
    values = [argument.split("=", 1)[1] for argument in worker.get("jvm_args", [])
              if argument.startswith("-Djava.io.tmpdir=")]
    if len(values) != 1:
        raise RuntimeError(f"test worker must have exactly one pinned java.io.tmpdir: {worker.get('jvm_args')}")
    return str(Path(values[0]).resolve())


def parse_junit_results(xml_root: Path) -> dict[str, Any]:
    if not xml_root.is_dir():
        raise RuntimeError(f"JUnit XML output is missing: {xml_root}")
    cases = []
    for path in sorted(xml_root.glob("**/*.xml")):
        root = ET.parse(path).getroot()
        for case in root.iter("testcase"):
            skipped = case.find("skipped") is not None
            failed = case.find("failure") is not None or case.find("error") is not None
            testcase_id = f"{case.attrib.get('classname', '')}#{case.attrib.get('name', '')}"
            cases.append({
                "id": testcase_id,
                "classname": case.attrib.get("classname", ""),
                "name": case.attrib.get("name", ""),
                "time_s": float(case.attrib.get("time", "0") or 0),
                "status": "failed" if failed else "skipped" if skipped else "passed",
            })
    identities = sorted(case["id"] for case in cases)
    counts = {
        "total": len(cases),
        "passed": sum(case["status"] == "passed" for case in cases),
        "failed": sum(case["status"] == "failed" for case in cases),
        "skipped": sum(case["status"] == "skipped" for case in cases),
    }
    with (xml_root.parent / "testcases.csv").open("w", newline="", encoding="utf-8") as destination:
        writer = csv.DictWriter(destination, fieldnames=("id", "classname", "name", "time_s", "status"))
        writer.writeheader()
        writer.writerows(cases)
    return {
        "counts": counts,
        "identities": identities,
        "identity_sha256": canonical_sha256(identities),
        "testcase_duration_s": sum(case["time_s"] for case in cases),
        "cases": cases,
    }


def parse_task_states(log_path: Path) -> dict[str, str]:
    task_pattern = re.compile(r"^> Task :([^\s]+)(?: ([A-Z-]+))?$")
    states: dict[str, str] = {}
    for line in log_path.read_text(encoding="utf-8", errors="replace").splitlines():
        match = task_pattern.match(line.strip())
        if not match:
            continue
        task = match.group(1).rsplit(":", 1)[-1]
        if task in COMPILE_TASK_NAMES:
            states[match.group(1)] = match.group(2) or "EXECUTED"
    return states


def source_unchanged(before: dict[str, Any], after: dict[str, Any]) -> bool:
    return stable_source_signature(before) == stable_source_signature(after)


def run_observation(
    stage: str,
    round_number: int,
    checkout: Path,
    preflight: dict[str, Any],
    source_before: dict[str, Any],
    expected_test_ids: list[str],
    output_root: Path,
    no_cache_path: str,
    java_runtime: dict[str, str],
    resource_cache: dict[str, Any],
) -> dict[str, Any]:
    ordinal = len(list((output_root / "runs").glob("*"))) + 1
    run_root = output_root / "runs" / f"{ordinal:02d}-round-{round_number}-{stage}"
    run_root.mkdir(parents=True)
    temp_root = run_root / "tmp"
    temp_root.mkdir()
    environment = base_environment(no_cache_path, temp_root, run_root)
    build_environment = stage_build_environment(stage, checkout, environment)
    environment_options = jvm_environment_audit(environment)
    if shutil.which("ccache", path=environment["PATH"]) is not None:
        raise RuntimeError("ccache unexpectedly resolves from the Java comparison PATH")
    staged_cache = stage_runtime_resource_cache(temp_root, java_runtime, resource_cache)

    pre_run_classpath = fingerprint_test_classpath(
        preflight["test_classes_dirs"], preflight["test_classpath"])
    command = gradle_base_command(SCRIPT_ROOT / "java-suite.init.gradle")
    command += ["-p", "ClangAstParser", "test", "-x", "jacocoTestReport", "-x", "jacocoTestCoverageVerification"]
    log_path = run_root / "gradle.log"
    process_result = run_process(command, checkout, environment, log_path)
    junit_root = run_root / "junit-xml"
    junit = parse_junit_results(junit_root)
    installed_tool = None
    runtime_metadata = parse_runtime_metadata(log_path)
    validate_worker(runtime_metadata["worker"])
    actual_java_tmpdir = worker_java_tmpdir(runtime_metadata["worker"])
    if stage == "eager":
        installed_tool = installed_tool_metadata(
            Path(resource_cache["source_root"]) / "releases" / EXPECTED_EAGER_RELEASE_TAG
        )

    actual_classes = runtime_metadata["test_classes_dirs"]
    actual_classpath = runtime_metadata["test_classpath"]
    classpath_match = (
        actual_classes == preflight["test_classes_dirs"]
        and actual_classpath == preflight["test_classpath"]
    )
    task_states = parse_task_states(log_path)
    compile_clean = bool(task_states) and all(state in UP_TO_DATE_TASK_STATES for state in task_states.values())
    source_after = stage_snapshot(stage, checkout)
    source_stable = source_unchanged(source_before, source_after)
    cache_after = resource_cache_tree_sha256(Path(resource_cache["source_root"]))
    release_manifest_after = sha256_file(
        Path(resource_cache["release_root"]) / "clang-dumper-release-manifest.json"
    )
    native_tool_after = sha256_file(Path(resource_cache["native_tool_path"]))
    post_run_classpath = fingerprint_test_classpath(
        preflight["test_classes_dirs"], preflight["test_classpath"])
    classpath_matches_preflight = (
        pre_run_classpath["sha256"] == preflight["classpath_artifact_sha256"])
    classpath_stable = pre_run_classpath["sha256"] == post_run_classpath["sha256"]

    errors = []
    if build_environment != preflight["build_environment"]:
        errors.append("schema-generation build inputs differ from the preflight")
    if process_result["return_code"] != 0:
        errors.append(f"Gradle returned {process_result['return_code']}")
    if actual_java_tmpdir != str(temp_root.resolve()):
        errors.append(f"test worker java.io.tmpdir differs from the run temp root: {actual_java_tmpdir}")
    if cache_after != staged_cache["cache_tree_sha256_before"]:
        errors.append("the pinned parser resource cache changed during the observation")
    if release_manifest_after != resource_cache["release_manifest_sha256"]:
        errors.append("the pinned parser release manifest changed during the observation")
    if native_tool_after != resource_cache["native_tool_sha256"]:
        errors.append("the pinned parser native tool changed during the observation")
    if junit["counts"] != {"total": EXPECTED_TESTS, "passed": EXPECTED_TESTS, "failed": 0, "skipped": 0}:
        errors.append(f"unexpected JUnit counts: {junit['counts']}")
    if junit["identities"] != expected_test_ids:
        errors.append("JUnit test IDs differ from the checked-in 116-test identity")
    if not classpath_match:
        errors.append("measured test classpath differs from the preflight classpath")
    errors.extend(classpath_fingerprint_errors(
        preflight["classpath_artifact_sha256"],
        pre_run_classpath["sha256"],
        post_run_classpath["sha256"],
    ))
    if not compile_clean:
        errors.append(f"compile tasks ran inside the observation: {task_states}")
    if not source_stable:
        errors.append("source revisions, patches, or parser fixtures changed during the observation")
    if not runtime_metadata["worker"].get("jacoco_enabled") is False:
        errors.append("JaCoCo agent was not disabled")
    selected_release = source_before["selected_release"]
    if installed_tool is None:
        if stage == "eager" and selected_release["kind"] == "published_release_tag":
            errors.append("the eager test JVM did not install a verifiable native tool and manifest")
    else:
        if (selected_release["kind"] == "local_build"
                and installed_tool["native_tool_sha256"] != selected_release["tool_sha256"]):
            errors.append("the installed native tool differs from the selected local release manifest")
        if stage == "eager" and selected_release["kind"] == "published_release_tag":
            if selected_release["tag"] != EXPECTED_EAGER_RELEASE_TAG:
                errors.append(f"eager release tag must be {EXPECTED_EAGER_RELEASE_TAG}, found {selected_release['tag']}")
            if installed_tool["native_tool_sha256"] != EXPECTED_EAGER_NATIVE_TOOL_SHA256:
                errors.append("the installed eager native tool does not match the pinned final RC SHA-256")
    local_tool_prefix = "Using local clang-dumper build:"
    observed_local_tools = sorted({
        line.removeprefix(local_tool_prefix).strip()
        for line in log_path.read_text(encoding="utf-8").splitlines()
        if line.startswith(local_tool_prefix)
    })
    if stage in {"text", "protobuf"}:
        expected_local_tool = str(Path(selected_release["tool_path"]).resolve())
        if observed_local_tools != [expected_local_tool]:
            errors.append(f"the observed local dumper paths differ from the pinned control: {observed_local_tools}")
        if sha256_file(Path(expected_local_tool)) != selected_release["tool_sha256"]:
            errors.append("the pinned historical control dumper changed during the observation")

    return {
        "ordinal": ordinal,
        "round": round_number,
        "stage": stage,
        "command": command,
        "gradle_log": str(log_path),
        "return_code": process_result["return_code"],
        "valid": not errors,
        "validation_errors": errors,
        "test_counts": junit["counts"],
        "test_identity_sha256": junit["identity_sha256"],
        "testcase_duration_s": junit["testcase_duration_s"],
        "gradle_wall_s": process_result["wall_s"],
        "timing_boundary": TIMING_BOUNDARY,
        "cache_state": {
            "CCACHE_DISABLE": environment["CCACHE_DISABLE"],
            "ccache_resolves_from_PATH": False,
            "ccache_invocations_expected": 0,
        },
        "worker": runtime_metadata["worker"],
        "worker_java_io_tmpdir": actual_java_tmpdir,
        "compile_task_states": task_states,
        "build_environment": build_environment,
        "test_classpath_sha256": preflight["classpath_fingerprint"]["sha256"],
        "installed_native_tool": installed_tool,
        "installed_native_tool_sha256": None if installed_tool is None else installed_tool["native_tool_sha256"],
        "installed_release_manifest_sha256": None if installed_tool is None
        else installed_tool["release_manifest_sha256"],
        "installed_wire_schema_sha256": None if installed_tool is None else installed_tool["wire_schema_sha256"],
        "installed_flatbuffers_toolchain": None if installed_tool is None else installed_tool["flatbuffers"],
        "observed_local_dumper_paths": observed_local_tools,
        "runtime_resource_cache": {
            **staged_cache,
            "cache_tree_sha256_after": cache_after,
            "release_manifest_sha256_before": resource_cache["release_manifest_sha256"],
            "release_manifest_sha256_after": release_manifest_after,
            "native_tool_sha256_before": resource_cache["native_tool_sha256"],
            "native_tool_sha256_after": native_tool_after,
            "wire_schema_sha256": resource_cache["wire_schema_sha256"],
        },
        "test_classpath_matches_preflight": classpath_match,
        "test_classpath_content_matches_preflight": classpath_matches_preflight,
        "test_classpath_pre_run_sha256": pre_run_classpath["sha256"],
        "test_classpath_post_run_sha256": post_run_classpath["sha256"],
        "test_classpath_stable_during_run": classpath_stable,
        "source_stable_during_run": source_stable,
        "jvm_environment": environment_options,
        "source_signature_sha256": stable_source_signature(source_before),
    }


def summarize(rows: list[dict[str, Any]], identity: list[str] | None) -> dict[str, Any]:
    medians = {
        stage: statistics.median(row["testcase_duration_s"] for row in rows if row["stage"] == stage)
        for stage in ("text", "protobuf", "eager")
    }
    paired = {}
    for control in ("text", "protobuf"):
        paired[control] = [
            next(row["testcase_duration_s"] for row in rows if row["stage"] == "eager" and row["round"] == round_number)
            - next(row["testcase_duration_s"] for row in rows if row["stage"] == control and row["round"] == round_number)
            for round_number in (1, 2, 3)
        ]
    return {
        "observations_per_stage": 3,
        "test_only_metric": "sum of JUnit testcase duration attributes, seconds",
        "test_only_median_s": medians,
        "paired_eager_minus_control_test_only_s": paired,
        "gradle_wall_is_reported_separately": True,
        "frozen_test_identity_sha256": canonical_sha256(identity) if identity else None,
        "wall_performance_claim": None,
    }


def write_results(output_root: Path, value: dict[str, Any]) -> None:
    temporary = output_root / "results.json.tmp"
    temporary.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    temporary.replace(output_root / "results.json")
    rows = value.get("results", [])
    with (output_root / "results.csv").open("w", newline="", encoding="utf-8") as destination:
        columns = (
            "ordinal", "round", "stage", "valid", "total", "passed", "failed", "skipped",
            "testcase_duration_s", "gradle_wall_s", "test_identity_sha256", "test_classpath_sha256",
        )
        writer = csv.DictWriter(destination, fieldnames=columns)
        writer.writeheader()
        for row in rows:
            counts = row.get("test_counts", {})
            writer.writerow({
                "ordinal": row.get("ordinal"), "round": row.get("round"), "stage": row.get("stage"),
                "valid": row.get("valid"), "total": counts.get("total"), "passed": counts.get("passed"),
                "failed": counts.get("failed"), "skipped": counts.get("skipped"),
                "testcase_duration_s": row.get("testcase_duration_s"), "gradle_wall_s": row.get("gradle_wall_s"),
                "test_identity_sha256": row.get("test_identity_sha256"),
                "test_classpath_sha256": row.get("test_classpath_sha256"),
            })


def main() -> int:
    args = parse_args()
    expected_test_ids = load_pinned_test_ids()
    checkouts = validate_checkouts({
        "eager": args.eager_checkout,
        "text": args.text_checkout,
        "protobuf": args.protobuf_checkout,
    })
    output_root = make_output_root(args.output_root)
    no_cache_path, path_root = make_path_without_ccache(output_root, ("gradle", "java", "javac", "git", "python3"))
    java_runtime = java_runtime_properties(no_cache_path)
    resource_cache = runtime_resource_cache_snapshot(java_runtime)
    snapshots = {stage: stage_snapshot(stage, checkouts[stage]) for stage in ("eager", "text", "protobuf")}
    fixture_comparison = compare_fixture_maps(snapshots)
    environment_options = jvm_environment_audit(base_environment(no_cache_path, output_root))
    inputs = {
        "runner": str(Path(__file__).resolve()),
        "runner_sha256": sha256_file(Path(__file__).resolve()),
        "init_script": str((SCRIPT_ROOT / "java-suite.init.gradle").resolve()),
        "init_script_sha256": sha256_file(SCRIPT_ROOT / "java-suite.init.gradle"),
        "checkout_arguments": {name: str(path) for name, path in checkouts.items()},
        "no_ccache_path": str(path_root.resolve()),
        "cache_state": {
            "CCACHE_DISABLE": "true",
            "ccache_resolves_from_PATH": shutil.which("ccache", path=no_cache_path) is not None,
        },
        "jvm_environment": environment_options,
        "java_runtime": java_runtime,
        "runtime_resource_cache": resource_cache,
        "expected_test_count": EXPECTED_TESTS,
        "max_gradle_workers": 1,
        "pinned_test_ids_file": str(PINNED_TEST_IDS_FILE.resolve()),
        "pinned_test_ids_file_sha256": sha256_file(PINNED_TEST_IDS_FILE),
        "pinned_test_identity_sha256": canonical_sha256(expected_test_ids),
        "test_selection": "java-suite.init.gradle fixed exclusion list; no selection arguments accepted",
        "fixture_comparison": fixture_comparison,
        "timing_boundary": TIMING_BOUNDARY,
    }
    report: dict[str, Any] = {
        "status": "preflight",
        "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "inputs": inputs,
        "stage_inputs": snapshots,
        "preflight": {},
        "identity_preflight": {
            "pinned_by": "suite/java-runtime-test-ids.json",
            "count": len(expected_test_ids),
            "sha256": canonical_sha256(expected_test_ids),
            "test_ids": expected_test_ids,
        },
        "results": [],
        "summary": None,
    }
    write_results(output_root, report)

    try:
        for stage in ("text", "protobuf", "eager"):
            preflight = run_preflight(stage, checkouts[stage], output_root, no_cache_path)
            report["preflight"][stage] = preflight
            report["status"] = f"preflight_{stage}"
            write_results(output_root, report)

        local_options_hashes = {
            stage: report["preflight"][stage]["prepared_runtime_file"]["sha256"]
            for stage in ("text", "protobuf", "eager")
        }
        if len(set(local_options_hashes.values())) != 1:
            raise RuntimeError(f"the parser default local-options file differs across runtimes: {local_options_hashes}")
        report["inputs"]["default_local_options"] = {
            "sha256": next(iter(local_options_hashes.values())),
            "per_stage": local_options_hashes,
        }
        write_results(output_root, report)

        source_signatures = {stage: stable_source_signature(snapshots[stage]) for stage in snapshots}
        for round_number, stage in STAGE_ORDER:
            row = run_observation(
                stage, round_number, checkouts[stage], report["preflight"][stage], snapshots[stage],
                expected_test_ids, output_root, no_cache_path,
                java_runtime, resource_cache,
            )
            if row["source_signature_sha256"] != source_signatures[stage]:
                row["valid"] = False
                row["validation_errors"].append("source provenance differs from its preflight snapshot")

            report["results"].append(row)
            report["status"] = "running" if row["valid"] else "failed"
            write_results(output_root, report)
            if not row["valid"]:
                report["error"] = f"invalid {stage} round {round_number} observation"
                write_results(output_root, report)
                return 1

        report["summary"] = summarize(report["results"], expected_test_ids)
        report["status"] = "complete"
        write_results(output_root, report)
        return 0
    except Exception as error:
        report["status"] = "failed"
        report["error"] = f"{type(error).__name__}: {error}"
        write_results(output_root, report)
        raise


if __name__ == "__main__":
    raise SystemExit(main())
