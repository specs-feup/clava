#!/usr/bin/env python3
"""Compare the frozen PB stage with FlatBuffers using normalized syntax checks.

The default invocation is plan-only: it validates the frozen matrix, source
transformation anchors, compile/link commands, warm-cache records, and fixed
JVM options without writing files or running builds/tests. Execution requires
an explicit parent host-release note and uses one isolated Flat native binary,
one Flat parser-JAR overlay, and four rotated PB-original/Flat-fast warm pairs.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import zipfile
from typing import Any


CLAVA_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MATRIX = (CLAVA_ROOT / "experiments/protocol-comparison/results/deadline-20260930"
                  / "matrix-r2/results.json")
DEFAULT_OUTPUT = DEFAULT_MATRIX.parent / "decision-noagent-r1/flat-fast-syntax-control-r1"
SYNTAX_PROPERTY = "-Dclava.astWireBenchmarkSyntaxOnly=true"
DUMPER_ENTRY = "pt/up/fe/specs/clang/dumper/ClangAstDumper.class"
EXPECTED_TEST_COUNTS = {"total_tests": 164, "passed_tests": 158,
                        "failed_tests": 0, "skipped_tests": 6}
EXPECTED_CACHE = {"cacheable_calls": 166, "hits": 166, "misses": 0,
                  "uncacheable_calls": 0}
PAIR_ORDER = (
    ("protobuf", "flatbuffers"),
    ("flatbuffers", "protobuf"),
    ("protobuf", "flatbuffers"),
    ("flatbuffers", "protobuf"),
)


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def canonical_hash(value: Any) -> str:
    return sha256_bytes(json.dumps(value, sort_keys=True, separators=(",", ":")).encode())


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--reuse-seed-root", type=Path,
                        help="resume from a validated, unmeasured Flat-fast seed in this output root")
    parser.add_argument("--host-release-note",
                        help="required parent handoff note; without it this remains plan-only")
    return parser.parse_args()


def replace_once(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"expected one {label} anchor, found {count}")
    return source.replace(old, new, 1)


def flat_native_control_source(source: str) -> str:
    """Add a syntax-only frontend action only for no-output validation calls."""
    source = replace_once(
        source,
        'static llvm::cl::opt<bool> CompileOnlyOption(\n'
        '        "c", llvm::cl::desc("Parse without linking"),\n'
        '        llvm::cl::cat(MyToolCategory));\n',
        'static llvm::cl::opt<bool> CompileOnlyOption(\n'
        '        "c", llvm::cl::desc("Parse without linking"),\n'
        '        llvm::cl::cat(MyToolCategory));\n'
        'static llvm::cl::opt<bool> SyntaxCheckOnlyOption(\n'
        '        "syntax-check-only",\n'
        '        llvm::cl::desc("Validate syntax without producing an AST dump"),\n'
        '        llvm::cl::cat(MyToolCategory));\n',
        "native syntax-only option declaration",
    )
    source = replace_once(
        source,
        'Argument == "-c" || Argument == "-MD" || HasSeparateValue ||',
        'Argument == "-c" || Argument == "-MD" ||\n'
        '        Argument == "-syntax-check-only" || HasSeparateValue ||',
        "ccache option normalization",
    )
    return replace_once(
        source,
        '    returnValue =\n'
        '        Tool.run(clang::tooling::newFrontendActionFactory<DumpAstAction>().get());',
        '    auto ActionFactory = SyntaxCheckOnlyOption\n'
        '        ? clang::tooling::newFrontendActionFactory<clang::SyntaxOnlyAction>()\n'
        '        : clang::tooling::newFrontendActionFactory<DumpAstAction>();\n'
        '    returnValue = Tool.run(ActionFactory.get());',
        "no-output frontend action selection",
    )


def flat_java_control_source(source: str) -> str:
    """Make Flat validation use the same native syntax-only path as PB.

    The guard checks every controlled invocation's actual argv shape while
    leaving stdout/stderr handling and the normal FlatBuffers parse untouched.
    """
    return replace_once(
        source,
        '        var output = SpecsSystem.runProcess(arguments, lastWorkingFolder,\n'
        '                this::discardOutput,\n'
        '                inputStream -> processOutput(inputStream));',
        '        boolean syntaxOnlyControl = Boolean.getBoolean("clava.astWireBenchmarkSyntaxOnly");\n'
        '        List<String> syntaxArguments = arguments;\n'
        '        if (syntaxOnlyControl) {\n'
        '            syntaxArguments = new ArrayList<>(arguments);\n'
        '            int separatorIndex = syntaxArguments.indexOf("--");\n'
        '            if (separatorIndex >= 0) {\n'
        '                syntaxArguments.add(separatorIndex, "-syntax-check-only");\n'
        '            } else {\n'
        '                syntaxArguments.add("-syntax-check-only");\n'
        '            }\n'
        '            int syntaxFlagCount = java.util.Collections.frequency(syntaxArguments, "-syntax-check-only");\n'
        '            int customEnd = separatorIndex >= 0 ? separatorIndex + 1 : syntaxArguments.size();\n'
        '            for (int i = 0; i < customEnd; i++) {\n'
        '                String argument = syntaxArguments.get(i);\n'
        '                if (argument.equals("-o") || argument.startsWith("-ast-dump-format")) {\n'
        '                    throw new IllegalStateException("syntax-only validation must not request an AST dump");\n'
        '                }\n'
        '            }\n'
        '            if (syntaxFlagCount != 1) {\n'
        '                throw new IllegalStateException("expected exactly one -syntax-check-only in validation argv");\n'
        '            }\n'
        '        }\n'
        '\n'
        '        var output = SpecsSystem.runProcess(syntaxArguments, lastWorkingFolder,\n'
        '                this::discardOutput,\n'
        '                inputStream -> processOutput(inputStream));',
        "Flat Java syntax validation launch",
    )


def fixed_java_options() -> dict[str, Any]:
    names = ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")
    inherited = {name: os.environ.get(name, "") for name in names}
    tokens = {name: shlex.split(value) for name, value in inherited.items() if value.strip()}
    forbidden = [f"{name}:{token}" for name, items in tokens.items() for token in items
                 if token.startswith(("-Xmx", "-Xms", "-javaagent", "-agentlib", "-agentpath"))
                 or "ExplicitGC" in token or "DisableExplicitGC" in token]
    if forbidden:
        raise RuntimeError(f"requires default heap/GC and no agent; inherited JVM options: {forbidden}")
    injected = [f"{name}:{token}" for name, items in tokens.items() for token in items
                if token.startswith(("-Dclava.astWireBenchmarkSyntaxOnly=", "-Dclava.astWireSyntaxMetrics="))]
    if injected:
        raise RuntimeError(f"syntax-control properties must not be inherited: {injected}")
    conflicting = [f"{name}:{token}" for name, items in tokens.items() for token in items
                   if token.startswith(("-Dclava.astWire=", "-Djava.io.tmpdir="))]
    if conflicting:
        raise RuntimeError(f"runner owns wire/temp JVM properties; inherited duplicates: {conflicting}")

    base = tokens.get("JAVA_TOOL_OPTIONS", [])
    controlled = shlex.join([*base, SYNTAX_PROPERTY])
    for arm in ("protobuf", "flatbuffers"):
        parsed = shlex.split(controlled)
        if parsed.count(SYNTAX_PROPERTY) != 1:
            raise RuntimeError(f"{arm}: expected exactly one fixed syntax property")
    return {
        "inherited_environment": inherited,
        "controlled_java_tool_options": controlled,
        "syntax_property": SYNTAX_PROPERTY,
        "property_count_per_arm": 1,
        "same_options_for_both_arms": True,
        "default_heap_gc_no_agent": True,
    }


def runtime_root(stage: dict[str, Any]) -> Path:
    return Path(stage["root"]) / "clava/Clava-JS/java-binaries"


def stage_paths(stage: dict[str, Any]) -> dict[str, Any]:
    converted = dict(stage)
    for key in ("root", "dumper", "native_root"):
        if converted.get(key) is not None:
            converted[key] = Path(converted[key]).resolve()
    return converted


def git_output(root: Path, *args: str) -> str:
    result = subprocess.run(["git", "-C", str(root), *args],
                            text=True, capture_output=True, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} failed in {root}: {result.stderr.strip()}")
    return result.stdout.strip()


def git_status_lines(root: Path) -> list[str]:
    result = subprocess.run(
        ["git", "-C", str(root), "status", "--porcelain=v1", "--untracked-files=all"],
        text=True, capture_output=True, check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(f"git status failed in {root}: {result.stderr.strip()}")
    return result.stdout.rstrip("\n").splitlines()


def validate_frozen_stage(stage: dict[str, Any]) -> dict[str, Any]:
    root = Path(stage["root"]).resolve()
    native_root = Path(stage["native_root"]).resolve()
    clava = root / "clava"
    expected_clava_status = stage.get("clava_status", [])
    expected_native_status = stage.get("dumper_status", [])
    actual_clava_status = git_status_lines(clava)
    actual_native_status = git_status_lines(native_root)
    if git_output(clava, "rev-parse", "HEAD") != stage["clava_revision"]:
        raise RuntimeError(f"{stage['key']} Clava revision differs from accepted matrix")
    if git_output(native_root, "rev-parse", "HEAD") != stage["dumper_revision"]:
        raise RuntimeError(f"{stage['key']} native revision differs from accepted matrix")
    if actual_clava_status != expected_clava_status or actual_native_status != expected_native_status:
        raise RuntimeError(f"{stage['key']} source status differs from accepted matrix snapshot")

    parser_jar = runtime_root(stage) / "lib/ClangAstParser.jar"
    binary = Path(stage["dumper"]).resolve()
    if not parser_jar.is_file() or not binary.is_file():
        raise RuntimeError(f"{stage['key']} frozen runtime or native binary is missing")
    if sha256_file(parser_jar) != stage["parser_jar_sha256"]:
        raise RuntimeError(f"{stage['key']} frozen parser JAR hash differs from matrix")
    if sha256_file(binary) != stage["dumper_sha256"]:
        raise RuntimeError(f"{stage['key']} frozen native binary hash differs from matrix")
    runtime_manifest = __import__("run_comparison").runtime_manifest(runtime_root(stage))
    if runtime_manifest != stage["runtime_manifest"]:
        raise RuntimeError(f"{stage['key']} frozen runtime manifest differs from matrix")
    return {
        "clava_revision": stage["clava_revision"],
        "native_revision": stage["dumper_revision"],
        "parser_jar_sha256": sha256_file(parser_jar),
        "native_binary_sha256": sha256_file(binary),
        "runtime_manifest": runtime_manifest,
        "clava_status_matches_matrix": True,
        "native_status_matches_matrix": True,
    }


def native_command_plan(stage: dict[str, Any], output_root: Path) -> dict[str, Any]:
    native_root = Path(stage["native_root"]).resolve()
    build_dir = native_root / "build"
    source_file = native_root / "src/tool.cpp"
    if not source_file.is_file():
        raise RuntimeError(f"missing Flat native source: {source_file}")
    compile_db_path = build_dir / "compile_commands.json"
    link_path = build_dir / "CMakeFiles/tool.dir/link.txt"
    cache_path = build_dir / "CMakeCache.txt"
    if not all(path.is_file() for path in (compile_db_path, link_path, cache_path)):
        raise RuntimeError("Flat build is missing compile_commands.json/link.txt/CMakeCache.txt")

    compile_rows = [row for row in json.loads(compile_db_path.read_text())
                    if Path(row["file"]).resolve() == source_file.resolve()]
    if len(compile_rows) != 1:
        raise RuntimeError(f"expected one frozen tool.cpp compile command, found {len(compile_rows)}")
    row = compile_rows[0]
    old_object = "CMakeFiles/tool.dir/src/tool.cpp.o"
    argv = shlex.split(row["command"])
    if row["directory"] != str(build_dir) or argv.count(source_file.as_posix()) != 1 or argv.count(old_object) != 1:
        raise RuntimeError("tool.cpp compile command differs from the expected frozen build")
    if "-O3" not in argv or "-DNDEBUG" not in argv or "clang++-18" not in argv[0]:
        raise RuntimeError("frozen tool.cpp compile command is not the accepted LLVM 18 -O3 build")

    cache_text = cache_path.read_text()
    for expected in ("CMAKE_BUILD_TYPE:STRING=Release", "CMAKE_CXX_COMPILER:STRING=/usr/bin/clang++-18",
                     "CMAKE_CXX_FLAGS_RELEASE:STRING=-O3 -DNDEBUG"):
        if expected not in cache_text:
            raise RuntimeError(f"Flat CMake cache lacks accepted build setting: {expected}")

    overlay_dir = output_root / "preparation/native"
    overlay_source = overlay_dir / "tool.cpp"
    overlay_object = overlay_dir / "tool.cpp.o"
    overlay_binary = overlay_dir / "tool"
    compile_argv = list(argv)
    compile_argv[compile_argv.index(source_file.as_posix())] = str(overlay_source)
    compile_argv[compile_argv.index(old_object)] = str(overlay_object)
    compile_index = compile_argv.index("-c")
    compile_argv[compile_index:compile_index] = ["-iquote", str(native_root / "src")]

    link_argv = shlex.split(link_path.read_text())
    if link_argv.count(old_object) != 1 or link_argv.count("-o") != 1:
        raise RuntimeError("frozen Flat native link command has an unexpected object/output list")
    dep_indices = [index for index, arg in enumerate(link_argv) if arg.startswith("--dependency-file=")]
    if len(dep_indices) != 1:
        raise RuntimeError("frozen Flat native link command has no unique dependency-file output")
    planned_link = list(link_argv)
    planned_link[planned_link.index(old_object)] = str(overlay_object)
    planned_link[planned_link.index("-o") + 1] = str(overlay_binary)
    planned_link[dep_indices[0]] = f"--dependency-file={overlay_dir / 'link.d'}"

    link_objects = []
    for argument in link_argv:
        if argument.endswith(".o"):
            path = Path(argument)
            path = path if path.is_absolute() else build_dir / path
            if not path.is_file():
                raise RuntimeError(f"frozen Flat native link object is missing: {path}")
            if argument != old_object:
                link_objects.append({"path": str(path), "sha256": sha256_file(path)})
    original_object = build_dir / old_object
    original_tool = build_dir / "tool"
    return {
        "build_dir": str(build_dir),
        "source_file": str(source_file),
        "source_sha256": sha256_file(source_file),
        "original_tool_sha256": sha256_file(original_tool),
        "original_tool_object_sha256": sha256_file(original_object),
        "compile_commands_sha256": sha256_file(compile_db_path),
        "link_txt_sha256": sha256_file(link_path),
        "compile_argv": compile_argv,
        "link_argv": planned_link,
        "frozen_link_objects": link_objects,
        "overlay_source": str(overlay_source),
        "overlay_object": str(overlay_object),
        "overlay_binary": str(overlay_binary),
        "native_revision": stage["dumper_revision"],
        "cmake_release_o3_llvm18": True,
    }


def warm_cache_sources(matrix: dict[str, Any]) -> dict[str, Path]:
    sources: dict[str, Path] = {}
    for key in ("protobuf", "flatbuffers"):
        rows = [row for row in matrix["results"] if row.get("suite") == "clava-js"
                and row.get("mode") == "warm" and row.get("stage") == key
                and row.get("measured") is True and row.get("selected") is True]
        if len(rows) != 4 or any(not row.get("valid") for row in rows):
            raise RuntimeError(f"expected four selected accepted warm JS runs for {key}, found {len(rows)}")
        cache_paths = {Path(row["cache_validation"]["cache_dir"]).resolve() for row in rows}
        observed = {(int(row.get("cacheable_calls", 0)), int(row.get("cache_hits", 0)),
                     int(row.get("cache_misses", 0)),
                     int(row.get("cache_validation", {}).get("uncacheable_calls", 0)))
                    for row in rows}
        expected = tuple(EXPECTED_CACHE.values())
        if len(cache_paths) != 1 or observed != {expected}:
            raise RuntimeError(f"{key} accepted warm cache records drifted: paths={cache_paths}, stats={observed}")
        sources[key] = next(iter(cache_paths))
    return sources


def load_dependencies(matrix_file: Path) -> tuple[dict[str, Any], Any, Any]:
    matrix_file = matrix_file.resolve()
    if not matrix_file.is_file():
        raise RuntimeError(f"accepted matrix results are missing: {matrix_file}")
    matrix_data = json.loads(matrix_file.read_text())
    deadline_root = matrix_file.parents[1]
    scripts_root = deadline_root / "orchestration/experiments/protocol-comparison"
    if not scripts_root.is_dir():
        raise RuntimeError(f"frozen matrix runner sources are missing: {scripts_root}")
    sys.path.insert(0, str(scripts_root))
    import run_deadline_matrix as matrix_module

    if Path(matrix_module.DEADLINE_ROOT).resolve() != deadline_root:
        raise RuntimeError("frozen matrix runner resolves a different experiment root")
    return matrix_data, matrix_module, matrix_module.comparison


def source_preflight(matrix_data: dict[str, Any], matrix_module: Any,
                     comparison: Any, output_root: Path) -> dict[str, Any]:
    plan = matrix_data.get("plan", {})
    if plan.get("all_measurements_valid") is not True:
        raise RuntimeError("accepted matrix does not report all measurements valid")
    stages_raw = plan.get("stages", {})
    if set(("protobuf", "flatbuffers")) - set(stages_raw):
        raise RuntimeError("accepted matrix does not contain both protocol stages")
    stages = {key: stage_paths(stages_raw[key]) for key in ("protobuf", "flatbuffers")}
    stage_gates = {key: validate_frozen_stage(stages[key]) for key in stages}

    identity = matrix_data.get("identity_preflight", {}).get("reference_test_ids", {}).get("clava-js")
    if not identity or identity.get("count") != EXPECTED_TEST_COUNTS["total_tests"]:
        raise RuntimeError("accepted matrix lacks the expected 164-test Clava-JS identity")
    cache_sources = warm_cache_sources(matrix_data)

    flat = stages["flatbuffers"]
    native_source = Path(flat["native_root"]) / "src/tool.cpp"
    java_source = (Path(flat["root"]) / "clava/ClangAstParser/src/pt/up/fe/specs/clang/dumper/ClangAstDumper.java")
    if not native_source.is_file() or not java_source.is_file():
        raise RuntimeError("Flat source files required for syntax control are missing")
    java_text = java_source.read_text()
    protobuf_java = (Path(stages["protobuf"]["root"])
                     / "clava/ClangAstParser/src/pt/up/fe/specs/clang/dumper/ClangAstDumper.java")
    if "clava.astWireBenchmarkSyntaxOnly" in protobuf_java.read_text():
        raise RuntimeError("PB frozen source unexpectedly consumes the benchmark-only syntax property")

    original_native = native_source.read_text()
    transformed_native = flat_native_control_source(original_native)
    transformed_java = flat_java_control_source(java_text)
    if transformed_native.count("flatbuffers-v2") != original_native.count("flatbuffers-v2"):
        raise RuntimeError("native syntax overlay unexpectedly modifies FlatBuffers producer code")
    if "clang::SyntaxOnlyAction" not in transformed_native:
        raise RuntimeError("Flat native overlay does not select SyntaxOnlyAction")
    if "syntaxFlagCount != 1" not in transformed_java:
        raise RuntimeError("Flat Java overlay does not enforce the actual validation argv")
    if "-syntax-check-only" in java_text or "-syntax-check-only" in original_native:
        raise RuntimeError("frozen Flat stage unexpectedly already has the syntax-only control")

    native_plan = native_command_plan(flat, output_root)
    java_options = fixed_java_options()
    expected_counts = {"total_tests": EXPECTED_TEST_COUNTS["total_tests"],
                       "passed_tests": EXPECTED_TEST_COUNTS["passed_tests"],
                       "failed_tests": EXPECTED_TEST_COUNTS["failed_tests"],
                       "skipped_tests": EXPECTED_TEST_COUNTS["skipped_tests"]}
    if any(row.get("stage") in ("protobuf", "flatbuffers")
           and row.get("suite") == "clava-js"
           and row.get("mode") == "warm"
           and row.get("measured") is True
           and {key: row.get(key) for key in expected_counts} != expected_counts
           for row in matrix_data["results"]):
        raise RuntimeError("accepted warm JS matrix rows do not have the fixed test counts")

    return {
        "matrix_file": str(matrix_module.DEADLINE_ROOT / "matrix-r2/results.json"),
        "accepted_js_test_identity_sha256": identity["sha256"],
        "expected_js_test_counts": EXPECTED_TEST_COUNTS,
        "stage_gates": stage_gates,
        "warm_cache_sources": {key: str(path) for key, path in cache_sources.items()},
        "expected_warm_cache": EXPECTED_CACHE,
        "native_build_plan": native_plan,
        "flat_java_source_sha256": sha256_file(java_source),
        "flat_java_overlay_sha256": sha256_bytes(transformed_java.encode()),
        "flat_native_overlay_sha256": sha256_bytes(transformed_native.encode()),
        "flat_producer_unchanged": True,
        "pb_runtime_and_native_unmodified": True,
        "java_options": java_options,
        "pair_order": [list(pair) for pair in PAIR_ORDER],
        "pairs": len(PAIR_ORDER),
        "measured_commands": 2 * len(PAIR_ORDER),
        "mode": "plan-only",
    }


def tree_sha256(root: Path) -> str:
    digest = hashlib.sha256()
    files = sorted(path for path in root.rglob("*") if path.is_file())
    for path in files:
        relative = path.relative_to(root).as_posix().encode()
        digest.update(len(relative).to_bytes(8, "big"))
        digest.update(relative)
        digest.update(bytes.fromhex(sha256_file(path)))
    return digest.hexdigest()


def run_checked(command: list[str], cwd: Path) -> None:
    subprocess.run(command, cwd=cwd, check=True)


def build_flat_native_control(stage: dict[str, Any], output_root: Path,
                              native_plan: dict[str, Any]) -> tuple[Path, dict[str, Any]]:
    native_root = Path(stage["native_root"]).resolve()
    build_dir = native_root / "build"
    source_file = native_root / "src/tool.cpp"
    if git_output(native_root, "rev-parse", "HEAD") != stage["dumper_revision"]:
        raise RuntimeError("Flat native source revision changed since preflight")
    if git_status_lines(native_root):
        raise RuntimeError("Flat native source tree is no longer clean")
    if sha256_file(source_file) != native_plan["source_sha256"]:
        raise RuntimeError("Flat native tool.cpp changed since preflight")

    overlay_dir = output_root / "preparation/native"
    overlay_dir.mkdir(parents=True)
    overlay_source = overlay_dir / "tool.cpp"
    overlay_source.write_text(flat_native_control_source(source_file.read_text()))
    overlay_object = overlay_dir / "tool.cpp.o"
    overlay_binary = overlay_dir / "tool"

    compile_argv = list(native_plan["compile_argv"])
    link_argv = list(native_plan["link_argv"])
    run_checked(compile_argv, build_dir)
    run_checked(link_argv, build_dir)
    if not overlay_binary.is_file() or not os.access(overlay_binary, os.X_OK):
        raise RuntimeError("isolated Flat fast-syntax native binary was not produced")

    build_plan = native_plan
    original_tool = build_dir / "tool"
    original_object = build_dir / "CMakeFiles/tool.dir/src/tool.cpp.o"
    if sha256_file(original_tool) != build_plan["original_tool_sha256"]:
        raise RuntimeError("frozen Flat native binary was modified during isolated build")
    if sha256_file(original_object) != build_plan["original_tool_object_sha256"]:
        raise RuntimeError("frozen Flat tool.cpp object was modified during isolated build")
    for link_object in build_plan["frozen_link_objects"]:
        if sha256_file(Path(link_object["path"])) != link_object["sha256"]:
            raise RuntimeError(f"frozen native link object changed: {link_object['path']}")

    provenance = {
        "native_revision": stage["dumper_revision"],
        "source_sha256": sha256_file(source_file),
        "original_tool_sha256": sha256_file(original_tool),
        "original_tool_object_sha256": sha256_file(original_object),
        "compile_commands_sha256": build_plan["compile_commands_sha256"],
        "link_txt_sha256": build_plan["link_txt_sha256"],
        "compile_argv": compile_argv,
        "link_argv": link_argv,
        "frozen_link_objects": build_plan["frozen_link_objects"],
        "overlay_source_sha256": sha256_file(overlay_source),
        "overlay_object_sha256": sha256_file(overlay_object),
        "control_binary_sha256": sha256_file(overlay_binary),
        "original_binary_and_objects_unchanged": True,
    }
    (overlay_dir / "native-build-provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
    return overlay_binary, provenance


def syntax_parity_probe(original_tool: Path, control_tool: Path,
                        output_root: Path) -> dict[str, Any]:
    probe_dir = output_root / "preparation/probe"
    probe_dir.mkdir(parents=True)
    fixture = probe_dir / "malformed.c"
    fixture.write_text("int main( {\n")

    def invoke(tool: Path, fast: bool) -> subprocess.CompletedProcess[str]:
        argv = [str(tool), "-c", str(fixture), "-id=1", "-system-header-threshold=1"]
        if fast:
            argv.append("-syntax-check-only")
        argv.extend(["--", "-std=c99"])
        return subprocess.run(argv, cwd=probe_dir, capture_output=True, text=True, check=False)

    baseline = invoke(original_tool, False)
    optimized = invoke(control_tool, True)
    baseline_errors = [line for line in baseline.stderr.splitlines() if "error:" in line]
    optimized_errors = [line for line in optimized.stderr.splitlines() if "error:" in line]
    if (baseline.returncode == 0 or optimized.returncode == 0
            or not baseline_errors or baseline_errors != optimized_errors):
        raise RuntimeError("Flat malformed-input syntax diagnostic parity probe failed")
    return {
        "fixture_sha256": sha256_file(fixture),
        "baseline_status": baseline.returncode,
        "fast_status": optimized.returncode,
        "error_lines_equal": True,
        "baseline_stderr_chars": len(baseline.stderr),
        "fast_stderr_chars": len(optimized.stderr),
        "timed": False,
    }


def java_overlay_classes(source_runtime: Path, destination_runtime: Path,
                         class_directory: Path, entries: list[str],
                         native_tool: Path) -> dict[str, Any]:
    parser_jar = destination_runtime / "lib/ClangAstParser.jar"
    command = ["jar", "uf", str(parser_jar)]
    for entry in entries:
        command.extend(["-C", str(class_directory), entry])
    subprocess.run(command, check=True)

    source_jars = {path.relative_to(source_runtime).as_posix(): sha256_file(path)
                   for path in source_runtime.rglob("*.jar") if path.is_file()}
    output_jars = {path.relative_to(destination_runtime).as_posix(): sha256_file(path)
                   for path in destination_runtime.rglob("*.jar") if path.is_file()}
    if set(source_jars) != set(output_jars):
        raise RuntimeError("Flat runtime overlay changed the JAR set")
    for relative, digest in source_jars.items():
        if relative != "lib/ClangAstParser.jar" and digest != output_jars[relative]:
            raise RuntimeError(f"Flat runtime overlay changed unrelated JAR: {relative}")

    with zipfile.ZipFile(source_runtime / "lib/ClangAstParser.jar") as source_archive, \
            zipfile.ZipFile(parser_jar) as output_archive:
        source_names = set(source_archive.namelist())
        output_names = set(output_archive.namelist())
        if output_names != source_names | set(entries):
            raise RuntimeError("Flat parser JAR contains unexpected overlay entries")
        allowed = set(entries) | {"clang-dumper-release.tag"}
        for name in source_names - allowed:
            if source_archive.read(name) != output_archive.read(name):
                raise RuntimeError(f"Flat parser JAR changed outside controlled overlay: {name}")
        tag = output_archive.read("clang-dumper-release.tag").decode("utf-8").strip()
        if Path(tag).resolve() != native_tool.parent.resolve():
            raise RuntimeError("Flat runtime release tag does not resolve to isolated native binary")

    return {
        "runtime_jar_manifest": __import__("run_comparison").runtime_manifest(destination_runtime),
        "parser_jar_sha256": sha256_file(parser_jar),
        "overlay_class_entries": entries,
        "only_parser_class_and_release_tag_changed": True,
    }


def compile_flat_java_overlay(stage: dict[str, Any], output_root: Path,
                              source_sha: str) -> tuple[Path, list[str]]:
    java_source = (Path(stage["root"]) / "clava/ClangAstParser/src/pt/up/fe/specs/clang/dumper/ClangAstDumper.java")
    if sha256_file(java_source) != source_sha:
        raise RuntimeError("frozen Flat ClangAstDumper source changed since preflight")
    source_runtime = runtime_root(stage)
    overlay_dir = output_root / "preparation/java"
    source_path = overlay_dir / DUMPER_ENTRY.removesuffix(".class")
    source_path = source_path.with_suffix(".java")
    source_path.parent.mkdir(parents=True)
    source_path.write_text(flat_java_control_source(java_source.read_text()))
    class_directory = overlay_dir / "classes"
    class_directory.mkdir()
    subprocess.run(["javac", "-classpath", str(source_runtime / "lib/*"), "-d",
                    str(class_directory), str(source_path)], check=True)
    entries = sorted(path.relative_to(class_directory).as_posix()
                     for path in class_directory.rglob("*.class"))
    if DUMPER_ENTRY not in entries:
        raise RuntimeError("javac did not produce ClangAstDumper.class")
    return class_directory, entries


def cache_tree_hash(root: Path) -> str:
    return tree_sha256(root)


def seed_cache(source: Path, destination: Path) -> dict[str, Any]:
    if not source.is_dir():
        raise RuntimeError(f"accepted warm cache is missing: {source}")
    if destination.exists():
        raise RuntimeError(f"refusing to overwrite cache destination: {destination}")
    before = cache_tree_hash(source)
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(source, destination)
    copied = cache_tree_hash(destination)
    after = cache_tree_hash(source)
    if before != copied or before != after:
        raise RuntimeError(f"warm ccache copy or source changed during clone: {source}")
    return {"source": str(source), "destination": str(destination),
            "sha256": before, "source_unchanged": True, "copy_matches": True}


def verify_java_options_log(run_dir: Path) -> dict[str, Any]:
    log_path = run_dir / "run.log"
    lines = [line for line in log_path.read_text(errors="replace").splitlines()
             if "Picked up JAVA_TOOL_OPTIONS:" in line]
    if not lines:
        raise RuntimeError(f"JVM option provenance line missing from {log_path}")
    for line in lines:
        if line.count(SYNTAX_PROPERTY) != 1:
            raise RuntimeError(f"controlled syntax property missing/duplicated in JVM launch line: {line}")
    return {"jvm_option_lines": len(lines), "one_syntax_property_each": True}


def write_results(output_root: Path, plan: dict[str, Any], results: list[dict[str, Any]]) -> None:
    (output_root / "plan.json").write_text(json.dumps(plan, indent=2) + "\n")
    (output_root / "results.json").write_text(json.dumps(results, indent=2) + "\n")
    csv_path = output_root / "paired-runs.csv"
    fields = ["pair", "position", "stage", "fast_syntax", "elapsed_s", "driver_elapsed_s",
              "cacheable_calls", "cache_hits", "cache_misses", "total_tests", "passed_tests",
              "skipped_tests", "failed_tests", "test_identity_sha256", "native_sha256",
              "parser_jar_sha256"]
    with csv_path.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields)
        writer.writeheader()
        for result in results:
            cache = result.get("cache_validation", {})
            writer.writerow({"pair": result.get("pair"), "position": result.get("position"),
                             "stage": result.get("stage"), "fast_syntax": True,
                             "elapsed_s": result.get("elapsed_s"),
                             "driver_elapsed_s": result.get("driver_elapsed_s"),
                             "cacheable_calls": cache.get("cacheable_calls"),
                             "cache_hits": cache.get("hits"), "cache_misses": cache.get("misses"),
                             "total_tests": result.get("total_tests"),
                             "passed_tests": result.get("passed_tests"),
                             "skipped_tests": result.get("skipped_tests"),
                             "failed_tests": result.get("failed_tests"),
                             "test_identity_sha256": result.get("test_identity_sha256"),
                             "native_sha256": result.get("native_sha256"),
                             "parser_jar_sha256": result.get("runtime_parser_jar_sha256")})


def execute(matrix_data: dict[str, Any], matrix_module: Any, comparison: Any,
            output_root: Path, plan_record: dict[str, Any], host_release_note: str,
            reuse_seed_root: Path | None = None) -> dict[str, Any]:
    reuse_seed_root = reuse_seed_root.resolve() if reuse_seed_root is not None else None
    resume_in_place = reuse_seed_root == output_root.resolve()
    if output_root.exists() and not resume_in_place:
        raise RuntimeError(f"refusing to reuse existing output root: {output_root}")
    if not host_release_note.strip():
        raise RuntimeError("execution requires an explicit host-release note")
    if not resume_in_place:
        output_root.mkdir(parents=True)
    stages = {key: stage_paths(matrix_data["plan"]["stages"][key])
              for key in ("protobuf", "flatbuffers")}
    flat_stage = stages["flatbuffers"]

    prior_plan: dict[str, Any] = {}
    if resume_in_place:
        prior_plan_path = output_root / "plan.json"
        prior_results_path = output_root / "results.json"
        if not prior_plan_path.is_file() or not prior_results_path.is_file():
            raise RuntimeError("resume root is missing its prior plan or result record")
        prior_plan = json.loads(prior_plan_path.read_text())
        prior_results = json.loads(prior_results_path.read_text())
        warmup = prior_plan.get("flat_fast_warmup")
        if warmup is None:
            first_attempt = output_root / "preparation/first-attempt-plan.json"
            warmup = json.loads(first_attempt.read_text()).get("flat_fast_warmup", {})
        native_provenance = prior_plan.get("native_provenance", {})
        syntax_probe = prior_plan.get("syntax_probe", {})
        cache_clones = prior_plan.get("cache_clones", {})
        native_tool = output_root / "preparation/native/tool"
        class_directory = output_root / "preparation/java/classes"
        warmup_cache = warmup.get("cache_validation", {})
        cache_counts = {name: warmup_cache.get(name) for name in
                        ("cacheable_calls", "hits", "misses", "uncacheable_calls")}
        accepted_flat_cache = warm_cache_sources(matrix_data)["flatbuffers"].resolve()
        flat_cache = Path(warmup_cache.get("cache_dir", "")).resolve()
        pb_cache_clone = cache_clones.get("protobuf", {})
        pb_cache_destination = Path(pb_cache_clone.get("destination", "")).resolve()
        pb_cache_source = Path(pb_cache_clone.get("source", "")).resolve()
        existing_measured_runs = [path for path in (output_root / "runs/clava-js").glob("[0-9][0-9]-*")
                                  if int(path.name[:2]) > 0]
        pb_cache_prime = None
        if prior_plan.get("mode") == "partial" and len(prior_results) == 1:
            candidate = prior_results[0]
            candidate_cache = candidate.get("cache_validation", {})
            candidate_counts = {name: candidate_cache.get(name) for name in
                                ("cacheable_calls", "hits", "misses", "uncacheable_calls")}
            if (candidate.get("stage") != "protobuf" or candidate.get("pair") != 1
                    or candidate.get("position") != 1 or candidate.get("valid") is not False
                    or candidate.get("return_code") != 0
                    or {name: candidate.get(name) for name in EXPECTED_TEST_COUNTS}
                    != EXPECTED_TEST_COUNTS
                    or candidate.get("workload_identity_match") is not True
                    or candidate_counts.get("cacheable_calls") != 166
                    or candidate_counts.get("hits", 0) + candidate_counts.get("misses", 0) != 166
                    or candidate_counts.get("hits", 0) <= 0 or candidate_counts.get("misses", 0) <= 0
                    or candidate_counts.get("uncacheable_calls") != 0):
                raise RuntimeError("partial result is not the expected PB cache-prime command")
            pb_cache_prime = candidate
        elif prior_plan.get("mode") != "invalid-flat-fast-warmup" or prior_results:
            raise RuntimeError("resume root is not the expected seed-only or PB-prime attempt")
        if not warmup.get("valid") or warmup.get("measured") is not False:
            raise RuntimeError("saved Flat-fast seed did not pass the full-suite gate")
        if {name: warmup.get(name) for name in EXPECTED_TEST_COUNTS} != EXPECTED_TEST_COUNTS:
            raise RuntimeError("saved Flat-fast seed has different suite counts")
        identity = matrix_data["identity_preflight"]["reference_test_ids"]["clava-js"]["sha256"]
        if warmup.get("test_identity_sha256") != identity:
            raise RuntimeError("saved Flat-fast seed has a different test identity")
        if (cache_counts["cacheable_calls"] != 166 or cache_counts["hits"] + cache_counts["misses"] != 166
                or cache_counts["misses"] <= 0 or cache_counts["uncacheable_calls"] != 0):
            raise RuntimeError(f"saved Flat-fast seed cache evidence is invalid: {cache_counts}")
        if flat_cache == accepted_flat_cache or not flat_cache.is_dir():
            raise RuntimeError("saved Flat-fast seed cache was reused from the accepted Flat stage")
        if not cache_clones.get("protobuf", {}).get("copy_matches") or not pb_cache_destination.is_dir():
            raise RuntimeError("saved PB accepted warm-cache clone is missing")
        if pb_cache_source != warm_cache_sources(matrix_data)["protobuf"].resolve():
            raise RuntimeError("saved PB cache clone does not originate from the accepted matrix")
        if cache_tree_hash(pb_cache_source) != pb_cache_clone["sha256"]:
            raise RuntimeError("accepted PB source cache changed since the initial clone")
        if pb_cache_prime is None and cache_tree_hash(pb_cache_destination) != pb_cache_clone["sha256"]:
            raise RuntimeError("saved PB cache clone changed before resume")
        if pb_cache_prime is None and existing_measured_runs:
            raise RuntimeError(f"resume root already contains measured runs: {existing_measured_runs}")
        if pb_cache_prime is not None and [path.name for path in existing_measured_runs] != ["01-protobuf"]:
            raise RuntimeError(f"resume root has unexpected commands around PB cache priming: {existing_measured_runs}")
        if not native_tool.is_file() or not class_directory.is_dir():
            raise RuntimeError("saved Flat-fast native or Java overlay artifacts are missing")
        if sha256_file(native_tool) != native_provenance.get("control_binary_sha256"):
            raise RuntimeError("saved Flat-fast native binary hash differs from its provenance")
        native_plan = plan_record["native_build_plan"]
        for key in ("native_revision", "source_sha256", "original_tool_sha256",
                    "original_tool_object_sha256", "compile_commands_sha256", "link_txt_sha256"):
            if native_provenance.get(key) != native_plan.get(key):
                raise RuntimeError(f"saved Flat-fast build differs from current frozen native input: {key}")
        if native_provenance.get("frozen_link_objects") != native_plan.get("frozen_link_objects"):
            raise RuntimeError("saved Flat-fast binary was linked against different frozen objects")
        if native_provenance.get("overlay_source_sha256") != plan_record["flat_native_overlay_sha256"]:
            raise RuntimeError("saved Flat-fast binary source differs from the current transform")
        if warmup.get("native_sha256") != native_provenance.get("control_binary_sha256"):
            raise RuntimeError("saved Flat-fast seed used a different native binary")
        if sha256_file(Path(warmup["run_dir"]) / "java-binaries/lib/ClangAstParser.jar") != \
                warmup.get("runtime_parser_jar_sha256"):
            raise RuntimeError("saved Flat-fast seed parser JAR hash differs from its run record")
        java_overlay_source = output_root / "preparation/java/pt/up/fe/specs/clang/dumper/ClangAstDumper.java"
        if sha256_file(java_overlay_source) != plan_record["flat_java_overlay_sha256"]:
            raise RuntimeError("saved Flat Java overlay source differs from the planned transform")
        class_entries = sorted(path.relative_to(class_directory).as_posix()
                               for path in class_directory.rglob("*.class"))
        if DUMPER_ENTRY not in class_entries:
            raise RuntimeError("saved Flat Java overlay lacks ClangAstDumper.class")
        if not syntax_probe.get("error_lines_equal") or syntax_probe.get("timed") is not False:
            raise RuntimeError("saved malformed-input parity probe did not pass")

        # Keep the rejected gate and cache-prime records before the successful
        # continuation replaces the top-level result summary.
        preparation = output_root / "preparation"
        for name in ("plan.json", "results.json"):
            saved = preparation / f"first-attempt-{name}"
            if not saved.exists():
                shutil.copy2(output_root / name, saved)
        if pb_cache_prime is not None:
            for name in ("plan.json", "results.json"):
                saved = preparation / f"pb-cache-prime-{name}"
                if not saved.exists():
                    shutil.copy2(output_root / name, saved)
            cache_clones["protobuf_seed"] = {
                "run_dir": pb_cache_prime["run_dir"],
                "excluded_from_pair_timings": True,
                "cacheable_calls": 166,
                "hits": pb_cache_prime["cache_validation"]["hits"],
                "misses": pb_cache_prime["cache_validation"]["misses"],
                "cache_tree_sha256_after_prime": cache_tree_hash(pb_cache_destination),
            }
        cache_clones["flat_fast_seed"] = {
            "source": str(flat_cache), "destination": str(flat_cache),
            "reused_in_place": True, "fresh_control_cache": True,
            "tree_sha256_before_measurements": cache_tree_hash(flat_cache),
            "warmup_calls": 166, "warmup_hits": cache_counts["hits"],
            "warmup_misses": cache_counts["misses"],
        }
        flat_fast_warmup = warmup
    else:
        native_tool, native_provenance = build_flat_native_control(
            flat_stage, output_root, plan_record["native_build_plan"])
        syntax_probe = syntax_parity_probe(Path(flat_stage["dumper"]), native_tool, output_root)
        class_directory, class_entries = compile_flat_java_overlay(
            flat_stage, output_root, plan_record["flat_java_source_sha256"])

    original_stage_runtime = comparison.stage_runtime
    overlay_gates: list[dict[str, Any]] = []

    def stage_runtime_with_control(source: Path, destination: Path, dumper: str | None) -> None:
        original_stage_runtime(source, destination, dumper)
        if dumper is not None and Path(dumper).resolve() == native_tool.resolve():
            overlay_gates.append(java_overlay_classes(
                source, destination, class_directory, class_entries, native_tool))

    comparison.stage_runtime = stage_runtime_with_control
    if not resume_in_place:
        cache_clones = {}
        expected_caches = warm_cache_sources(matrix_data)
        pb_cache_root = output_root / "cache/clava-js/protobuf"
        pb_cache_destination = (pb_cache_root / "@specs-feup/clava"
                                / comparison.cache_namespace(stages["protobuf"]))
        cache_clones["protobuf"] = seed_cache(expected_caches["protobuf"], pb_cache_destination)
        flat_cache_root = output_root / "cache/clava-js/flatbuffers"
        flat_cache_destination = (flat_cache_root / "@specs-feup/clava"
                                  / comparison.cache_namespace(stages["flatbuffers"]))
        if flat_cache_destination.exists():
            raise RuntimeError(f"refusing to reuse Flat-fast cache destination: {flat_cache_destination}")
        flat_fast_warmup = None

    original_java_tool_options = os.environ.get("JAVA_TOOL_OPTIONS", "")
    controlled_options = plan_record["java_options"]["controlled_java_tool_options"]
    os.environ["JAVA_TOOL_OPTIONS"] = controlled_options
    measured: list[dict[str, Any]] = []
    flat_fast_warmup: dict[str, Any] | None
    expected_identity = matrix_data["identity_preflight"]["reference_test_ids"]["clava-js"]["sha256"]
    try:
        # The original Flat cache is not reusable: ccache keys the compiler
        # binary, and this control deliberately links a new native executable.
        # Seed only after installing the isolated runtime overlay, then require
        # the complete 166-call warmup to miss into this fresh cache.
        if not resume_in_place:
            seed_stage = dict(flat_stage)
            seed_stage["dumper"] = str(native_tool)
            seed_stage["label"] = "FlatBuffers · syntax-normalized warm-up"
            flat_fast_warmup = comparison.run_clava_js(
                seed_stage, output_root, ordinal=0, measured=False, repeat=None, mode="warm")
            warm_identity = matrix_module.js_identity(flat_fast_warmup)
            warm_identity_sha = canonical_hash(warm_identity)
            flat_fast_warmup.update({
                "unmeasured_cache_seed": True,
                "fast_syntax": True,
                "test_identity": warm_identity,
                "test_identity_sha256": warm_identity_sha,
                "workload_identity_match": warm_identity_sha == expected_identity,
                "native_sha256": sha256_file(native_tool),
            })
            verify_java_options_log(Path(flat_fast_warmup["run_dir"]))
            warm_cache = flat_fast_warmup.get("cache_validation", {})
            seed_calls = int(warm_cache.get("cacheable_calls", 0))
            seed_hits = int(warm_cache.get("hits", 0))
            seed_misses = int(warm_cache.get("misses", 0))
            seed_uncacheable = int(warm_cache.get("uncacheable_calls", 0))
            seed_cache_ok = (seed_calls == 166 and seed_hits + seed_misses == 166
                             and seed_misses > 0 and seed_uncacheable == 0)
            if (not flat_fast_warmup.get("valid")
                    or {name: flat_fast_warmup.get(name) for name in EXPECTED_TEST_COUNTS}
                    != EXPECTED_TEST_COUNTS
                    or not flat_fast_warmup["workload_identity_match"]
                    or not seed_cache_ok):
                write_results(output_root, {**plan_record, "mode": "invalid-flat-fast-warmup",
                                            "host_release_note": host_release_note,
                                            "native_provenance": native_provenance,
                                            "syntax_probe": syntax_probe,
                                            "cache_clones": cache_clones,
                                            "flat_fast_warmup": flat_fast_warmup}, [])
                raise RuntimeError(f"Flat-fast unmeasured cache seed failed its gates: {flat_fast_warmup}")
            cache_clones["flat_fast_seed"] = {
                "source": str(flat_cache_destination), "destination": str(flat_cache_destination),
                "reused_in_place": True, "fresh_control_cache": True,
                "tree_sha256_before_measurements": cache_tree_hash(flat_cache_destination),
                "warmup_calls": seed_calls, "warmup_hits": seed_hits,
                "warmup_misses": seed_misses,
            }
        else:
            verify_java_options_log(Path(flat_fast_warmup["run_dir"]))

        ordinal_bias = 1 if resume_in_place and "protobuf_seed" in cache_clones else 0
        for pair_index, pair_order in enumerate(PAIR_ORDER, 1):
            for position, key in enumerate(pair_order, 1):
                stage = dict(stages[key])
                stage["dumper"] = str(native_tool) if key == "flatbuffers" else str(stage["dumper"])
                stage["label"] = "FlatBuffers · syntax-normalized" if key == "flatbuffers" else "Protobuf · original"
                ordinal = ordinal_bias + (pair_index - 1) * 2 + position
                result = comparison.run_clava_js(
                    stage, output_root, ordinal, measured=True, repeat=pair_index, mode="warm")
                identity = matrix_module.js_identity(result)
                identity_sha = canonical_hash(identity)
                result.update({"pair": pair_index, "position": position,
                               "fast_syntax": True, "test_identity": identity,
                               "test_identity_sha256": identity_sha,
                               "workload_identity_match": identity_sha == expected_identity,
                               "native_sha256": sha256_file(Path(stage["dumper"]))})
                verify_java_options_log(Path(result["run_dir"]))
                if result.get("stage") != key or result.get("mode") != "warm":
                    result["valid"] = False
                if {name: result.get(name) for name in EXPECTED_TEST_COUNTS} != EXPECTED_TEST_COUNTS:
                    result["valid"] = False
                cache = result.get("cache_validation", {})
                observed = {name: cache.get(name) for name in EXPECTED_CACHE}
                if observed != EXPECTED_CACHE:
                    result["valid"] = False
                expected_native_sha = (native_provenance["control_binary_sha256"]
                                       if key == "flatbuffers"
                                       else plan_record["stage_gates"]["protobuf"]["native_binary_sha256"])
                if result["native_sha256"] != expected_native_sha:
                    result["valid"] = False
                if not result["workload_identity_match"] or not result.get("valid"):
                    measured.append(result)
                    write_results(output_root, {**plan_record, "mode": "partial",
                                                "host_release_note": host_release_note,
                                                "native_provenance": native_provenance,
                                                "syntax_probe": syntax_probe,
                                                "cache_clones": cache_clones}, measured)
                    raise RuntimeError(f"invalid measured row retained at {result['run_dir']}")
                measured.append(result)
                write_results(output_root, {**plan_record, "mode": "running",
                                            "host_release_note": host_release_note,
                                            "native_provenance": native_provenance,
                                            "syntax_probe": syntax_probe,
                                            "cache_clones": cache_clones,
                                            "flat_fast_warmup": flat_fast_warmup,
                                            "runtime_overlay_gates": overlay_gates}, measured)
    finally:
        os.environ["JAVA_TOOL_OPTIONS"] = original_java_tool_options
        comparison.stage_runtime = original_stage_runtime

    expected_flat_overlays = len(PAIR_ORDER) if resume_in_place else len(PAIR_ORDER) + 1
    if len(measured) != 2 * len(PAIR_ORDER) or len(overlay_gates) != expected_flat_overlays:
        raise RuntimeError(f"incomplete run: measured={len(measured)}, Flat overlays={len(overlay_gates)}")

    pair_rows = []
    for pair_index in range(1, len(PAIR_ORDER) + 1):
        pair = [row for row in measured if row["pair"] == pair_index]
        by_stage = {row["stage"]: row for row in pair}
        pb = by_stage["protobuf"]
        flat = by_stage["flatbuffers"]
        pair_rows.append({"pair": pair_index, "pb_elapsed_s": pb["elapsed_s"],
                          "flat_fast_elapsed_s": flat["elapsed_s"],
                          "flat_minus_pb_s": flat["elapsed_s"] - pb["elapsed_s"],
                          "delta_pct_of_pb": 100 * (flat["elapsed_s"] - pb["elapsed_s"]) / pb["elapsed_s"]})
    final_plan = {**plan_record, "mode": "complete", "host_release_note": host_release_note,
                  "native_provenance": native_provenance, "syntax_probe": syntax_probe,
                  "cache_clones": cache_clones, "runtime_overlay_gates": overlay_gates,
                  "flat_fast_warmup": flat_fast_warmup,
                  "pb_cache_prime_excluded": cache_clones.get("protobuf_seed"),
                  "pair_deltas": pair_rows}
    write_results(output_root, final_plan, measured)
    with (output_root / "paired-deltas.csv").open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=["pair", "pb_elapsed_s", "flat_fast_elapsed_s",
                                                     "flat_minus_pb_s", "delta_pct_of_pb"])
        writer.writeheader()
        writer.writerows(pair_rows)
    return final_plan


def main() -> int:
    args = parse_args()
    matrix_file = args.matrix.resolve()
    output_root = args.output_root.resolve()
    matrix_data, matrix_module, comparison = load_dependencies(matrix_file)
    plan_record = source_preflight(matrix_data, matrix_module, comparison, output_root)
    plan_record["matrix_file"] = str(matrix_file)
    plan_record["matrix_sha256"] = sha256_file(matrix_file)
    plan_record["output_root"] = str(output_root)
    plan_record["suite"] = "clava-js"
    plan_record["cache_mode"] = "warm"
    plan_record["timing_boundary"] = "whole Clava-JS Vitest command /usr/bin/time elapsed_s"
    plan_record["comparison"] = "accepted PB original vs FlatBuffers with syntax-only validation normalized"
    plan_record["followup_not_original_stage_headline"] = True
    plan_record["flat_cache_seed_strategy"] = (
        "fresh cache for control native binary, populated by one unmeasured full-suite invocation; "
        "accepted Flat cache is never copied"
    )
    plan_record["host_release_note"] = args.host_release_note

    if not args.host_release_note:
        print(json.dumps(plan_record, indent=2))
        return 0
    complete = execute(matrix_data, matrix_module, comparison, output_root,
                       plan_record, args.host_release_note, args.reuse_seed_root)
    print(json.dumps({"mode": complete["mode"], "output_root": str(output_root),
                      "pair_deltas": complete["pair_deltas"]}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
