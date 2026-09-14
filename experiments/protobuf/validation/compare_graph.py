#!/usr/bin/env python3
"""Compare two production graph inspection JSON files.

The input contract is intentionally small and independent of generated
protobuf class names:

    {"nodes": [{"wire_id": "...", "class": "...", "fields": {},
                "children": ["..."], "references": {"decl": "..."}}],
     "auxiliary": [...]}

Only ``wire_id`` and values below ``children``/``references`` are normalized.
Fields, source data, literals, paths, names and auxiliary values are compared
byte-for-byte after canonical JSON serialization.
"""

from __future__ import annotations

import argparse
import copy
import json
from pathlib import Path
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--protobuf", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    return parser.parse_args()


def load(path: Path) -> Any:
    try:
        return json.loads(path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise SystemExit(f"could not read graph JSON {path}: {error}") from error


def pointer(value: Any, ids: dict[str, str], *, path: str) -> Any:
    if value is None:
        return value
    if isinstance(value, bool):
        raise ValueError(f"invalid boolean reference {value!r} at {path}")
    key = str(value)
    if key not in ids:
        raise ValueError(f"invalid reference {value!r} at {path}")
    return ids[key]


def normalize(payload: Any) -> Any:
    if not isinstance(payload, dict) or not isinstance(payload.get("nodes"), list):
        raise ValueError("graph JSON must contain a nodes array")
    ids: dict[str, str] = {}
    for index, node in enumerate(payload["nodes"], start=1):
        if not isinstance(node, dict) or "wire_id" not in node:
            raise ValueError(f"node {index} is missing mandatory wire_id")
        original = str(node["wire_id"])
        if original in ids:
            raise ValueError(f"duplicate wire_id {original!r}")
        ids[original] = f"@{index}"

    result = copy.deepcopy(payload)
    for index, node in enumerate(result["nodes"], start=1):
        node["wire_id"] = ids[str(node["wire_id"])]
        if "children" in node:
            if not isinstance(node["children"], list):
                raise ValueError(f"node {index} children must be an array")
            node["children"] = [pointer(value, ids, path=f"nodes[{index - 1}].children") for value in node["children"]]
        if "references" in node:
            node["references"] = normalize_references(node["references"], ids, f"nodes[{index - 1}].references")
    return result


def normalize_references(value: Any, ids: dict[str, str], path: str) -> Any:
    if isinstance(value, dict):
        return {key: normalize_references(item, ids, f"{path}.{key}") for key, item in value.items()}
    if isinstance(value, list):
        return [normalize_references(item, ids, f"{path}[]") for item in value]
    if value is None:
        return None
    return pointer(value, ids, path=path)


def canonical(value: Any) -> str:
    return json.dumps(value, sort_keys=True, indent=2, ensure_ascii=False) + "\n"


def main() -> int:
    args = parse_args()
    try:
        baseline = normalize(load(args.baseline))
        protobuf = normalize(load(args.protobuf))
    except ValueError as error:
        raise SystemExit(f"graph validation failed: {error}") from error
    baseline_text, protobuf_text = canonical(baseline), canonical(protobuf)
    report = {
        "equal": baseline_text == protobuf_text,
        "baseline": str(args.baseline.resolve()),
        "protobuf": str(args.protobuf.resolve()),
        "normalization": "wire_id plus children/references pointer values only",
        "baseline_nodes": len(baseline["nodes"]),
        "protobuf_nodes": len(protobuf["nodes"]),
    }
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2) + "\n")
    if baseline_text != protobuf_text:
        if args.output:
            args.output.with_suffix(args.output.suffix + ".baseline.json").write_text(baseline_text)
            args.output.with_suffix(args.output.suffix + ".protobuf.json").write_text(protobuf_text)
        print(json.dumps(report, indent=2))
        return 1
    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
