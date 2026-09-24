#!/usr/bin/env python3
"""Compare normalized Clava AST graph snapshots emitted by the scratch A/B test."""

from __future__ import annotations

import argparse
from collections import Counter
import json
from pathlib import Path
import sys
from typing import Any


CASES = ("c", "cxx")
MODES = ("text", "protobuf")
MAX_DIFFERENCES = 40


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--snapshots-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    return parser.parse_args()


def load_snapshot(root: Path, mode: str, case: str) -> dict[str, Any]:
    path = root / mode / f"{case}.json"
    with path.open(encoding="utf-8") as source:
        snapshot = json.load(source)
    if snapshot.get("schema") != "clava-ast-graph-v1":
        raise ValueError(f"Unsupported graph snapshot schema in {path}")
    if not isinstance(snapshot.get("graph"), dict):
        raise ValueError(f"Missing graph object in {path}")
    return snapshot


def graph_stats(graph: dict[str, Any]) -> dict[str, Any]:
    nodes = graph.get("nodes", [])
    kinds = Counter(node.get("class", "?") for node in nodes)
    child_edges = sum(len(node.get("children", [])) for node in nodes)
    refs = sum(count_refs(node.get("fields", {})) for node in nodes)
    field_count = sum(len(node.get("fields", {})) for node in nodes)
    return {
        "nodes": len(nodes),
        "child_edges": child_edges,
        "data_reference_edges": refs,
        "populated_data_fields": field_count,
        "node_kinds": dict(sorted(kinds.items())),
    }


def count_refs(value: Any) -> int:
    if isinstance(value, dict):
        here = 1 if set(value) == {"$ref"} else 0
        return here + sum(count_refs(child) for child in value.values())
    if isinstance(value, list):
        return sum(count_refs(child) for child in value)
    return 0


def compare_values(left: Any, right: Any, path: str, differences: list[dict[str, Any]]) -> None:
    if len(differences) >= MAX_DIFFERENCES:
        return
    if type(left) is not type(right):
        differences.append({"path": path, "text": summarize(left), "protobuf": summarize(right)})
        return
    if isinstance(left, dict):
        keys = sorted(set(left) | set(right))
        for key in keys:
            if key not in left:
                differences.append({"path": f"{path}/{escape_pointer(key)}", "text": "<missing>",
                                    "protobuf": summarize(right[key])})
            elif key not in right:
                differences.append({"path": f"{path}/{escape_pointer(key)}", "text": summarize(left[key]),
                                    "protobuf": "<missing>"})
            else:
                compare_values(left[key], right[key], f"{path}/{escape_pointer(key)}", differences)
            if len(differences) >= MAX_DIFFERENCES:
                break
        return
    if isinstance(left, list):
        if len(left) != len(right):
            differences.append({"path": f"{path}/length", "text": len(left), "protobuf": len(right)})
        for index, (left_value, right_value) in enumerate(zip(left, right)):
            compare_values(left_value, right_value, f"{path}/{index}", differences)
            if len(differences) >= MAX_DIFFERENCES:
                break
        return
    if left != right:
        differences.append({"path": path, "text": summarize(left), "protobuf": summarize(right)})


def summarize(value: Any) -> Any:
    rendered = json.dumps(value, ensure_ascii=False, sort_keys=True)
    return rendered if len(rendered) <= 320 else rendered[:317] + "..."


def escape_pointer(value: str) -> str:
    return value.replace("~", "~0").replace("/", "~1")


def main() -> int:
    args = parse_args()
    output: dict[str, Any] = {
        "gate": "same-revision-text-protobuf-ast-fidelity",
        "passed": False,
        "comparison": "exact normalized Clava node graph equality",
        "cases": {},
        "excluded_fields": [],
    }
    try:
        all_passed = True
        for case in CASES:
            text = load_snapshot(args.snapshots_dir, "text", case)
            protobuf = load_snapshot(args.snapshots_dir, "protobuf", case)
            differences: list[dict[str, Any]] = []
            compare_values(text["graph"], protobuf["graph"], "", differences)
            passed = not differences
            all_passed &= passed
            output["cases"][case] = {
                "passed": passed,
                "text": graph_stats(text["graph"]),
                "protobuf": graph_stats(protobuf["graph"]),
                "differences": differences,
                "difference_limit": MAX_DIFFERENCES,
                "truncated": len(differences) >= MAX_DIFFERENCES,
            }
            if not output["excluded_fields"]:
                output["excluded_fields"] = text.get("excluded_fields", [])
        output["passed"] = all_passed
    except (OSError, ValueError, json.JSONDecodeError) as error:
        output["error"] = str(error)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(output, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(output, indent=2, sort_keys=True))
    return 0 if output["passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
