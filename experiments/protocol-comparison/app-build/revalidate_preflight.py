#!/usr/bin/env python3
"""Re-enrich an existing App-build preflight without running either suite."""

from __future__ import annotations

import collections
import hashlib
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
sys.path.insert(0, str(Path(__file__).resolve().parent))
import run_app_build_matrix as matrix


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def java_test_identities(result_dir: Path) -> list[dict[str, str]]:
    identities: list[dict[str, str]] = []
    for xml_path in sorted((result_dir / "junit-results").glob("*.xml")):
        root = ET.parse(xml_path).getroot()
        for test in root.findall("testcase"):
            if test.find("failure") is not None or test.find("error") is not None:
                status = "failed"
            elif test.find("skipped") is not None:
                status = "skipped"
            else:
                status = "passed"
            identities.append({
                "class": str(test.attrib.get("classname", "")),
                "test": str(test.attrib.get("name", "")),
                "status": status,
            })
    return sorted(identities, key=lambda item: (item["class"], item["test"], item["status"]))


def suite_identity(result: dict[str, object]) -> tuple[dict[str, object], list[dict[str, str]]]:
    run_dir = Path(str(result["run_dir"]))
    suite = str(result["suite"])
    if suite == "clava-js":
        report = json.loads((run_dir / "vitest.json").read_text())
        return matrix.comparison.js_counts(report), matrix.js_test_identities(report)
    test_root = run_dir / "junit-results"
    return matrix.comparison.java_counts(test_root), java_test_identities(run_dir)


