#!/usr/bin/env python3
"""Alternate old and fixed SpecsUtils builds on the same Java parser suite."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
from datetime import datetime, timezone

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from run_comparison import run_java  # noqa: E402


def git_head(root: Path) -> str:
    return subprocess.check_output(
        ["git", "-C", str(root), "rev-parse", "HEAD"], text=True
    ).strip()


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--old-specs-java-libs", type=Path, required=True)
    parser.add_argument("--fixed-specs-java-libs", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--pairs", type=int, default=8)
    args = parser.parse_args()
    if args.pairs < 1:
        parser.error("--pairs must be positive")

    old_root = args.old_specs_java_libs.resolve()
    fixed_root = args.fixed_specs_java_libs.resolve()
    output_root = args.output_root.resolve()
    output_root.mkdir(parents=True, exist_ok=True)
    clava_root = Path(__file__).resolve().parents[3]
    native_tool = clava_root.parent / "clang-dumper/build/tool"
    if not native_tool.is_file():
        raise SystemExit(f"missing native tool: {native_tool}")

    variants = {"old": old_root, "fixed": fixed_root}
    build_inputs = {}
    for name, root in variants.items():
        source = root / "SpecsUtils/src/pt/up/fe/specs/util/SpecsSystem.java"
        jar = root / "SpecsUtils/build/libs/SpecsUtils.jar"
        if not source.is_file() or not jar.is_file():
            raise SystemExit(f"missing {name} SpecsUtils source or jar under {root}")
        build_inputs[name] = {
            "revision": git_head(root),
            "specs_system_source_sha256": sha256(source),
            "specsutils_jar_sha256": sha256(jar),
        }

    stage = {
        "root": clava_root.parent,
        "key": "protobuf",
        "label": "Protobuf branch, GC control",
        "wire": "protobuf",
        "cache": True,
    }
    metadata = {
        "created_at": datetime.now(timezone.utc).isoformat(),
        "clava_revision": git_head(clava_root),
        "native_tool_sha256": sha256(native_tool),
        "build_inputs": build_inputs,
        "mode": "direct",
        "suite": "java",
        "pairs": args.pairs,
    }
    (output_root / "plan.json").write_text(json.dumps(metadata, indent=2) + "\n")
    rows = []
    for pair in range(1, args.pairs + 1):
        order = ("old", "fixed") if pair % 2 else ("fixed", "old")
        for variant in order:
            os.environ["SPECS_JAVA_LIBS_HOME"] = str(variants[variant])
            result = run_java(stage, output_root / variant, pair, True, pair, "direct")
            result["gc_variant"] = variant
            result["specsutils_jar_sha256"] = build_inputs[variant]["specsutils_jar_sha256"]
            rows.append(result)
            (output_root / "results.json").write_text(json.dumps(rows, indent=2) + "\n")
            print(
                f"pair={pair} variant={variant} valid={result['valid']} "
                f"passed={result['passed_tests']}/{result['total_tests']} "
                f"elapsed={result.get('elapsed_s')}",
                flush=True,
            )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
