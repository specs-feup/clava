#!/usr/bin/env python3
"""Run the pinned Protobuf memory workload against an isolated final RC snapshot."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

# This command imports helpers from the canonical checkout. Keep it read-only.
sys.dont_write_bytecode = True

from support import MEMORY_WORKLOADS, normalize_memory_workload

MANIFEST_NAME = "clang-dumper-release-manifest.json"
SCHEMA_NAME = "clang-dumper-ast-wire.proto"
DESCRIPTOR_NAME = "clang-dumper-ast-wire.pb"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def resolve_executable(name: str) -> str | None:
    candidate = shutil.which(name)
    return str(Path(candidate).resolve()) if candidate else None


def tree_manifest(root: Path) -> dict[str, dict[str, int | str]]:
    return {
        path.relative_to(root).as_posix(): {"sha256": sha256(path), "size": path.stat().st_size}
        for path in sorted(root.rglob("*")) if path.is_file()
    }


def parse_time(path: Path) -> dict[str, object]:
    text = path.read_text(errors="replace")
    result: dict[str, object] = {"raw": text}
    patterns = {
        "user_s": r"User time \(seconds\): ([0-9.]+)",
        "sys_s": r"System time \(seconds\): ([0-9.]+)",
        "max_rss_kb": r"Maximum resident set size \(kbytes\): (\d+)",
        "exit_status": r"Exit status: (\d+)",
    }
    for key, pattern in patterns.items():
        match = re.search(pattern, text)
        if match:
            result[key] = float(match.group(1)) if key in ("user_s", "sys_s") else int(match.group(1))
    elapsed = re.search(r"Elapsed \(wall clock\) time .*?: ([0-9:]+(?:\.[0-9]+)?)", text)
    if elapsed:
        pieces = [float(piece) for piece in elapsed.group(1).split(":")]
        seconds = pieces[-1] + (pieces[-2] * 60 if len(pieces) > 1 else 0)
        if len(pieces) > 2:
            seconds += pieces[-3] * 3600
        result["elapsed_s"] = seconds
    return result


def load_release(snapshot: Path, cache_input: Path) -> tuple[str, bytes, dict, Path, dict[str, Path]]:
    selector = snapshot / "clava/ClangAstParser/clang-dumper-release.tag"
    tag = selector.read_text().strip()
    if not re.fullmatch(r"v[0-9]+(?:\.[0-9]+)+_[0-9]+-rc[0-9]+", tag):
        raise SystemExit(f"snapshot selector is not an RC release tag: {tag}")

    asset_root = snapshot / "clang-dumper/build"
    manifest_path = asset_root / MANIFEST_NAME
    if not manifest_path.is_file():
        raise SystemExit(f"published {tag} manifest has not been staged at {manifest_path}")
    manifest_bytes = manifest_path.read_bytes()
    manifest = json.loads(manifest_bytes)
    protocol = manifest["protocol"]
    assets = manifest["assets"]
    by_name = {entry["filename"]: entry for entry in assets}
    tool_matches = [entry for entry in assets if entry.get("kind") == "tool"
                    and entry.get("platform") == "linux" and entry.get("arch") == "x64"]
    if len(tool_matches) != 1:
        raise SystemExit(f"expected one Linux x64 tool in {tag} manifest, found {len(tool_matches)}")
    required = {
        MANIFEST_NAME: manifest_path,
        tool_matches[0]["filename"]: asset_root / tool_matches[0]["filename"],
        SCHEMA_NAME: asset_root / SCHEMA_NAME,
        DESCRIPTOR_NAME: asset_root / DESCRIPTOR_NAME,
    }
    expected_hashes = {
        MANIFEST_NAME: hashlib.sha256(manifest_bytes).hexdigest(),
        tool_matches[0]["filename"]: tool_matches[0]["sha256"],
        SCHEMA_NAME: protocol["schema_sha256"],
        DESCRIPTOR_NAME: protocol["descriptor_sha256"],
    }
    for name, path in required.items():
        if not path.is_file() or sha256(path) != expected_hashes[name]:
            raise SystemExit(f"published {tag} asset is missing or has the wrong hash: {path}")
    for name in (SCHEMA_NAME, DESCRIPTOR_NAME):
        if by_name.get(name, {}).get("kind") != "protocol":
            raise SystemExit(f"{name} is not a protocol asset in {tag} manifest")

    cache = cache_input.expanduser().resolve()
    candidates = [cache, cache / "clang-dumper", cache.parent, cache.parent.parent]
    seed_root = next((candidate for candidate in candidates
                      if (candidate / "releases" / tag / MANIFEST_NAME).is_file()), None)
    if seed_root is None:
        raise SystemExit(f"verified consumer resource cache has no releases/{tag}: {cache}")
    release_dir = seed_root / "releases" / tag
    if (release_dir / MANIFEST_NAME).read_bytes() != manifest_bytes:
        raise SystemExit("consumer resource cache manifest differs byte-for-byte from published manifest")
    for name, path in required.items():
        if name == MANIFEST_NAME:
            continue
        cached = release_dir / name
        if not cached.is_file() and by_name.get(name, {}).get("kind") == "protocol":
            # The consumer DUMPER_FOLDER carries the selected tool and exact
            # manifest; protocol artifacts are separately resolved by Gradle.
            # Their canonical bytes were independently verified in asset_root.
            continue
        if not cached.is_file() or sha256(cached) != expected_hashes[name]:
            raise SystemExit(f"consumer resource cache asset hash mismatch: {cached}")
    return tag, manifest_bytes, manifest, seed_root, required


def parse_probe_rows(log_path: Path) -> list[dict]:
    rows = []
    for line in log_path.read_text(errors="replace").splitlines():
        marker = "CLAVA_HEAP "
        if marker in line:
            rows.append(json.loads(line.split(marker, 1)[1]))
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--snapshot", type=Path, required=True,
                        help="isolated sibling-repository build snapshot")
    parser.add_argument("--output", type=Path, required=True,
                        help="new, empty memory evidence directory")
    parser.add_argument("--resource-cache", type=Path, required=True,
                        help="consumer_resume verified DUMPER_FOLDER/clang-dumper cache")
    parser.add_argument("--runtime", type=Path, default=None,
                        help="defaults to the isolated snapshot installDist output")
    parser.add_argument("--classes", type=Path, default=None,
                        help="directory containing the isolated ValidationProbe.class")
    args = parser.parse_args()

    snapshot = args.snapshot.expanduser().resolve()
    output = args.output.expanduser().resolve()
    runtime = (args.runtime or snapshot / "clava/ClavaWeaver/build/install/ClavaWeaver").resolve()
    classes = (args.classes or snapshot / "clava/ClangAstParser/build/protobuf-validation-classes").resolve()
    if output.exists():
        raise SystemExit(f"refusing to reuse memory output folder: {output}")
    if not (runtime / "lib/ClangAstParser.jar").is_file():
        raise SystemExit(f"final installDist runtime is missing: {runtime}")
    if not (classes / "ValidationProbe.class").is_file():
        raise SystemExit(f"final ValidationProbe classes are missing: {classes}")

    tag, manifest_bytes, manifest, seed_root, assets = load_release(snapshot, args.resource_cache)
    source_root = snapshot / "clava/ClangAstParser/test-resources"
    workloads = [dict(spec) for spec in MEMORY_WORKLOADS]
    for workload in workloads:
        source = source_root / workload["relative_source"]
        if not source.is_file():
            raise SystemExit(f"pinned memory workload is missing: {source}")
        workload["source"] = str(source)
        workload["source_sha256"] = sha256(source)
        if workload["source_sha256"] != workload["expected_sha256"]:
            raise SystemExit(f"pinned memory workload changed: {source}")

    java = resolve_executable("java")
    gnu_time = resolve_executable("/usr/bin/time") or resolve_executable("time")
    if not java or not gnu_time:
        raise SystemExit("java and GNU time are required")
    parser_source = snapshot / "clava/experiments/protobuf/validation/java/ValidationProbe.java"
    if not parser_source.is_file():
        raise SystemExit(f"ValidationProbe source is missing: {parser_source}")

    output.mkdir(parents=True)
    record = {
        "created_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "status": "running",
        "driver_sha256": sha256(Path(__file__)),
        "methodology": "two pinned source workloads; three fresh JVMs per workload; 20 strict-cleanup parse cycles per JVM",
        "total_parse_cycles": 120,
        "jvm_repeats_per_workload": 3,
        "cycles_per_jvm": 20,
        "workloads": workloads,
        "selector": {"tag": tag, "path": str(snapshot / "clava/ClangAstParser/clang-dumper-release.tag")},
        "published_manifest": {
            "path": str(snapshot / "clang-dumper/build" / MANIFEST_NAME),
            "sha256": hashlib.sha256(manifest_bytes).hexdigest(),
            "bytes": len(manifest_bytes),
            "native_tool_commit": subprocess.run(
                ["git", "-C", str(snapshot / "clang-dumper"), "rev-parse", "HEAD"],
                check=True, capture_output=True, text=True).stdout.strip(),
            "protocol": manifest.get("protocol"),
            "toolchain": manifest.get("toolchain"),
            "verified_assets": {path.name: sha256(path) for path in assets.values()},
        },
        "verified_resource_seed": {
            "root": str(seed_root),
            "tree": tree_manifest(seed_root),
            "tree_sha256": hashlib.sha256(json.dumps(tree_manifest(seed_root), sort_keys=True,
                                                       separators=(",", ":")).encode()).hexdigest(),
        },
        "runtime": {
            "path": str(runtime),
            "jars": {path.name: sha256(path) for path in sorted((runtime / "lib").glob("*.jar"))},
            "classes": str(classes),
            "classes_tree": tree_manifest(classes),
            "validation_probe_source_sha256": sha256(parser_source),
            "java_binary": java,
        },
        "jvm_options": ["-Xms256m", "-Xmx4g", "-XX:+UseG1GC", "-XX:-DisableExplicitGC"],
        "cache_policy": "CCACHE_DISABLE=true; AST_DUMP_CACHE=false in ValidationProbe",
        "observations": [],
    }

    try:
        ordinal = 0
        for workload in workloads:
            source = Path(workload["source"])
            for repeat in range(1, 4):
                ordinal += 1
                work = output / workload["key"] / f"repeat-{repeat}"
                work.mkdir(parents=True, exist_ok=False)
                temp = work / "tmp"
                temp.mkdir()
                resource_root = work / "resources"
                dest_cache = resource_root / "clang-dumper"
                shutil.copytree(seed_root, dest_cache)
                if tree_manifest(dest_cache) != tree_manifest(seed_root):
                    raise RuntimeError(f"repeat {repeat} resource cache copy differs from verified seed")
                copied_release = dest_cache / "releases" / tag
                if (copied_release / MANIFEST_NAME).read_bytes() != manifest_bytes:
                    raise RuntimeError(f"repeat {repeat} resource manifest changed during copy")
                for name, asset_source in assets.items():
                    target = copied_release / name
                    expected = sha256(asset_source)
                    if target.is_file() and sha256(target) != expected:
                        raise RuntimeError(f"repeat {repeat} cached release asset differs from verified asset: {target}")
                    if not target.is_file():
                        if name not in (SCHEMA_NAME, DESCRIPTOR_NAME):
                            raise RuntimeError(f"repeat {repeat} resource cache is missing required tool asset: {target}")
                        shutil.copy2(asset_source, target)
                copied_assets = {path.name: sha256(copied_release / path.name) for path in assets.values()}
                if copied_assets != {path.name: sha256(path) for path in assets.values()}:
                    raise RuntimeError(f"repeat {repeat} resource assets differ from published release")

                env = os.environ.copy()
                env.update({"TMPDIR": str(temp), "TMP": str(temp), "TEMP": str(temp),
                            "XDG_CACHE_HOME": str(work / "cache"), "CCACHE_DISABLE": "true"})
                (work / "cache").mkdir()
                command = [
                    gnu_time, "-v", "-o", str(work / "time.txt"), "--", java,
                    "-Xms256m", "-Xmx4g", "-XX:+UseG1GC", "-XX:-DisableExplicitGC",
                    "-Djava.io.tmpdir=" + str(temp), "-cp",
                    str(classes) + os.pathsep + str(runtime / "lib" / "*"),
                    "ValidationProbe", "memory", str(source), str(work), workload["standard"],
                    "20", "true", str(resource_root),
                ]
                (work / "command.json").write_text(json.dumps({"argv": command, "cwd": str(snapshot / "clava")}, indent=2) + "\n")
                start = time.perf_counter()
                with (work / "probe.log").open("w") as log:
                    completed = subprocess.run(command, cwd=snapshot / "clava", env=env,
                                               stdout=log, stderr=subprocess.STDOUT, check=False)
                wall_s = time.perf_counter() - start
                rows = parse_probe_rows(work / "probe.log")
                expected_repeats = list(range(1, 21))
                actual_repeats = [row.get("repeat") for row in rows]
                if completed.returncode != 0 or actual_repeats != expected_repeats:
                    raise RuntimeError(f"{workload['key']} repeat {repeat} failed: rc={completed.returncode}, cycles={actual_repeats}")
                for row in rows:
                    if (row.get("app_collected") is not True or row.get("mapped_paths_under_work") != 0
                            or row.get("leftover_clang_temp_folders") != 0 or row.get("open_parser_files") != 0
                            or row.get("unexpected_open_parser_fd_targets") != {}):
                        raise RuntimeError(f"strict cleanup failed in {workload['key']} repeat {repeat}: {row}")
                    if row.get("dumper_resource_root") != str(resource_root):
                        raise RuntimeError(f"ValidationProbe used unexpected DUMPER_FOLDER: {row.get('dumper_resource_root')}")
                observation = {
                    "workload": workload["key"], "repeat": repeat, "return_code": completed.returncode,
                    "wall_s": wall_s, "gnu_time": parse_time(work / "time.txt"),
                    "heap_phases": rows, "command": command, "log": str(work / "probe.log"),
                    "resource_root": str(resource_root), "resource_cache_tree_sha256": hashlib.sha256(
                        json.dumps(tree_manifest(dest_cache), sort_keys=True, separators=(",", ":")).encode()).hexdigest(),
                    "release_manifest_sha256": hashlib.sha256((copied_release / MANIFEST_NAME).read_bytes()).hexdigest(),
                    "release_asset_sha256": copied_assets,
                }
                record["observations"].append(observation)
                (output / "memory.json").write_text(json.dumps(record, indent=2) + "\n")
                print(json.dumps({"workload": workload["key"], "repeat": repeat,
                                  "cycles": len(rows), "valid": True}), flush=True)
        if len(record["observations"]) != 6 or sum(len(row["heap_phases"]) for row in record["observations"]) != 120:
            raise RuntimeError("final memory cohort does not contain 6 JVMs and 120 parse cycles")
        report_payloads = []
        for workload in workloads:
            payload = normalize_memory_workload(record, workload["key"])
            report_payloads.append(("NAS+" if workload["key"] == "nas-lu" else "C++ templates", payload))
        report_path = snapshot / "clava/experiments/protobuf/report/render_updated_benchmark.py"
        import importlib.util
        spec = importlib.util.spec_from_file_location("final_protobuf_memory_report", report_path)
        if spec is None or spec.loader is None:
            raise RuntimeError(f"could not load memory report validator: {report_path}")
        report_module = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = report_module
        spec.loader.exec_module(report_module)
        report_module._validate_memory_inputs(report_payloads)
        for workload, (_, payload) in zip(workloads, report_payloads):
            sidecar = output / ("memory-nas-lu.json" if workload["key"] == "nas-lu" else "memory-templates.json")
            sidecar.write_text(json.dumps(payload, indent=2) + "\n")
        record["report_payloads"] = {
            workload["key"]: str(output / ("memory-nas-lu.json" if workload["key"] == "nas-lu" else "memory-templates.json"))
            for workload in workloads
        }
        record["status"] = "complete"
        record["completed_utc"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
        (output / "memory.json").write_text(json.dumps(record, indent=2) + "\n")
        return 0
    except Exception as exc:
        record["status"] = "failed"
        record["error"] = repr(exc)
        (output / "memory.json").write_text(json.dumps(record, indent=2) + "\n")
        raise


if __name__ == "__main__":
    raise SystemExit(main())