def main() -> int:
    if len(sys.argv) != 3:
        raise SystemExit("usage: revalidate_preflight.py SOURCE_PLAN.json OUTPUT_DIR")
    source_plan_path = Path(sys.argv[1]).resolve()
    output_root = Path(sys.argv[2]).resolve()
    if output_root.exists():
        raise SystemExit(f"refusing to overwrite existing output: {output_root}")
    source_plan = json.loads(source_plan_path.read_text())
    source_hash = sha256_file(source_plan_path)
    if len(source_plan.get("results", [])) != len(matrix.STAGE_KEYS) * len(matrix.SUITES):
        raise SystemExit("source plan does not contain the complete eight-cell preflight")
    frozen_plan = matrix.read_plan(matrix.DEFAULT_FROZEN_ROOT)
    stages = matrix.stage_records(frozen_plan)
    preparation_path = matrix.SCRIPT_ROOT / "results/matched-fast-20261002-r1/preparation.json"
    preparation = json.loads(preparation_path.read_text())
    frozen_runtime_records = {
        record["stage"]: record["runtime_manifest_jars"]
        for record in preparation["records"]
        if record.get("task") == "frozen_runtime_identity"
    }
    if set(frozen_runtime_records) != set(matrix.STAGE_KEYS):
        raise RuntimeError("matched-stage preparation lacks full runtime JAR identity records")
    frozen_stages = frozen_plan["plan"]["stages"]
    runner_init = matrix.SCRIPT_ROOT / "java-suite.init.gradle"
    runner_init_sha256 = sha256_file(runner_init)
    clava_test_path = stages["before-cache"]["clava"] / "ClavaWeaver/resources/clava/test/api/ClavaTest.js"
    clava_test_source = clava_test_path.read_text()
    no_include_source_literal = "int foo() {return 0;}"
    if f'"{no_include_source_literal}"' not in clava_test_source:
        raise RuntimeError(f"no-include source literal changed: {clava_test_path}")
    if hashlib.sha256(no_include_source_literal.encode()).hexdigest() != matrix.NO_INCLUDE_ADDED_FILE_SHA256:
        raise RuntimeError("the fixture literal does not match the captured addedFile.cpp source SHA-256")
    for key in matrix.STAGE_KEYS:
        recorded = source_plan["stages"][key]
        overlay_path = Path(recorded["overlay"]["overlay_jar"]).resolve()
        overlay_provenance_path = Path(recorded["overlay"]["provenance"]).resolve()
        overlay_provenance = json.loads(overlay_provenance_path.read_text())
        frozen_stage = frozen_stages[key]
        runtime_jars = frozen_runtime_records[key]
        current_runtime = matrix.comparison.runtime_manifest(stages[key]["runtime"])
        if current_runtime != frozen_stage["runtime_manifest"]:
            raise RuntimeError(f"runtime JAR manifest changed for {key}: {current_runtime}")
        if matrix.digest(runtime_jars) != current_runtime["sha256"]:
            raise RuntimeError(f"per-JAR runtime inventory does not match the frozen digest for {key}")
        if overlay_provenance.get("runtime_jars_unchanged") is not True:
            raise RuntimeError(f"overlay build reports runtime JAR mutation for {key}")
        if overlay_provenance.get("runtime_jars_before") != overlay_provenance.get("runtime_jars_after"):
            raise RuntimeError(f"overlay runtime-JAR hash sets differ for {key}")
        stages[key]["overlay"] = {
            "overlay_jar": overlay_path,
            "overlay_sha256": sha256_file(overlay_path),
        }
    output_root.mkdir(parents=True)

    by_suite: dict[str, list[dict[str, object]]] = {suite: [] for suite in matrix.SUITES}
    baseline_identity: dict[str, list[dict[str, str]]] = {}
    runtime_hashes_before = {
        key: {
            "parser_jar_sha256": sha256_file(stages[key]["runtime_lib"] / "ClangAstParser.jar"),
            "native_tool_sha256": sha256_file(stages[key]["dumper"]),
        }
        for key in matrix.STAGE_KEYS
    }
    raw_hashes: dict[str, str] = {}

    for original in source_plan["results"]:
        result = dict(original)
        suite, key = str(result["suite"]), str(result["stage"])
        run_dir = Path(str(result["run_dir"]))
        raw_metrics = run_dir / "app-calls.jsonl"
        raw_hashes[f"{suite}:{key}"] = sha256_file(raw_metrics)
        temp_root = source_plan_path.parent / "temp" / suite / key
        rows = matrix.enrich_calls(raw_metrics, suite, key, str(result["mode"]), int(result["repeat"]),
                                   stages[key], temp_root)
        matrix.validate_capture_rows(rows, stages[key]["overlay"]["overlay_jar"], suite, key)
        for row in rows:
            row["source_count"] = len(row.get("resolved_sources", []))
        app_rows = [row for row in rows if row.get("elapsed_ms") is not None
                    and not row.get("syntax_only") and row.get("excluded_reason") != "syntax_only"]
        syntax_rows = [row for row in rows if row.get("syntax_only") or row.get("excluded_reason") == "syntax_only"]
        if not rows or not app_rows:
            raise RuntimeError(f"capture lacks parser/App rows: {suite}/{key}")

        # Every stage must contain exactly one captured row for each narrowly
        # approved JS generated-root equivalence and the source-only case.
        if suite == "clava-js":
            exact_rows = {
                "file-rebuild-current-code": [row for row in app_rows if [s.get("sha256") for s in row.get("resolved_sources", [])] == [matrix.FILE_REBUILD_HEADER_SHA256]],
                "source-without-includes": [row for row in app_rows if [s.get("sha256") for s in row.get("resolved_sources", [])] == [matrix.NO_INCLUDE_ADDED_FILE_SHA256]],
            }
            for label, selected in exact_rows.items():
                if len(selected) != 1:
                    raise RuntimeError(f"expected exactly one {label} case in {key}, found {len(selected)}")
            if len(rows) != 300 or len(app_rows) != 170 or len(syntax_rows) != 130:
                raise RuntimeError(f"unexpected fixed Clava-JS capture scope in {key}: total={len(rows)}, apps={len(app_rows)}, syntax={len(syntax_rows)}")
        elif len(rows) != 216 or len(app_rows) != 216 or syntax_rows:
            raise RuntimeError(f"unexpected fixed Java capture scope in {key}: total={len(rows)}, apps={len(app_rows)}, syntax={len(syntax_rows)}")

        counts, identities = suite_identity(result)
        expected_counts = matrix.EXPECTED_TESTS[suite]
        if {name: counts.get(name) for name in expected_counts} != expected_counts:
            raise RuntimeError(f"test-count contract changed in {suite}/{key}: {counts}")
        if suite not in baseline_identity:
            baseline_identity[suite] = identities
        elif identities != baseline_identity[suite]:
            raise RuntimeError(f"full test identity/status set changed in {suite}/{key}")
        result["test_counts"] = counts
        result["test_identity_sha256"] = matrix.digest(identities)
        result["test_identity_count"] = len(identities)
        result["test_identity_matches_suite_baseline"] = True
        if int(result.get("return_code", 1)) != 0:
            raise RuntimeError(f"retained suite returned nonzero for {suite}/{key}: {result.get('return_code')}")
        if suite == "clava-js":
            report_path = run_dir / "vitest.json"
            report_hashes = {report_path.name: sha256_file(report_path)}
            runner_paths = [run_dir / "vitest.app-build.config.ts", run_dir / "appBuildWeaverEnvironment.ts"]
        else:
            report_hashes = {
                path.name: sha256_file(path)
                for path in sorted((run_dir / "junit-results").glob("*.xml"))
            }
            runner_paths = [run_dir / "app-build-overlay.init.gradle", runner_init]
        runner_paths += [run_dir / "command.json"]
        result["test_report_sha256_by_file"] = report_hashes
        result["runner_artifact_sha256_by_path"] = {
            str(path.resolve()): sha256_file(path) for path in runner_paths if path.is_file()
        }
        result["call_count"] = len(rows)
        result["app_calls"] = len(app_rows)
        result["syntax_only_calls"] = len(syntax_rows)
        result["app_returned_null"] = sum(bool(row.get("app_returned_null")) for row in app_rows)
        result["app_elapsed_ms"] = sum(float(row["elapsed_ms"]) for row in app_rows)
        result["source_count"] = sum(int(row.get("source_count", 0)) for row in app_rows)
        result["context_pattern"] = matrix.context_pattern(rows)
        result["app_group_count"] = len(matrix.workload_counter(rows))
        enriched_path = output_root / "captures" / suite / key / "app-calls.enriched.jsonl"
        enriched_path.parent.mkdir(parents=True, exist_ok=True)
        enriched_path.write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in rows))
        result["raw_metrics"] = str(raw_metrics.resolve())
        result["raw_metrics_sha256"] = raw_hashes[f"{suite}:{key}"]
        result["metrics"] = str(enriched_path.resolve())
        result["app_only_reenriched"] = True
        result["valid"] = True
        by_suite[suite].append(result)

    results = [result for suite in matrix.SUITES for result in by_suite[suite]]
    workload_refs: dict[str, collections.Counter[str]] = {}
    context_refs: dict[str, list[tuple[str, str]]] = {}
    matrix.compare_workloads(results, workload_refs, context_refs)
    for key in matrix.STAGE_KEYS:
        if runtime_hashes_before[key]["parser_jar_sha256"] != stages[key]["parser_jar_sha256"]:
            raise RuntimeError(f"runtime parser JAR changed while revalidating {key}")
        if runtime_hashes_before[key]["native_tool_sha256"] != sha256_file(stages[key]["dumper"]):
            raise RuntimeError(f"native tool changed while revalidating {key}")

    manifest = dict(source_plan)
    manifest_stages = {}
    for key in matrix.STAGE_KEYS:
        frozen_stage = frozen_stages[key]
        runtime_jars = frozen_runtime_records[key]
        overlay_provenance_path = Path(source_plan["stages"][key]["overlay"]["provenance"])
        overlay_provenance = json.loads(overlay_provenance_path.read_text())
        manifest_stages[key] = {
            **source_plan["stages"][key],
            "runtime_manifest": frozen_stage["runtime_manifest"],
            "runtime_manifest_sha256": frozen_stage["runtime_manifest_sha256"],
            "runtime_manifest_jars": runtime_jars,
            "runtime_manifest_jars_sha256": matrix.digest(runtime_jars),
            "clava_patch_sha256": frozen_stage.get("clava_patch_sha256"),
            "native_source_diff_sha256": frozen_stage.get("native_source_diff_sha256"),
            "native_build": frozen_stage.get("native_build"),
            "native_build_provenance_sha256": frozen_stage.get("native_build_provenance_sha256"),
            "native_schema_sha256": frozen_stage.get("native_schema_sha256"),
            "native_build_type": frozen_stage.get("native_build_type"),
            "native_optimization_flags": frozen_stage.get("native_optimization_flags"),
            "llvm_version": frozen_stage.get("llvm_version"),
            "fast_syntax": frozen_stage.get("fast_syntax"),
            "fast_syntax_policy": frozen_stage.get("fast_syntax_policy"),
            "java_resource_tag_sha256": frozen_stage.get("java_resource_tag_sha256"),
            "producer_schema_commit": (frozen_stage.get("native_build") or {}).get("producer_schema_commit"),
            "overlay_build": {
                "parser_source_sha256": overlay_provenance.get("parser_source_sha256"),
                "transformed_parser_sha256": overlay_provenance.get("transformed_parser_sha256"),
                "helper_source_sha256": overlay_provenance.get("helper_source_sha256"),
                "javac_target_release": overlay_provenance.get("javac_target_release"),
                "overlay_class_major_versions": overlay_provenance.get("overlay_class_major_versions"),
                "runtime_jars_unchanged": overlay_provenance.get("runtime_jars_unchanged"),
            },
        }
    manifest.update({
        "task": "Clava App construction fixed-suite preflight, offline re-enrichment",
        "phase": "preflight",
        "source_preflight_manifest": str(source_plan_path),
        "source_preflight_manifest_sha256": source_hash,
        "source_preflight_valid_before_reenrichment": source_plan.get("valid"),
        "preparation_manifest": str(preparation_path.resolve()),
        "preparation_manifest_sha256": sha256_file(preparation_path),
        "java_suite_init_script": str(runner_init.resolve()),
        "java_suite_init_script_sha256": runner_init_sha256,
        "normalization_version": "app-only-generated-include-roots-r1",
        "normalization_policy": {
            "timed_workload": "compare only non-syntax rows with non-null elapsed_ms; preserve all 130 JS syntax-only rows per cell in raw/enriched captures and verify their 130 count",
            "input_sources": "sorted membership only; duplicates retained because frozen ParallelCodeParser sorts user files and input folders before parsing",
            "compiler_options": "preserve option sequence; no global -I sorting or removal",
            "file_rebuild_generated_root": {
                "source_sha256": matrix.FILE_REBUILD_HEADER_SHA256,
                "role": "$JS_REBUILD_CURRENT_CODE",
                "evidence": "focused include-audit run records identical first-I directory inventory (2 files: file_rebuild.cpp 3622299d… and file_rebuild_2.h 388be761…; inventory f16b0b4f229603a8) for static Clava-JS/__clava_woven_for_file_rebuild and generated current-code roots across all four stages",
                "audit_manifest": str((source_plan_path.parent.parent / "include-audit-run-java17-r3" / "include-audit.json").resolve()),
                "audit_manifest_sha256": sha256_file(source_plan_path.parent.parent / "include-audit-run-java17-r3" / "include-audit.json"),
            },
            "source_without_include_root": {
                "source_sha256": matrix.NO_INCLUDE_ADDED_FILE_SHA256,
                "role": "$JS_REBUILD_NO_INCLUDE_I_ROOT",
                "evidence": "ClavaTest.js constructs exactly int foo() {return 0;} for this generated source; it has no include directives, so the first generated -I path does not affect this translation unit; other options/order are retained",
                "verified_source": "ClavaWeaver/resources/clava/test/api/ClavaTest.js",
                "verified_source_sha256": sha256_file(clava_test_path),
            },
        },
        "source_raw_metrics_sha256": raw_hashes,
        "stages": manifest_stages,
        "test_identity_sha256_by_suite": {suite: matrix.digest(rows) for suite, rows in baseline_identity.items()},
        "test_identity_count_by_suite": {suite: len(rows) for suite, rows in baseline_identity.items()},
        "workload_counters": {suite: dict(workload_refs[suite]) for suite in matrix.SUITES},
        "context_patterns": {suite: context_refs[suite] for suite in matrix.SUITES},
        "app_call_count_by_suite": {suite: sum(int(r["app_calls"]) for r in by_suite[suite]) for suite in matrix.SUITES},
        "syntax_only_call_count_by_suite": {suite: sum(int(r["syntax_only_calls"]) for r in by_suite[suite]) for suite in matrix.SUITES},
        "app_call_count": sum(int(r["app_calls"]) for r in results),
        "completed": len(results),
        "completed_rounds": 0,
        "preflight_complete": True,
        "valid": True,
        "results": results,
        "offline_reenrichment_only": True,
        "suite_invocations_during_reenrichment": 0,
    })
    manifest_path = output_root / "app-build-preflight.json"
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
    (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(json.dumps({
        "valid": True,
        "manifest": str(manifest_path),
        "manifest_sha256": sha256_file(manifest_path),
        "cells": len(results),
        "app_calls": manifest["app_call_count"],
        "app_calls_by_suite": manifest["app_call_count_by_suite"],
        "syntax_only_by_suite": manifest["syntax_only_call_count_by_suite"],
        "workload_fingerprints": {suite: len(workload_refs[suite]) for suite in matrix.SUITES},
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
