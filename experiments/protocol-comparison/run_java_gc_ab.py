#!/usr/bin/env python3
"""Measure Text/Protobuf Java-suite A/B with explicit GC on and off."""

from __future__ import annotations

import argparse
import datetime as dt
import json
from pathlib import Path
import statistics
from typing import Any

import run_dual_ab as ab


POLICIES = ("normal", "disabled")
STAGE_KEYS = tuple(stage["key"] for stage in ab.STAGES)


def measured_order(repeat: int) -> tuple[tuple[str, dict[str, Any]], ...]:
    """Balance which policy and wire format runs first over six repeats."""
    policies = POLICIES if repeat % 2 else POLICIES[::-1]
    stages = ab.STAGES if repeat % 4 in (0, 1) else ab.STAGES[::-1]
    return tuple((policy, stage) for policy in policies for stage in stages)


def summarize(rows: list[dict[str, Any]], repeat_count: int) -> dict[str, Any]:
    measured = [row for row in rows if row.get("measured") is True]
    conditions: dict[str, Any] = {}
    for policy in POLICIES:
        pairs = []
        for repeat in range(1, repeat_count + 1):
            group = [row for row in measured if row.get("gc_policy") == policy and row.get("repeat") == repeat]
            by_stage = {row["stage"]: row for row in group}
            if len(group) != 2 or set(by_stage) != set(STAGE_KEYS):
                raise ValueError(f"missing or duplicate {policy} pair {repeat}")
            if not all(row.get("valid") is True and row.get("worker_gc_policy_verified") is True for row in group):
                raise ValueError(f"invalid {policy} pair {repeat}")
            if len({row.get("metric_event_count") for row in group}) != 1:
                raise ValueError(f"mismatched AST metric counts in {policy} pair {repeat}")
            text_time = float(by_stage["ab-text"]["elapsed_s"])
            proto_time = float(by_stage["ab-protobuf"]["elapsed_s"])
            pairs.append({"repeat": repeat, "text_s": text_time, "protobuf_s": proto_time,
                          "gap_s": proto_time - text_time,
                          "protobuf_change_percent": (proto_time / text_time - 1) * 100})
        conditions[policy] = {
            "pairs": pairs,
            "text_median_s": statistics.median(pair["text_s"] for pair in pairs),
            "protobuf_median_s": statistics.median(pair["protobuf_s"] for pair in pairs),
            "median_paired_gap_s": statistics.median(pair["gap_s"] for pair in pairs),
            "median_paired_change_percent": statistics.median(pair["protobuf_change_percent"] for pair in pairs),
        }
    return {
        "conditions": conditions,
        "paired_gap_removed_s": (conditions["normal"]["median_paired_gap_s"]
                                 - conditions["disabled"]["median_paired_gap_s"]),
        "same_revision": True,
        "same_116_test_suite": True,
        "same_cache_bypass": True,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preflight-result", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--repeat-count", type=int, default=6)
    parser.add_argument("--clava-root", type=Path, default=ab.CLAVA_ROOT)
    parser.add_argument("--native-tool", type=Path, default=ab.DEFAULT_NATIVE_TOOL)
    parser.add_argument("--js-workspace", type=Path, default=ab.DEFAULT_JS_WORKSPACE)
    args = parser.parse_args()
    if args.repeat_count < 2:
        parser.error("--repeat-count must be at least two")
    root = args.output_root.resolve()
    if root.exists():
        parser.error(f"refusing to reuse output root: {root}")
    clava_root = args.clava_root.resolve()
    native_tool = args.native_tool.resolve()
    js_workspace = args.js_workspace.resolve()
    dependencies = ab.configure_gradle_dependency_roots(js_workspace)
    ab.validate_inputs(clava_root, native_tool, js_workspace, dependencies, require_native=True)
    sources = ab.source_metadata(clava_root, native_tool, js_workspace)
    runtime, gate = ab.validate_preflight(args.preflight_result.resolve(), sources, native_tool)
    root.mkdir(parents=True)
    plan = {
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "experiment": "same-revision Java Text/Protobuf by explicit-GC policy",
        "suite": "java", "test_count": 116, "repeat_count": args.repeat_count,
        "preflight_root": str(args.preflight_result.resolve()),
        "fidelity_gate": gate, "sources": sources,
        "runtime_parser_jar_sha256": ab.base.sha256_file(runtime / "lib" / "ClangAstParser.jar"),
        "gc_policies": {
            "normal": "test worker receives -XX:-DisableExplicitGC",
            "disabled": "test worker receives -XX:+DisableExplicitGC",
        },
        "cache_mode": "direct; CCACHE_DISABLE=true and zero ccache invocations checked per run",
        "order": {str(repeat): [(policy, stage["key"]) for policy, stage in measured_order(repeat)]
                  for repeat in range(1, args.repeat_count + 1)},
    }
    ab.save_json(root / "plan.json", plan)
    rows: list[dict[str, Any]] = []
    ordinal = 0
    for policy in POLICIES:
        for stage in ab.STAGES:
            ordinal += 1
            row = ab.run_timing(stage, "java", clava_root, native_tool, js_workspace,
                                runtime, root, ordinal, False, None, gc_policy=policy)
            rows.append(row)
            ab.save_json(root / "results.json", {**plan, "results": rows, "complete": False})
            print(f"warmup {policy} {stage['wire']}: {row['passed_tests']}/116, valid={row['valid']}", flush=True)
            if not row["valid"]:
                return 1
    for repeat in range(1, args.repeat_count + 1):
        for policy, stage in measured_order(repeat):
            ordinal += 1
            row = ab.run_timing(stage, "java", clava_root, native_tool, js_workspace,
                                runtime, root, ordinal, True, repeat, gc_policy=policy)
            rows.append(row)
            ab.save_json(root / "results.json", {**plan, "results": rows, "complete": False})
            print(f"repeat {repeat} {policy} {stage['wire']}: {row['elapsed_s']:.2f}s, "
                  f"{row['passed_tests']}/116, valid={row['valid']}", flush=True)
            if not row["valid"]:
                return 1
        try:
            partial = summarize(rows, repeat)
        except ValueError as error:
            print(f"pair validation failed: {error}", flush=True)
            return 1
        ab.save_json(root / "results.json", {**plan, "results": rows, "summary": partial,
                                             "complete": False})
    if not ab.source_metadata_matches(ab.source_metadata(clava_root, native_tool, js_workspace), sources):
        raise SystemExit("source or native tool identity changed during measurement")
    summary = summarize(rows, args.repeat_count)
    ab.save_json(root / "results.json", {**plan, "results": rows, "summary": summary,
                                         "complete": True, "valid": True})
    print(json.dumps(summary, indent=2), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
