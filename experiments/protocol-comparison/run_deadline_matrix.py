#!/usr/bin/env python3
"""Run the approved serial full-suite AST protocol/cache matrix.

This runner refuses to start measured cells without an explicit host-lock
confirmation, frozen-runtime preparation, matching fixture hashes, and
matching test identities from every cell's untimed warm-up.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import shlex
import re
import statistics
import subprocess
import sys
import getpass
import shutil
import xml.etree.ElementTree as ET
from typing import Any

import run_comparison as comparison


SCRIPT_ROOT = Path(__file__).resolve().parent
DEADLINE_ROOT = SCRIPT_ROOT.parents[2]
SHARED_ROOT = DEADLINE_ROOT / "shared"
SPECS_ROOT = SHARED_ROOT / "specs-java-libs"
LARA_ROOT = SHARED_ROOT / "lara-framework"
PREPARATION_MANIFEST = DEADLINE_ROOT / "preparation.json"
STAGE_CONFIG = (
    {
        "key": "before-cache",
        "label": "Before cache",
        "root": DEADLINE_ROOT / "stages/before-cache",
        "dumper": None,
        "native_root": None,
        "dumper_revision": "published v18.1.8_4",
        "wire": "text",
        "cache": False,
    },
    {
        "key": "ccache-text",
        "label": "Text + ccache",
        "root": DEADLINE_ROOT / "stages/ccache-text",
        "dumper": Path("/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache/build/tool"),
        "native_root": Path("/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache"),
        "wire": "text",
        "cache": True,
    },
    {
        "key": "protobuf",
        "label": "Protobuf",
        "root": DEADLINE_ROOT / "stages/protobuf",
        "dumper": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clang-dumper/build/tool"),
        "native_root": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clang-dumper"),
        "wire": "protobuf",
        "cache": True,
    },
    {
        "key": "flatbuffers",
        "label": "eager FlatBuffers",
        "root": DEADLINE_ROOT / "stages/flatbuffers",
        "dumper": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers/clang-dumper/build/tool"),
        "native_root": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers/clang-dumper"),
        "wire": "flat-eager",
        "cache": True,
    },
)
EXPECTED = {
    "clava-js": {"total_tests": 164, "passed_tests": 158, "failed_tests": 0, "skipped_tests": 6},
    "java": {"total_tests": 116, "passed_tests": 116, "failed_tests": 0, "skipped_tests": 0},
}
MODES = ("direct", "cold", "warm")
SUITES = ("clava-js", "java")
FIXED_SPECSUTILS_REVISION = "19c8e3e4c81dbc77a89d77cd7ab2bc8bfc3844fa"
BASELINE_BINARY_SHA256 = "61cfc4bae90c24d59274a13fc5222946a234d86a55ae20dd38f5b01d951883b6"
BASELINE_BINARY_NAME = "clang-dumper-linux-x64"
BASELINE_RELEASE_TAG = "v18.1.8_4"


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def canonical_hash(value: Any) -> str:
    return sha256_bytes(json.dumps(value, sort_keys=True, separators=(",", ":")).encode())


def git(path: Path, *args: str) -> str:
    result = subprocess.run(["git", "-C", str(path), *args], text=True, capture_output=True)
    return result.stdout.strip() if result.returncode == 0 else "unknown"


def lara_bridge_overlay_identity() -> dict[str, str]:
    source = LARA_ROOT / "Lara-JS/api/LaraJoinPoint.ts"
    revision = git(LARA_ROOT, "rev-parse", "HEAD")
    patch = subprocess.run(
        ["git", "-C", str(LARA_ROOT), "show", "--format=", "--binary", "HEAD"],
        capture_output=True,
        check=True,
    ).stdout
    if revision == "unknown" or not source.is_file():
        raise SystemExit(f"cannot fingerprint the shared Lara-JS bridge source: {source}")
    return {
        "repository": str(LARA_ROOT.resolve()),
        "revision": revision,
        "patch_sha256": sha256_bytes(patch),
        "source_path": str(source.resolve()),
        "source_sha256": sha256_file(source),
    }


def verify_lara_bridge_overlay(plan: dict[str, Any]) -> None:
    expected = plan.get("lara_js_bridge_overlay")
    observed = lara_bridge_overlay_identity()
    if expected != observed:
        raise RuntimeError(
            "shared Lara-JS bridge source changed after its fingerprint was frozen: "
            f"expected={expected}, observed={observed}"
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=("preflight", "measure"), required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--repeat-count", type=int, default=4)
    parser.add_argument(
        "--repair-invalid", action="store_true",
        help="under an approved host lock, add one new attempt for each invalid measured cell that has no valid selected attempt",
    )
    parser.add_argument(
        "--host-lock-confirmed",
        action="store_true",
        help="required only after the parent has granted the exclusive measured-run host lock",
    )
    parser.add_argument(
        "--lock-note",
        default="",
        help="human-readable parent approval timestamp/reference recorded in the local plan",
    )
    return parser.parse_args()


def eligible_stages(mode: str) -> list[dict[str, Any]]:
    return [stage for stage in comparison.STAGES if mode == "direct" or stage["cache"]]


def stage_mode_schedule(repeat: int) -> list[tuple[str, dict[str, Any]]]:
    """Interleave cache states within each stage and rotate stage/mode starts."""
    all_stages = list(comparison.STAGES)
    rotate = repeat % len(all_stages)
    ordered_stages = all_stages[rotate:] + all_stages[:rotate]
    schedule: list[tuple[str, dict[str, Any]]] = []
    for stage_position, stage in enumerate(ordered_stages):
        modes = ["direct"]
        if stage["cache"]:
            modes += ["cold", "warm"]
            offset = (repeat + stage_position) % len(modes)
            modes = modes[offset:] + modes[:offset]
        schedule.extend((mode, stage) for mode in modes)
    return schedule


def suite_interleaved_schedule(repeat: int) -> list[tuple[str, str, dict[str, Any]]]:
    schedule: list[tuple[str, str, dict[str, Any]]] = []
    for position, (mode, stage) in enumerate(stage_mode_schedule(repeat)):
        suite_order = SUITES if (repeat + position) % 2 == 0 else tuple(reversed(SUITES))
        schedule.extend((suite, mode, stage) for suite in suite_order)
    return schedule


def fixture_maps(stages: list[dict[str, Any]]) -> dict[str, dict[str, dict[str, str]]]:
    categories = {
        "java_parser_resources": lambda clava: clava / "ClangAstParser" / "test-resources",
        "js_imported_resources": lambda clava: clava / "ClavaWeaver" / "resources" / "clava" / "test",
        "clava_js_sources": lambda clava: clava / "Clava-JS",
    }
    maps: dict[str, dict[str, dict[str, str]]] = {name: {} for name in categories}
    for stage in stages:
        clava = Path(stage["root"]) / "clava"
        for category, root_fn in categories.items():
            root = root_fn(clava)
            files: dict[str, str] = {}
            for path in sorted(root.rglob("*")):
                if not path.is_file() or "node_modules" in path.parts or "java-binaries" in path.parts:
                    continue
                if category == "clava_js_sources" and path.suffix != ".ts":
                    continue
                files[path.relative_to(root).as_posix()] = sha256_file(path)
            maps[category][stage["key"]] = files
    return maps


def fixture_comparison(fingerprints: dict[str, dict[str, dict[str, str]]]) -> dict[str, Any]:
    report: dict[str, Any] = {"passed": True, "categories": {}, "mismatches": []}
    for category, stages in fingerprints.items():
        key_order = [stage["key"] for stage in comparison.STAGES]
        reference_key = key_order[0]
        reference = stages[reference_key]
        category_report = {
            "reference_stage": reference_key,
            "reference_sha256": canonical_hash(reference),
            "path_counts": {key: len(value) for key, value in stages.items()},
            "stage_sha256": {key: canonical_hash(value) for key, value in stages.items()},
        }
        report["categories"][category] = category_report
        for key, values in stages.items():
            missing = sorted(set(reference) - set(values))
            extra = sorted(set(values) - set(reference))
            changed = sorted(path for path in set(reference) & set(values) if reference[path] != values[path])
            if missing or extra or changed:
                report["passed"] = False
                mismatch = {"category": category, "stage": key, "missing": missing, "extra": extra, "changed": changed}
                report["mismatches"].append(mismatch)
    return report


def java_identity(result: dict[str, Any], stage: dict[str, Any]) -> list[dict[str, str]]:
    root = Path(stage["root"]) / "clava" / "ClangAstParser" / "build" / "test-results" / "test"
    identities: list[dict[str, str]] = []
    for path in sorted(root.glob("*.xml")):
        suite = ET.parse(path).getroot()
        for case in suite.findall("testcase"):
            status = "failed" if case.find("failure") is not None else "error" if case.find("error") is not None else "skipped" if case.find("skipped") is not None else "passed"
            identities.append({
                "class": case.attrib.get("classname", ""),
                "name": case.attrib.get("name", ""),
                "status": status,
            })
    return sorted(identities, key=lambda item: (item["class"], item["name"], item["status"]))


def js_identity(result: dict[str, Any]) -> list[dict[str, str]]:
    report_path = Path(result["run_dir"]) / "vitest.json"
    if not report_path.is_file():
        return []
    report = json.loads(report_path.read_text())
    identities: list[dict[str, str]] = []
    for test_file in report.get("testResults", []):
        raw_path = str(test_file.get("name", ""))
        anchor = "/Clava-JS/"
        relative_path = raw_path.split(anchor, 1)[1] if anchor in raw_path else Path(raw_path).name
        for assertion in test_file.get("assertionResults", []):
            identities.append({
                "file": relative_path,
                "test": str(assertion.get("fullName") or assertion.get("title") or ""),
                "status": str(assertion.get("status", "unknown")).lower(),
            })
    return sorted(identities, key=lambda item: (item["file"], item["test"], item["status"]))


def compile_tasks_clean(run_dir: Path) -> tuple[bool, list[str]]:
    log = (run_dir / "run.log").read_text(errors="replace") if (run_dir / "run.log").is_file() else ""
    bad: list[str] = []
    for line in log.splitlines():
        if "> Task " not in line or not any(task in line for task in (":compileJava", ":compileTestJava")):
            continue
        allowed = ("UP-TO-DATE", "FROM-CACHE", "NO-SOURCE", "SKIPPED")
        if not any(status in line for status in allowed):
            bad.append(line.strip())
    return not bad, bad


def compression_record(stage: dict[str, Any], mode: str) -> dict[str, Any]:
    if stage["key"] == "flatbuffers":
        return {
            "payload_format": "FlatBuffers",
            "payload_compressed": False,
            "ccache_enabled": mode != "direct",
            "ccache_internal_compression": mode != "direct",
            "basis": "FlatBuffers serialization emits raw buffers; WireMode enables ccache COMPRESS for ccache modes",
        }
    if stage["key"] == "before-cache" or mode == "direct":
        return {
            "payload_format": "text AST dump",
            "payload_compressed": False,
            "ccache_enabled": False,
            "ccache_internal_compression": False,
            "basis": "AST dump cache path is disabled for the published baseline or CCACHE_DISABLE=true",
        }
    return {
        "payload_format": "text AST dump" if stage["key"] == "ccache-text" else "Protobuf",
        "payload_compressed": True,
        "payload_compression": "zstd",
        "ccache_enabled": True,
        "ccache_internal_compression": False,
        "basis": "effective AST_DUMP_CACHE path adds -ast-dump-compression=zstd; adapter sets CCACHE_NOCOMPRESS",
    }


def native_binary_for_run(stage: dict[str, Any], mode_root: Path, suite: str) -> Path:
    if stage["dumper"] is not None:
        return Path(stage["dumper"])
    if suite == "clava-js":
        cache_root = mode_root / "cache" / suite / stage["key"]
        return (
            cache_root / "@specs-feup/clava" / "clang-dumper" / "releases"
            / BASELINE_RELEASE_TAG / BASELINE_BINARY_NAME
        )
    temp_root = mode_root / "temp" / suite / stage["key"]
    return (
        temp_root / f"clang_ast_exe_{getpass.getuser()}" / "clang-dumper" / "releases"
        / BASELINE_RELEASE_TAG / BASELINE_BINARY_NAME
    )


def result_metadata(
    result: dict[str, Any], stage: dict[str, Any], mode: str, suite: str,
    measured: bool, repeat: int | None, attempt: int,
    fingerprints: dict[str, dict[str, dict[str, str]]],
) -> dict[str, Any]:
    stage_manifest = next(item for item in plan_stage_metadata if item["key"] == stage["key"])
    run_dir = Path(result["run_dir"])
    if suite == "java":
        test_ids = java_identity(result, stage)
        clean, compile_violations = compile_tasks_clean(run_dir)
        result["junit_aggregate_s"] = result.get("junit_aggregate_s", 0.0)
        result["gradle_non_test_elapsed_s"] = max(0.0, float(result.get("elapsed_s", 0.0)) - float(result["junit_aggregate_s"]))
        result["compile_task_violations"] = compile_violations
    else:
        test_ids = js_identity(result)
        clean = True
        result["junit_aggregate_s"] = None
        result["gradle_non_test_elapsed_s"] = None
        result["compile_task_violations"] = []

    result["cell_id"] = f"{suite}/{mode}/{stage['key']}/{'repeat-' + format(repeat, '02d') if measured else 'warmup'}"
    result["attempt"] = attempt
    result["test_identity"] = test_ids
    result["test_identity_sha256"] = canonical_hash(test_ids)
    result["fixture_fingerprint_sha256"] = canonical_hash({
        category: values[stage["key"]]
        for category, values in fingerprints.items()
    })
    result["compile_tasks_clean"] = clean
    result["compression"] = compression_record(stage, mode)
    result["clava_revision"] = stage_manifest["clava_revision"]
    result["clava_patch_sha256"] = stage_manifest["clava_patch_sha256"]
    result["native_revision"] = stage_manifest["dumper_revision"]
    native_binary = native_binary_for_run(stage, OUTPUT_ROOT / mode, suite)
    native_binary_sha = sha256_file(native_binary) if native_binary.is_file() else None
    expected_native_sha = BASELINE_BINARY_SHA256 if stage["key"] == "before-cache" else stage_manifest["dumper_sha256"]
    result["native_binary_path"] = str(native_binary.resolve()) if native_binary.is_file() else None
    result["native_binary_sha256"] = native_binary_sha
    result["native_binary_matches_expected"] = native_binary_sha == expected_native_sha
    result["native_build_provenance_sha256"] = canonical_hash(stage_manifest["native_build"])
    result["native_schema_sha256"] = stage_manifest["native_build"]["schema_files_sha256"]
    result["native_build_type"] = stage_manifest["native_build"].get("cmake_build_type", stage_manifest["native_build"].get("build_type"))
    result["native_optimization_flags"] = stage_manifest["native_build"].get("optimization_flags", [])
    result["llvm_version"] = stage_manifest["native_build"].get("llvm_version")
    stage_parser_jar = Path(stage["root"]) / "clava" / "Clava-JS" / "java-binaries" / "lib" / "ClangAstParser.jar"
    result["parser_jar_sha256"] = sha256_file(stage_parser_jar)
    result["specsutils_runtime_jar_sha256"] = stage_manifest["runtime_specsutils"]["jar_sha256"]
    result["runtime_manifest_sha256"] = stage_manifest["runtime_manifest"]["sha256"]
    result["specsutils_revision"] = stage_manifest["java_build_dependencies"]["specs_java_libs"]["revision"]
    result["specsutils_jar_sha256"] = stage_manifest["java_build_dependencies"]["specs_java_libs"]["specsutils_jar_sha256"]
    result["run_dir_relative"] = run_dir.relative_to(OUTPUT_ROOT).as_posix()
    result["valid"] = bool(result["valid"] and clean and result["native_binary_matches_expected"])
    return result


def write_primary_csv(results: list[dict[str, Any]], destination: Path) -> None:
    fields = [
        "cell_id", "suite", "mode", "stage", "measured", "repeat", "attempt", "selected", "superseded_by_attempt", "valid",
        "elapsed_s", "driver_elapsed_s", "user_s", "sys_s", "max_rss_kb",
        "total_tests", "passed_tests", "failed_tests", "skipped_tests", "junit_aggregate_s",
        "gradle_non_test_elapsed_s", "return_code", "cacheable_calls", "cache_hits", "cache_misses",
        "uncacheable_calls", "cache_validation_passed", "cache_validation_reason", "failure_names",
        "test_identity_sha256", "fixture_fingerprint_sha256", "compile_tasks_clean",
        "clava_revision", "clava_patch_sha256", "native_revision", "native_binary_sha256", "native_binary_matches_expected",
        "native_build_provenance_sha256", "native_schema_sha256", "native_build_type", "native_optimization_flags", "llvm_version",
        "parser_jar_sha256", "runtime_manifest_sha256", "specsutils_revision", "specsutils_jar_sha256",
        "specsutils_runtime_jar_sha256",
        "payload_format", "payload_compressed", "ccache_enabled", "ccache_internal_compression",
        "workload_identity_match", "run_dir_relative",
    ]
    with destination.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        for result in results:
            row = dict(result)
            row.update(result.get("compression", {}))
            row["failure_names"] = json.dumps(result.get("failure_names", []), separators=(",", ":"))
            row["cache_validation_passed"] = result.get("cache_validation", {}).get("passed")
            row["cache_validation_reason"] = result.get("cache_validation", {}).get("reason")
            writer.writerow(row)


def percentile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    position = (len(ordered) - 1) * fraction
    lower = int(position)
    upper = min(lower + 1, len(ordered) - 1)
    blend = position - lower
    return ordered[lower] * (1 - blend) + ordered[upper] * blend


def write_summary_csv(results: list[dict[str, Any]], destination: Path, repeat_count: int) -> None:
    fields = [
        "suite", "mode", "stage", "expected_total_tests", "expected_passed_tests", "expected_skipped_tests",
        "measured_attempts", "measurement_attempt_rows", "valid_attempts", "failed_attempts", "median_elapsed_s", "q1_elapsed_s", "q3_elapsed_s",
        "median_junit_aggregate_s", "warmup_elapsed_s", *[f"repeat{n}_elapsed_s" for n in range(1, repeat_count + 1)],
    ]
    rows: list[dict[str, Any]] = []
    for suite in SUITES:
        for mode in MODES:
            for stage in eligible_stages(mode):
                cell = [item for item in results if item["suite"] == suite and item["mode"] == mode and item["stage"] == stage["key"]]
                warmups = [item for item in cell if not item["measured"]]
                repeats = {
                    int(item["repeat"]): item for item in cell
                    if item["measured"] and item.get("selected") is True
                }
                attempts_for_cell = [item for item in cell if item.get("measured")]
                valid = [float(item["elapsed_s"]) for item in repeats.values() if item["valid"] and "elapsed_s" in item]
                expected = EXPECTED[suite]
                row: dict[str, Any] = {
                    "suite": suite,
                    "mode": mode,
                    "stage": stage["key"],
                    "expected_total_tests": expected["total_tests"],
                    "expected_passed_tests": expected["passed_tests"],
                    "expected_skipped_tests": expected["skipped_tests"],
                    "measured_attempts": len(repeats),
                    "measurement_attempt_rows": len(attempts_for_cell),
                    "valid_attempts": len(valid),
                    "failed_attempts": sum(not item["valid"] for item in attempts_for_cell),
                    "median_elapsed_s": statistics.median(valid) if valid else "",
                    "q1_elapsed_s": percentile(valid, 0.25) if valid else "",
                    "q3_elapsed_s": percentile(valid, 0.75) if valid else "",
                    "median_junit_aggregate_s": statistics.median([
                        float(item["junit_aggregate_s"]) for item in repeats.values()
                        if item["valid"] and item.get("junit_aggregate_s") is not None
                    ]) if suite == "java" and valid else "",
                    "warmup_elapsed_s": warmups[0].get("elapsed_s", "") if warmups else "",
                }
                for repeat in range(1, repeat_count + 1):
                    item = repeats.get(repeat)
                    row[f"repeat{repeat}_elapsed_s"] = item.get("elapsed_s", "") if item and item["valid"] else ""
                rows.append(row)
    with destination.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def checkpoint(results: list[dict[str, Any]], plan: dict[str, Any], identity: dict[str, Any]) -> None:
    (OUTPUT_ROOT / "results.json").write_text(json.dumps({
        "schema_version": 1,
        "plan": plan,
        "identity_preflight": identity,
        "results": results,
    }, indent=2) + "\n")
    write_primary_csv(results, OUTPUT_ROOT / "primary-runs.csv")
    write_summary_csv(results, OUTPUT_ROOT / "summary.csv", int(plan["repeat_count"]))


def run_one(
    suite: str, mode: str, stage: dict[str, Any], measured: bool, repeat: int | None,
    results: list[dict[str, Any]], plan: dict[str, Any], identity: dict[str, Any],
    fingerprints: dict[str, dict[str, dict[str, str]]], jfr_path: Path | None = None,
    attempt: int = 1,
) -> dict[str, Any]:
    verify_lara_bridge_overlay(plan)
    mode_root = OUTPUT_ROOT / mode
    mode_root.mkdir(parents=True, exist_ok=True)
    ordinal_key = f"{suite}/{mode}"
    ordinal = 1 + sum(item["suite"] == suite and item["mode"] == mode for item in results)
    runner = comparison.run_clava_js if suite == "clava-js" else comparison.run_java
    if suite == "java" and jfr_path is not None:
        result = runner(
            stage, mode_root, ordinal, measured, repeat, mode, jfr_path=jfr_path,
            jfr_init_script=JFR_INIT_SCRIPT,
        )
    else:
        result = runner(stage, mode_root, ordinal, measured, repeat, mode)
    verify_lara_bridge_overlay(plan)
    result = result_metadata(result, stage, mode, suite, measured, repeat, attempt=attempt, fingerprints=fingerprints)

    expected = EXPECTED[suite]
    if {key: result.get(key) for key in expected} != expected:
        result["valid"] = False
    identity_ids = result["test_identity"]
    expected_ids = identity.setdefault("reference_test_ids", {}).get(suite)
    if not measured:
        signature = result["test_identity_sha256"]
        cell_ids = identity.setdefault("warmup_test_identities", {}).setdefault(suite, {})
        cell_ids[result["cell_id"]] = {"sha256": signature, "count": len(identity_ids)}
        identity.setdefault("_identity_sets", {}).setdefault(suite, {})[result["cell_id"]] = identity_ids
    if expected_ids is not None:
        result["workload_identity_match"] = result["test_identity_sha256"] == expected_ids["sha256"]
        if not result["workload_identity_match"]:
            result["valid"] = False
    else:
        result["workload_identity_match"] = None
    if measured and not result["compile_tasks_clean"]:
        result["valid"] = False
    if jfr_path is not None:
        jfr_tool = shutil.which("jfr")
        recording = subprocess.run(
            [jfr_tool, "print", "--events", "jdk.SystemGC", str(jfr_path)],
            text=True, capture_output=True, check=False,
        ) if jfr_tool and jfr_path.is_file() else None
        system_gc_count = sum(
            line.strip() == "jdk.SystemGC {" for line in recording.stdout.splitlines()
        ) if recording is not None else None
        result["system_gc_request_count"] = system_gc_count
        result["system_gc_jfr_verified"] = bool(recording is not None and recording.returncode == 0)
        result["jfr_path_relative"] = jfr_path.relative_to(OUTPUT_ROOT).as_posix()
        if not result["system_gc_jfr_verified"] or system_gc_count != 0:
            result["valid"] = False
    result["selected"] = bool(result["valid"])
    result["superseded_by_attempt"] = None
    results.append(result)
    print(json.dumps({key: value for key, value in result.items() if key not in {"command", "test_identity"}}, sort_keys=True), flush=True)
    checkpoint(results, plan, identity)
    return result


def set_reference_identities(identity: dict[str, Any], results: list[dict[str, Any]]) -> None:
    identity["reference_test_ids"] = {}
    for suite in SUITES:
        cells = identity.get("warmup_test_identities", {}).get(suite, {})
        full_sets = identity.get("_identity_sets", {}).get(suite, {})
        if not cells:
            identity["mismatches"].append({"suite": suite, "reason": "no warm-up test identity recorded"})
            continue
        first_id = sorted(cells)[0]
        reference_hash = cells[first_id]["sha256"]
        reference_set = full_sets[first_id]
        identity["reference_test_ids"][suite] = {
            "source_cell": first_id,
            "sha256": reference_hash,
            "count": len(reference_set),
        }
        for cell_id, metadata in cells.items():
            if metadata["sha256"] != reference_hash:
                identity["mismatches"].append({
                    "suite": suite,
                    "cell_id": cell_id,
                    "reference_cell": first_id,
                    "reference_sha256": reference_hash,
                    "observed_sha256": metadata["sha256"],
                    "missing_ids": sorted({json.dumps(value, sort_keys=True) for value in reference_set} - {json.dumps(value, sort_keys=True) for value in full_sets[cell_id]}),
                    "extra_ids": sorted({json.dumps(value, sort_keys=True) for value in full_sets[cell_id]} - {json.dumps(value, sort_keys=True) for value in reference_set}),
                })
    for result in results:
        if result["measured"]:
            continue
        expected = identity.get("reference_test_ids", {}).get(result["suite"])
        result["workload_identity_match"] = bool(expected and result["test_identity_sha256"] == expected["sha256"])
        if not result["workload_identity_match"]:
            result["valid"] = False
        result["selected"] = bool(result["valid"])
    identity["passed"] = identity["passed"] and not identity["mismatches"]
    identity.pop("_identity_sets", None)


def load_preparation() -> dict[str, Any]:
    if not PREPARATION_MANIFEST.is_file():
        raise SystemExit(f"missing frozen-runtime preparation manifest: {PREPARATION_MANIFEST}")
    manifest = json.loads(PREPARATION_MANIFEST.read_text())
    by_stage: dict[str, set[str]] = {}
    for record in manifest.get("records", []):
        if record.get("task") in {"runtime_install", "java_test_classes"} and record.get("return_code") == 0:
            by_stage.setdefault(str(record["stage"]), set()).add(str(record["task"]))
    required = {"runtime_install", "java_test_classes"}
    missing = {stage["key"]: sorted(required - by_stage.get(stage["key"], set())) for stage in STAGE_CONFIG if required - by_stage.get(stage["key"], set())}
    if missing:
        raise SystemExit(f"runtime preparation incomplete: {missing}")
    common = [item for item in manifest.get("records", []) if item.get("task") == "common_dependency_identity"]
    if len(common) != 1:
        raise SystemExit("preparation manifest lacks a unique shared dependency identity record")
    common = common[0]
    expected_common = {
        "specs_java_libs_revision": git(SPECS_ROOT, "rev-parse", "HEAD"),
        "specs_java_libs_diff_sha256": sha256_bytes(subprocess.run(
            ["git", "-C", str(SPECS_ROOT), "diff", "--binary", "HEAD"], capture_output=True, check=True,
        ).stdout),
        "specsutils_jar_sha256": sha256_file(SPECS_ROOT / "SpecsUtils/build/libs/SpecsUtils.jar"),
        "joptions_jar_sha256": sha256_file(SPECS_ROOT / "jOptions/build/libs/jOptions.jar"),
        "lara_framework_revision": git(LARA_ROOT, "rev-parse", "HEAD"),
        "lara_framework_diff_sha256": sha256_bytes(subprocess.run(
            ["git", "-C", str(LARA_ROOT), "diff", "--binary", "HEAD"], capture_output=True, check=True,
        ).stdout),
    }
    for field, value in expected_common.items():
        if common.get(field) != value:
            raise SystemExit(f"common dependency identity changed after build preparation: {field}")
    return manifest


def static_gc_gate(stages: list[dict[str, Any]]) -> dict[str, Any]:
    report: dict[str, Any] = {"passed": True, "stages": {}, "explicit_true_overrides": []}
    override_patterns = (
        re.compile(r"(?:set|put)\s*\([^\n;]*SHOW_EXEC_INFO\s*,\s*true\s*\)", re.IGNORECASE),
        re.compile(r"(?:showExecInfo|SHOW_EXEC_INFO)\s*[:=]\s*true", re.IGNORECASE),
    )
    for stage in stages:
        clava = Path(stage["root"]) / "clava"
        parser_root = clava / "ClangAstParser"
        key_path = parser_root / "src/pt/up/fe/specs/clang/codeparser/CodeParser.java"
        key_source = key_path.read_text(errors="replace")
        default_false = bool(re.search(
            r"SHOW_EXEC_INFO\s*=.*?\.setDefault\(\(\)\s*->\s*false\)", key_source, re.DOTALL
        ))
        matches = []
        for path in [*parser_root.rglob("*.java"), *clava.joinpath("Clava-JS").rglob("*.ts")]:
            if "build" in path.parts or "node_modules" in path.parts:
                continue
            source = path.read_text(errors="replace")
            if any(pattern.search(source) for pattern in override_patterns):
                matches.append(str(path.relative_to(clava)))
        report["stages"][stage["key"]] = {"show_exec_info_default_false": default_false, "files_with_explicit_true": matches}
        report["explicit_true_overrides"].extend({"stage": stage["key"], "path": path} for path in matches)
        if not default_false or matches:
            report["passed"] = False
    return report


def native_build_provenance(stage: dict[str, Any]) -> dict[str, Any]:
    if stage["native_root"] is None:
        return {
            "build_type": "published-release (historical deployment binary)",
            "build_flags": "not available; binary not rebuilt",
            "llvm_version": "18.1.8 (release tag context)",
            "producer_schema_commit": stage["dumper_revision"],
            "schema_files": [],
            "schema_files_sha256": canonical_hash({"wire_format": "text AST dump"}),
            "llvm_linkage": "published artifact linkage not reconstructed",
        }

    root = Path(stage["native_root"])
    build = root / "build"
    cache_path = build / "CMakeCache.txt"
    cache: dict[str, str] = {}
    for line in cache_path.read_text(errors="replace").splitlines():
        if line.startswith("//") or line.startswith("#") or ":" not in line or "=" not in line:
            continue
        key_type, value = line.split("=", 1)
        cache[key_type.split(":", 1)[0]] = value
    commands = json.loads((build / "compile_commands.json").read_text())
    representative = next((item for item in commands if "src/Clang/ClangAst.cpp" in item.get("file", "")), None)
    if representative is None:
        raise SystemExit(f"cannot find a representative Clang compile command for {stage['key']}")
    command_text = representative.get("command") or shlex.join(representative.get("arguments", []))
    arguments = shlex.split(command_text)
    compiler = arguments[0]
    opt_flags = [arg for arg in arguments if re.fullmatch(r"-O[0-3s]", arg)]
    build_flags = [arg for arg in arguments if arg in {"-DNDEBUG", "-g", "-g0", "-fPIC", "-fPIE"} or re.fullmatch(r"-O[0-3s]", arg)]
    compiler_version = subprocess.run([compiler, "--version"], text=True, capture_output=True, check=False)
    llvm_config = shutil.which("llvm-config-18")
    llvm_version = subprocess.run([llvm_config, "--version"], text=True, capture_output=True, check=False) if llvm_config else None
    schema_roots = []
    if stage["key"] == "protobuf":
        schema_roots = [root / "wire/clava_ast_wire.proto"]
    elif stage["key"] == "flatbuffers":
        schema_roots = sorted((root / "wire").rglob("*.fbs"))
    schema_files = {
        path.relative_to(root).as_posix(): sha256_file(path)
        for path in schema_roots if path.is_file()
    }
    linkage = subprocess.run(["ldd", str(stage["dumper"])], text=True, capture_output=True, check=False)
    llvm_shared = [line.strip() for line in linkage.stdout.splitlines() if "LLVM" in line or "clang" in line.lower()]
    return {
        "cmake_build_type": cache.get("CMAKE_BUILD_TYPE", "unknown"),
        "cmake_cxx_flags": cache.get("CMAKE_CXX_FLAGS", ""),
        "cmake_compiler": cache.get("CMAKE_CXX_COMPILER", compiler),
        "compiler_version": compiler_version.stdout.splitlines()[0] if compiler_version.stdout else "unknown",
        "optimization_flags": opt_flags,
        "effective_compile_flags": build_flags,
        "representative_compile_command_sha256": sha256_bytes(command_text.encode()),
        "compile_command_count": len(commands),
        "llvm_dir": cache.get("LLVM_DIR", "unknown"),
        "llvm_version": llvm_version.stdout.strip() if llvm_version and llvm_version.returncode == 0 else "unknown",
        "llvm_linkage": "shared: " + "; ".join(llvm_shared) if llvm_shared else "no LLVM/Clang shared-library dependency in ldd output",
        "producer_schema_commit": stage["dumper_revision"],
        "schema_files": schema_files,
        "schema_files_sha256": canonical_hash(schema_files),
    }


def create_jfr_init_script(output_root: Path) -> tuple[Path, Path]:
    java = subprocess.run(["java", "-XshowSettings:properties", "-version"], text=True, capture_output=True, check=False)
    home_line = next((line for line in (java.stdout + java.stderr).splitlines() if "java.home =" in line), None)
    if java.returncode != 0 or home_line is None:
        raise SystemExit("could not find the active Java home for JFR preflight")
    profile = Path(home_line.split("=", 1)[1].strip()) / "lib" / "jfr" / "profile.jfc"
    if not profile.is_file() or shutil.which("jfr") is None:
        raise SystemExit(f"JFR preflight unavailable: profile={profile}, jfr={shutil.which('jfr')}")
    profile_xml = ET.parse(profile).getroot()
    system_gc_config = next((event for event in profile_xml.iter("event") if event.attrib.get("name") == "jdk.SystemGC"), None)
    enabled = system_gc_config.find("setting[@name='enabled']") if system_gc_config is not None else None
    if enabled is None or (enabled.text or "").strip().lower() != "true":
        raise SystemExit("JFR profile does not enable jdk.SystemGC; cannot validate explicit GC requests")
    init_script = output_root / "jfr" / "deadline-jfr.init.gradle"
    init_script.parent.mkdir(parents=True, exist_ok=True)
    init_script.write_text(
        "gradle.projectsEvaluated {\n"
        "  gradle.rootProject.allprojects.each { project ->\n"
        "    project.tasks.withType(org.gradle.api.tasks.testing.Test).configureEach { task ->\n"
        "      if (task.name == 'test' && System.getenv('DEADLINE_JFR_PATH')) {\n"
        "        task.maxParallelForks = 1\n"
        "        task.jvmArgs(\"-XX:StartFlightRecording=filename=${System.getenv('DEADLINE_JFR_PATH')},settings=${System.getenv('DEADLINE_JFR_SETTINGS')},dumponexit=true\")\n"
        "      }\n"
        "    }\n"
        "  }\n"
        "}\n"
    )
    return init_script, profile


def main() -> int:
    global OUTPUT_ROOT, plan_stage_metadata, JFR_INIT_SCRIPT
    args = parse_args()
    if args.repeat_count != 4:
        raise SystemExit("the approved design requires exactly four measured repeats")
    OUTPUT_ROOT = args.output_root.resolve()
    if args.repair_invalid and args.phase != "measure":
        raise SystemExit("--repair-invalid is valid only with --phase measure")
    if args.phase == "measure":
        if not args.host_lock_confirmed:
            raise SystemExit("refusing measured commands: wait for parent approval of the exclusive host lock")
        if not args.lock_note.strip():
            raise SystemExit("--lock-note must record the parent host-lock approval reference/time")
        if not (OUTPUT_ROOT / "results.json").is_file():
            raise SystemExit(f"measurement phase requires a completed preflight manifest under {OUTPUT_ROOT}")
    elif args.host_lock_confirmed or args.lock_note.strip():
        raise SystemExit("host-lock confirmation is only valid for the measured phase")
    elif OUTPUT_ROOT.exists():
        raise SystemExit(f"preflight refuses to reuse an existing output root: {OUTPUT_ROOT}")

    os.environ["SPECS_JAVA_LIBS_HOME"] = str(SPECS_ROOT)
    os.environ["LARA_FRAMEWORK_HOME"] = str(LARA_ROOT)
    comparison.FIXED_SPECSUTILS_REVISION = FIXED_SPECSUTILS_REVISION
    comparison.STAGES = tuple(dict(stage) for stage in STAGE_CONFIG)

    preparation = load_preparation()
    bridge_overlay = lara_bridge_overlay_identity()
    current_stages = comparison.validate_stages({stage["key"] for stage in comparison.STAGES})
    for item in current_stages:
        if item["key"] == "before-cache":
            item["dumper_sha256"] = BASELINE_BINARY_SHA256
            item["native_artifact_note"] = "published v18.1.8_4 x64 asset; per-run copied resource hash is checked"
        item["native_binary_sha256"] = item["dumper_sha256"]
        item["native_revision"] = item["dumper_revision"]
        item["native_build"] = native_build_provenance(item)
        item["parser_jar_sha256"] = sha256_file(
            Path(item["root"]) / "clava" / "Clava-JS/java-binaries/lib/ClangAstParser.jar"
        )
        item["runtime_manifest_sha256"] = item["runtime_manifest"]["sha256"]
        specs = item["java_build_dependencies"]["specs_java_libs"]
        item["specsutils_revision"] = specs["revision"]
        item["specsutils_jar_sha256"] = specs["specsutils_jar_sha256"]
    preparation_records = preparation.get("records", [])
    frozen_runtime_records = {
        item["stage"]: item
        for item in preparation_records if item.get("task") == "frozen_runtime_identity"
    }
    prepared_jars = {
        item["stage"]: item.get("runtime_manifest_jars")
        for item in preparation_records if item.get("task") == "frozen_runtime_identity"
    }
    for item in current_stages:
        frozen = frozen_runtime_records.get(item["key"])
        if not isinstance(frozen, dict):
            raise SystemExit(f"missing frozen runtime identity record for {item['key']}")
        item["clava_patch_sha256"] = frozen.get("clava_patch_sha256")
        if not item["clava_patch_sha256"]:
            raise SystemExit(f"frozen runtime identity lacks Clava overlay hash for {item['key']}")
        jars = prepared_jars.get(item["key"])
        if not isinstance(jars, dict) or canonical_hash(jars) != item["runtime_manifest"]["sha256"]:
            raise SystemExit(f"frozen runtime JAR identity differs from preparation for {item['key']}")

    fingerprints = fixture_maps(current_stages)
    fixture_gate = fixture_comparison(fingerprints)
    gc_gate = static_gc_gate(current_stages)
    if args.phase == "preflight":
        OUTPUT_ROOT.mkdir(parents=True)
        plan_stage_metadata = current_stages
        JFR_INIT_SCRIPT, jfr_settings = create_jfr_init_script(OUTPUT_ROOT)
        plan = {
            "experiment": "deadline full-suite protocol/cache matrix",
            "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
            "host_lock_approval": None,
            "host_lock_status": "not-granted-during-preflight",
            "repeat_count": args.repeat_count,
            "lara_js_bridge_overlay": bridge_overlay,
            "warmup_count": 1,
            "suites": list(SUITES),
            "expected_tests": {
                suite: {
                    "total": counts["total_tests"],
                    "passed": counts["passed_tests"],
                    "failed": counts["failed_tests"],
                    "skipped": counts["skipped_tests"],
                }
                for suite, counts in EXPECTED.items()
            },
            "cells": [
                {"suite": suite, "mode": mode, "stage": stage["key"], "warmup_count": 1, "repeat_count": args.repeat_count}
                for suite in SUITES for mode in MODES for stage in eligible_stages(mode)
            ],
            "cache_states": {
                "direct": [stage["key"] for stage in eligible_stages("direct")],
                "cold": [stage["key"] for stage in eligible_stages("cold")],
                "warm": [stage["key"] for stage in eligible_stages("warm")],
            },
            "timing_boundary": {
                "headline": "GNU time elapsed_s for one full test command (npm exec/vitest for Clava-JS; Gradle test for Java), including launcher/Gradle overhead but excluding runtime build and test-class compilation",
                "java_test_only": "sum of JUnit XML testcase durations (junit_aggregate_s); gradle_non_test_elapsed_s is elapsed_s minus this aggregate, clamped at zero",
            },
            "stages": {item["key"]: item for item in current_stages},
            "fixture_fingerprints": fingerprints,
            "fixture_identity_gate": fixture_gate,
            "show_exec_info_gc_gate": gc_gate,
            "gc_request_jfr": {
                "event": "jdk.SystemGC",
                "settings": str(jfr_settings),
                "settings_sha256": sha256_file(jfr_settings),
                "one_unmeasured_java_full_suite_per_stage": True,
                "jfr_not_enabled_for_measured_rows": True,
            },
            "compression_policy": {
                stage["key"]: {mode: compression_record(stage, mode) for mode in MODES if mode == "direct" or stage["cache"]}
                for stage in comparison.STAGES
            },
            "preparation_manifest_sha256": sha256_file(PREPARATION_MANIFEST),
            "runtime_preparation": preparation,
            "preflight_status": "running",
        }
        identity: dict[str, Any] = {
            "passed": fixture_gate["passed"] and gc_gate["passed"],
            "fixture_gate_passed": fixture_gate["passed"],
            "show_exec_info_gc_gate_passed": gc_gate["passed"],
            "mismatches": list(fixture_gate["mismatches"]),
            "warmup_test_identities": {},
            "reference_test_ids": {},
            "gc_requests_by_stage": {},
        }
        results: list[dict[str, Any]] = []
        checkpoint(results, plan, identity)
        if not fixture_gate["passed"] or not gc_gate["passed"]:
            print("Fixture or heap-logging static GC gate failed; no suite command was launched.", file=sys.stderr)
            plan["preflight_status"] = "failed-before-warmups"
            checkpoint(results, plan, identity)
            return 2

        for suite, mode, stage in suite_interleaved_schedule(0):
            jfr_path = None
            if suite == "java" and mode == "direct":
                jfr_path = OUTPUT_ROOT / "jfr" / f"systemgc-{stage['key']}.jfr"
            result = run_one(suite, mode, stage, False, None, results, plan, identity, fingerprints, jfr_path)
            if jfr_path is not None:
                identity["gc_requests_by_stage"][stage["key"]] = {
                    "cell_id": result["cell_id"],
                    "request_count": result.get("system_gc_request_count"),
                    "verified": result.get("system_gc_jfr_verified", False),
                    "recording_sha256": sha256_file(jfr_path) if jfr_path.is_file() else None,
                }
        set_reference_identities(identity, results)
        identity["passed"] = identity["passed"] and len(results) == 20 and all(
            result["valid"] for result in results if not result["measured"]
        ) and all(
            evidence.get("verified") and evidence.get("request_count") == 0
            for evidence in identity["gc_requests_by_stage"].values()
        ) and len(identity["gc_requests_by_stage"]) == 4
        plan["preflight_status"] = "passed" if identity["passed"] else "failed"
        plan["preflight_completed_at"] = dt.datetime.now(dt.timezone.utc).isoformat()
        checkpoint(results, plan, identity)
        comparison.assert_heads_unchanged(current_stages)
        if not identity["passed"]:
            print("Warm-up count/cache/test-identity/native hash/JFR gate failed; measured commands remain blocked.", file=sys.stderr)
            return 2
        print(f"Untimed preflight passed. Await parent host lock before phase=measure. Output: {OUTPUT_ROOT}")
        return 0

    # Measurement phase: reload the exact successful preflight and reject any drift.
    manifest_path = OUTPUT_ROOT / "results.json"
    saved = json.loads(manifest_path.read_text())
    plan = saved.get("plan", {})
    identity = saved.get("identity_preflight", {})
    results = saved.get("results", [])
    if plan.get("preflight_status") != "passed" or identity.get("passed") is not True:
        raise SystemExit("measurement blocked because the saved untimed preflight did not pass")
    verify_lara_bridge_overlay(plan)
    if plan.get("preparation_manifest_sha256") != sha256_file(PREPARATION_MANIFEST):
        raise SystemExit("frozen-runtime preparation changed after preflight")
    if canonical_hash(plan.get("stages")) != canonical_hash({item["key"]: item for item in current_stages}):
        raise SystemExit("stage/runtime/dependency provenance changed after preflight")
    if canonical_hash(plan.get("fixture_fingerprints")) != canonical_hash(fingerprints):
        raise SystemExit("fixture/source fingerprints changed after preflight")
    if canonical_hash(plan.get("fixture_identity_gate")) != canonical_hash(fixture_gate):
        raise SystemExit("fixture identity gate changed after preflight")
    if canonical_hash(plan.get("show_exec_info_gc_gate")) != canonical_hash(gc_gate):
        raise SystemExit("SHOW_EXEC_INFO gate changed after preflight")
    warmup_rows = [item for item in results if not item.get("measured")]
    warmup_ids = {item.get("cell_id") for item in warmup_rows}
    expected_warmups = {f"{suite}/{mode}/{stage['key']}/warmup" for suite in SUITES for mode in MODES for stage in eligible_stages(mode)}
    if warmup_ids != expected_warmups or len(warmup_rows) != len(expected_warmups) or any(
        item.get("attempt") != 1 or item.get("selected") is not True or item.get("valid") is not True
        for item in warmup_rows
    ):
        raise SystemExit(f"preflight warm-up cell set is incomplete or duplicated: missing={sorted(expected_warmups-warmup_ids)}, unexpected={sorted(warmup_ids-expected_warmups)}")
    plan["host_lock_approval"] = args.lock_note
    plan["host_lock_status"] = "approved-exclusive"
    plan["measurement_started_at"] = dt.datetime.now(dt.timezone.utc).isoformat()
    plan["measurement_schedule"] = "serial paired suites interleaved within each rotating stage/mode round; stage and cache-mode starts rotate by repeat; no concurrent parser subprocesses"
    plan_stage_metadata = current_stages
    checkpoint(results, plan, identity)

    selected_valid_ids = {
        item.get("cell_id") for item in results
        if item.get("measured") and item.get("selected") is True and item.get("valid") is True
    }
    attempted_ids = {item.get("cell_id") for item in results if item.get("measured")}
    for repeat in range(1, args.repeat_count + 1):
        for suite, mode, stage in suite_interleaved_schedule(repeat):
            cell_id = f"{suite}/{mode}/{stage['key']}/repeat-{repeat:02d}"
            existing = [item for item in results if item.get("measured") and item.get("cell_id") == cell_id]
            if cell_id in selected_valid_ids:
                continue
            if existing and not args.repair_invalid:
                continue
            attempt = 1 + max((int(item.get("attempt", 0)) for item in existing), default=0)
            if existing:
                for previous in existing:
                    previous["selected"] = False
                    previous["superseded_by_attempt"] = attempt
            result = run_one(suite, mode, stage, True, repeat, results, plan, identity, fingerprints, attempt=attempt)
            attempted_ids.add(cell_id)
            if result.get("selected") is True:
                selected_valid_ids.add(cell_id)

    comparison.assert_heads_unchanged(current_stages)
    expected_measurements = {
        f"{suite}/{mode}/{stage['key']}/repeat-{repeat:02d}"
        for repeat in range(1, args.repeat_count + 1) for suite in SUITES
        for mode in MODES for stage in eligible_stages(mode)
    }
    plan["missing_measured_cells"] = sorted(expected_measurements - attempted_ids)
    plan["invalid_attempts"] = [
        {"cell_id": item["cell_id"], "attempt": item["attempt"], "reason": item.get("failure_names", [])}
        for item in results if item.get("measured") and not item.get("valid")
    ]
    plan["invalid_measured_cells"] = sorted(expected_measurements - selected_valid_ids)
    plan["all_measurements_valid"] = not plan["invalid_measured_cells"] and identity["passed"] and not plan["missing_measured_cells"]
    plan["measurement_completed_at"] = dt.datetime.now(dt.timezone.utc).isoformat()
    checkpoint(results, plan, identity)
    return 0 if plan["all_measurements_valid"] else 1


OUTPUT_ROOT: Path
plan_stage_metadata: list[dict[str, Any]]
JFR_INIT_SCRIPT: Path


if __name__ == "__main__":
    raise SystemExit(main())
