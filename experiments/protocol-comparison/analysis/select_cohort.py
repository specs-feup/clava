#!/usr/bin/env python3
"""Create a validated Clava-JS shard for a suite-partitioned matrix analysis.

The primary matrix is validated as a complete two-suite input before its
Clava-JS rows are copied. A fresh Java-only manifest is validated separately
and recorded in the provenance audit. This selector never edits either input
or changes timing values, repeat numbers, or selected-attempt flags.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path
from typing import Any

import analyze_deadline as analyzer


SHARED_PLAN_FIELDS = (
    "experiment",
    "repeat_count",
    "warmup_count",
    "expected_tests",
    "fixture_fingerprints",
    "timing_boundary",
    "cache_states",
    "compression_policy",
)


def sha256_bytes(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def canonical_sha256(value: Any) -> str:
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return sha256_bytes(encoded)


def read_source(path: Path) -> tuple[dict[str, Any], str]:
    try:
        content = path.read_bytes()
        value = json.loads(content)
    except (OSError, json.JSONDecodeError) as error:
        raise analyzer.AnalysisError(f"cannot read {path}: {error}") from error
    if not isinstance(value, dict):
        raise analyzer.AnalysisError(f"{path}: manifest must be an object")
    return value, sha256_bytes(content)


def _validate_primary(manifest: dict[str, Any], source: Path):
    plan, planned, baseline, rows, audit = analyzer.validate_manifest(manifest, source)
    if set(plan.get("suites", [])) != set(analyzer.SUITES):
        raise analyzer.AnalysisError(f"{source}: primary input must plan both suites")
    if planned != analyzer.expected_cells():
        raise analyzer.AnalysisError(f"{source}: primary input must cover the complete two-suite matrix")
    _require_complete_attempt_set(rows, planned, plan, source)
    return plan, planned, baseline, rows, audit


def _validate_java(manifest: dict[str, Any], source: Path):
    plan, planned, baseline, rows, audit = analyzer.validate_manifest(manifest, source)
    if plan.get("suites") != ["java"]:
        raise analyzer.AnalysisError(f"{source}: fresh input must be a Java-only suite partition")
    expected_java = {cell for cell in analyzer.expected_cells() if cell[0] == "java"}
    if planned != expected_java:
        raise analyzer.AnalysisError(f"{source}: Java input does not cover every expected Java matrix cell")
    _require_complete_attempt_set(rows, planned, plan, source)
    return plan, planned, baseline, rows, audit


def _require_complete_attempt_set(rows: list[dict[str, Any]], planned: set[tuple[str, str, str]],
                                  plan: dict[str, Any], source: Path) -> None:
    """Require one selected warm-up and all selected measured repeats per cell."""
    expected_repeats = set(range(1, plan["repeat_count"] + 1))
    slots: dict[tuple[str, str, str], set[int]] = {cell: set() for cell in planned}
    for row in rows:
        cell = (row["suite"], row["mode"], row["stage"])
        slot = row["repeat"] if row["measured"] else 0
        if slot in slots[cell]:
            raise analyzer.AnalysisError(f"{source}: duplicate selected result slot for {cell} repeat {slot}")
        slots[cell].add(slot)
    expected_slots = {0, *expected_repeats}
    incomplete = {cell: sorted(expected_slots - cell_slots)
                  for cell, cell_slots in slots.items() if cell_slots != expected_slots}
    if incomplete:
        raise analyzer.AnalysisError(f"{source}: missing selected warm-up/measurement rows: {incomplete}")


def _require_shared_provenance(primary_plan: dict[str, Any], java_plan: dict[str, Any],
                               primary_source: Path, java_source: Path) -> None:
    for field in SHARED_PLAN_FIELDS:
        if primary_plan.get(field) != java_plan.get(field):
            raise analyzer.AnalysisError(
                f"{java_source}: plan.{field} differs from primary source {primary_source}"
            )
    if primary_plan.get("stages") != java_plan.get("stages"):
        raise analyzer.AnalysisError(f"{java_source}: stage build provenance differs from primary source {primary_source}")


def select_js_partition(primary: dict[str, Any], primary_source: Path,
                        java: dict[str, Any], java_source: Path) -> tuple[dict[str, Any], dict[str, Any]]:
    """Validate both inputs and return an unchanged Clava-JS projection plus audit metadata."""
    primary_plan, primary_cells, primary_baseline, _, _ = _validate_primary(primary, primary_source)
    java_plan, java_cells, java_baseline, _, _ = _validate_java(java, java_source)
    _require_shared_provenance(primary_plan, java_plan, primary_source, java_source)

    selected = copy.deepcopy(primary)
    selected_plan = selected["plan"]
    selected_plan["suites"] = ["clava-js"]
    selected_plan["cells"] = [cell for cell in selected_plan["cells"] if cell.get("suite") == "clava-js"]
    selected["identity_preflight"]["reference_test_ids"] = {
        "clava-js": selected["identity_preflight"]["reference_test_ids"]["clava-js"]
    }
    selected["identity_preflight"]["warmup_test_identities"] = {
        "clava-js": selected["identity_preflight"]["warmup_test_identities"]["clava-js"]
    }
    # Keep the complete attempt history, including rejected/superseded rows,
    # so validation and downstream audit retain the original selected flags.
    selected["results"] = [row for row in selected["results"] if row.get("suite") == "clava-js"]

    output_source = Path("<derived-clava-js-partition>")
    _, js_cells, js_baseline, _, _ = analyzer.validate_manifest(selected, output_source)
    if js_cells != {cell for cell in primary_cells if cell[0] == "clava-js"}:
        raise analyzer.AnalysisError("derived Clava-JS plan changed the selected cell set")
    if js_baseline != {"clava-js": primary_baseline["clava-js"]}:
        raise analyzer.AnalysisError("derived Clava-JS identity reference changed")
    if java_baseline.keys() != {"java"} or java_cells & js_cells:
        raise analyzer.AnalysisError("Java and Clava-JS input partitions overlap or have invalid identities")
    if js_cells | java_cells != analyzer.expected_cells():
        raise analyzer.AnalysisError("input partitions do not reconstruct the expected matrix cells")

    metadata = {
        "schema_version": 1,
        "selection": "clava-js from validated complete primary matrix",
        "primary_source": {
            "path": str(primary_source.resolve()),
            "sha256": None,
            "plan_sha256": canonical_sha256(primary_plan),
            "result_rows": len(primary["results"]),
            "suite_result_rows": len(selected["results"]),
            "suite_result_rows_sha256": canonical_sha256(selected["results"]),
            "reference_test_identity_sha256": primary_baseline["clava-js"],
        },
        "java_source": {
            "path": str(java_source.resolve()),
            "sha256": None,
            "suite": "java",
            "planned_cells": len(java_cells),
            "validated_result_rows": len(java["results"]),
            "reference_test_identity_sha256": java_baseline["java"],
            "plan_sha256": canonical_sha256(java_plan),
        },
        "derived_manifest": {
            "suite": "clava-js",
            "planned_cells": len(js_cells),
            "result_rows": len(selected["results"]),
            "plan_sha256": canonical_sha256(selected_plan),
            "suite_result_rows_sha256": canonical_sha256(selected["results"]),
        },
        "timing_handling": "result rows copied verbatim; no timing or repeat arithmetic performed",
        "cross_suite_rounds": "not combined by this selector",
    }
    return selected, metadata


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--primary", type=Path, required=True,
                        help="complete frozen two-suite primary matrix results.json")
    parser.add_argument("--java", type=Path, required=True,
                        help="fresh, complete Java-only results.json manifest")
    parser.add_argument("--output-dir", type=Path, required=True,
                        help="new directory for clava-js.results.json and selection-provenance.json")
    args = parser.parse_args(argv)

    primary_path = args.primary.resolve()
    java_path = args.java.resolve()
    output_dir = args.output_dir.resolve()
    output_manifest = output_dir / "clava-js.results.json"
    output_provenance = output_dir / "selection-provenance.json"
    if primary_path == java_path:
        raise analyzer.AnalysisError("primary and Java inputs must be distinct manifests")
    if output_manifest in (primary_path, java_path) or output_provenance in (primary_path, java_path):
        raise analyzer.AnalysisError("output paths must not overwrite source manifests")
    if output_dir.exists() and (output_manifest.exists() or output_provenance.exists()):
        raise analyzer.AnalysisError(f"refusing to overwrite existing selector outputs in {output_dir}")

    primary, primary_sha = read_source(primary_path)
    java, java_sha = read_source(java_path)
    selected, provenance = select_js_partition(primary, primary_path, java, java_path)
    provenance["primary_source"]["sha256"] = primary_sha
    provenance["java_source"]["sha256"] = java_sha

    manifest_bytes = (json.dumps(selected, indent=2, sort_keys=True) + "\n").encode("utf-8")
    provenance["derived_manifest"]["path"] = str(output_manifest)
    provenance["derived_manifest"]["sha256"] = sha256_bytes(manifest_bytes)
    provenance_bytes = (json.dumps(provenance, indent=2, sort_keys=True) + "\n").encode("utf-8")
    output_dir.mkdir(parents=True, exist_ok=True)
    output_manifest.write_bytes(manifest_bytes)
    output_provenance.write_bytes(provenance_bytes)
    print(output_manifest)
    print(output_provenance)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
