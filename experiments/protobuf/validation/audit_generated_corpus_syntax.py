#!/usr/bin/env python3
"""Audit CLEAN Clava corpus outputs with LLVM 18 syntax-only checks.

The script first verifies the manifest, source hashes, row metadata, generated
file sets, and ValidationProbe aggregate generated-code hashes for every CLEAN
row in every runtime. No compiler process starts unless all of those checks
pass. Each translated source is then checked with its recorded standard and
options, plus the source parent's quote-include path used by the roundtrip
probe. Full commands and compiler output are streamed to a JSONL sidecar.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shlex
import subprocess
import sys
from typing import Any


EXPECTED_FULL_MANIFEST_SHA256 = "e2d71d99190ec95c345214bc69064e438934660961fc56e302e6d9ed574fe6bd"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validation_probe_hash(files: list[Path]) -> str:
    """Match ValidationProbe.generatedCodeHash(List<File>) exactly."""
    digest = hashlib.sha256()
    for path in sorted(files, key=lambda item: item.name):
        digest.update(path.name.encode("utf-8"))
        digest.update(b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\xff")
    return digest.hexdigest()


def infer_standard(source: Path) -> str:
    return "c11" if source.suffix.lower() == ".c" else "c++17"


def compiler_for(source: Path, clang: str, clangxx: str) -> str:
    return clang if source.suffix.lower() == ".c" else clangxx


def read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text())
    except (OSError, json.JSONDecodeError) as exc:
        raise SystemExit(f"cannot read JSON file {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise SystemExit(f"expected JSON object in {path}")
    return value


def runtime_rows(result: dict[str, Any], manifest_files: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    label = result.get("label")
    raw_command = result.get("command")
    rows = result.get("rows")
    if not isinstance(label, str) or not isinstance(raw_command, list) or not raw_command:
        raise SystemExit(f"runtime result is missing label or command: {result}")
    if not isinstance(rows, list):
        raise SystemExit(f"runtime {label} has no rows")

    command = [str(part) for part in raw_command]
    work_root = Path(command[-1]).resolve()
    unique: dict[str, dict[str, Any]] = {}
    for row in rows:
        if not isinstance(row, dict) or not isinstance(row.get("relative"), str):
            raise SystemExit(f"invalid row in runtime {label}")
        relative = row["relative"]
        if relative in unique:
            raise SystemExit(f"duplicate {label} result row: {relative}")
        unique[relative] = row
    if set(unique) != set(manifest_files):
        missing = sorted(set(manifest_files) - set(unique))
        extra = sorted(set(unique) - set(manifest_files))
        raise SystemExit(f"{label} rows do not match manifest: missing={missing}, extra={extra}")

    verified: list[dict[str, Any]] = []
    for relative, row in unique.items():
        if row.get("bucket") != "CLEAN":
            continue
        case = manifest_files[relative]
        source = Path(str(case.get("source", ""))).resolve()
        row_source = Path(str(row.get("source", ""))).resolve()
        if row_source != source:
            raise SystemExit(f"{label} source path drift for {relative}: {row_source} != {source}")
        if not source.is_file():
            raise SystemExit(f"source is missing for {relative}: {source}")

        source_hash = sha256_file(source)
        if row.get("source_sha256") != source_hash:
            raise SystemExit(f"{label} source SHA-256 drift for {relative}")
        expected_standard = case.get("standard") or infer_standard(source)
        if row.get("standard") != expected_standard:
            raise SystemExit(f"{label} standard drift for {relative}: {row.get('standard')} != {expected_standard}")
        expected_options = case.get("options") or []
        if row.get("options") != expected_options:
            raise SystemExit(f"{label} options drift for {relative}")
        if not isinstance(row.get("index"), int) or row["index"] < 0:
            raise SystemExit(f"{label} row has no valid generation index for {relative}")

        generated_dir = work_root / "generated" / f"{row['index']:05d}"
        if not generated_dir.is_dir():
            raise SystemExit(f"generated directory is missing for {label}/{relative}: {generated_dir}")
        generated_files = sorted((path for path in generated_dir.iterdir() if path.is_file()),
                                 key=lambda item: item.name)
        if not generated_files:
            raise SystemExit(f"no generated files for {label}/{relative}: {generated_dir}")
        actual_generated_hash = validation_probe_hash(generated_files)
        if row.get("generated_code_sha256") != actual_generated_hash:
            raise SystemExit(
                f"{label} generated-code SHA-256 drift for {relative}: "
                f"row={row.get('generated_code_sha256')} files={actual_generated_hash}"
            )

        primary = generated_dir / source.name
        if not primary.is_file():
            raise SystemExit(f"generated translation unit is missing for {label}/{relative}: {primary}")

        verified.append({
            "label": label,
            "relative": relative,
            "source": str(source),
            "source_sha256": source_hash,
            "standard": expected_standard,
            "options": expected_options,
            "index": row["index"],
            "work_root": str(work_root),
            "generated_directory": str(generated_dir),
            "generated_files": [str(path) for path in generated_files],
            "generated_code_sha256": actual_generated_hash,
            "translation_unit": str(primary),
            "translation_unit_sha256": sha256_file(primary),
        })
    return verified


def compiler_version(compiler: str) -> str:
    try:
        completed = subprocess.run([compiler, "--version"], capture_output=True,
                                   text=True, check=False, timeout=10)
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise SystemExit(f"cannot query compiler version for {compiler}: {exc}") from exc
    version = (completed.stdout or completed.stderr).splitlines()
    if completed.returncode != 0 or not version:
        raise SystemExit(f"compiler version query failed for {compiler}: {completed.returncode}")
    first_line = version[0]
    if "clang version 18." not in first_line.lower():
        raise SystemExit(f"expected LLVM/Clang 18, got {first_line!r} from {compiler}")
    return first_line


def syntax_command(item: dict[str, Any], clang: str, clangxx: str) -> list[str]:
    compiler = compiler_for(Path(item["source"]), clang, clangxx)
    # The dumper accepts cc1's split float ABI option. The independent driver
    # needs it forwarded explicitly, at the same position in the option list.
    options: list[str] = []
    values = [str(value) for value in item["options"]]
    index = 0
    while index < len(values):
        value = values[index]
        if value == "-mfloat-abi" and (index == 0 or values[index - 1] != "-Xclang"):
            if index + 1 == len(values):
                raise ValueError("Missing value for frontend option -mfloat-abi")
            options.extend(["-Xclang", value, "-Xclang", values[index + 1]])
            index += 2
        else:
            options.append(value)
            index += 1
    return [compiler, "-fsyntax-only", f"-std={item['standard']}",
            *options,
            "-iquote" + str(Path(item["source"]).parent),
            item["translation_unit"]]


def run_syntax(command: list[str], timeout_seconds: float) -> dict[str, Any]:
    try:
        completed = subprocess.run(command, capture_output=True, text=True,
                                   check=False, timeout=timeout_seconds)
        return {"return_code": completed.returncode, "timed_out": False,
                "stdout": completed.stdout, "stderr": completed.stderr}
    except subprocess.TimeoutExpired as exc:
        def decode(value: str | bytes | None) -> str:
            if value is None:
                return ""
            if isinstance(value, bytes):
                return value.decode(errors="replace")
            return value
        return {"return_code": None, "timed_out": True,
                "stdout": decode(exc.stdout), "stderr": decode(exc.stderr)}
    except OSError as exc:
        return {"return_code": None, "timed_out": False,
                "stdout": "", "stderr": f"compiler launch failed: {exc}"}


def syntax_status(record: dict[str, Any] | None) -> str:
    if record is None:
        return "NOT_CLEAN"
    if record.get("timed_out"):
        return "TIMEOUT"
    return "PASS" if record.get("return_code") == 0 else "FAIL"


def paired_bucket(eager: str, text: str) -> str:
    if eager == "PASS" and text == "PASS":
        return "BOTH_SYNTAX_PASS"
    if eager == "FAIL" and text == "FAIL":
        return "BOTH_SYNTAX_FAIL"
    if eager == "FAIL" and text == "PASS":
        return "EAGER_ONLY_SYNTAX_FAIL"
    if eager == "PASS" and text == "FAIL":
        return "TEXT_ONLY_SYNTAX_FAIL"
    if eager == "NOT_CLEAN" and text == "NOT_CLEAN":
        return "BOTH_NOT_CLEAN"
    if eager == "NOT_CLEAN":
        return "EAGER_NOT_CLEAN"
    if text == "NOT_CLEAN":
        return "TEXT_NOT_CLEAN"
    if eager == "TIMEOUT" and text == "TIMEOUT":
        return "BOTH_TIMEOUT"
    if eager == "TIMEOUT":
        return "EAGER_TIMEOUT"
    if text == "TIMEOUT":
        return "TEXT_TIMEOUT"
    return "OTHER_INCOMPLETE_PAIR"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--summary", required=True, type=Path,
                        help="run_corpus_consumer.py summary.json")
    parser.add_argument("--output", required=True, type=Path,
                        help="final summary JSON; detailed compiler results use OUTPUT.cases.jsonl")
    parser.add_argument("--expected-manifest-sha256",
                        help="optional pin; the fixed 1,062-case manifest SHA is available by default")
    parser.add_argument("--allow-nonfixed-manifest", action="store_true",
                        help="allow a subset manifest rather than the pinned full corpus")
    parser.add_argument("--clang", default="/usr/lib/llvm-18/bin/clang")
    parser.add_argument("--clangxx", default="/usr/lib/llvm-18/bin/clang++")
    parser.add_argument("--timeout-seconds", type=float, default=120.0)
    args = parser.parse_args()

    if args.timeout_seconds <= 0:
        raise SystemExit("--timeout-seconds must be positive")
    manifest_bytes = args.manifest.read_bytes()
    manifest_sha = hashlib.sha256(manifest_bytes).hexdigest()
    if not args.allow_nonfixed_manifest and manifest_sha != EXPECTED_FULL_MANIFEST_SHA256:
        raise SystemExit(f"manifest is not the pinned 1,062-case input: {manifest_sha}")
    if args.expected_manifest_sha256 and manifest_sha != args.expected_manifest_sha256:
        raise SystemExit(f"manifest hash pin mismatch: {manifest_sha}")

    manifest = json.loads(manifest_bytes)
    cases = manifest.get("files")
    if not isinstance(cases, list) or not cases:
        raise SystemExit("manifest contains no files")
    manifest_files = {case.get("relative"): case for case in cases
                      if isinstance(case, dict) and isinstance(case.get("relative"), str)}
    if len(manifest_files) != len(cases):
        raise SystemExit("manifest has invalid or duplicate relative paths")

    summary = read_json(args.summary)
    if summary.get("consumer_manifest_sha256") != manifest_sha:
        raise SystemExit("summary consumer manifest hash does not match the supplied manifest")
    results = summary.get("results")
    if not isinstance(results, list) or not results:
        raise SystemExit("summary has no runtime results")
    labels = [result.get("label") for result in results]
    if len(labels) != len(set(labels)):
        raise SystemExit("summary runtime labels are not unique")

    # Complete every metadata, source, and generated-output hash check for all
    # runtimes before even querying a compiler version or starting compilation.
    verified_by_label: dict[str, list[dict[str, Any]]] = {}
    for result in results:
        verified_by_label[result["label"]] = runtime_rows(result, manifest_files)
    if not any(verified_by_label.values()):
        raise SystemExit("summary contains no CLEAN runtime outputs to syntax-check")

    output = args.output.resolve()
    cases_output = Path(str(output) + ".cases.jsonl")
    if output.exists() or cases_output.exists():
        raise SystemExit(f"refusing to overwrite syntax evidence: {output} or {cases_output}")
    output.parent.mkdir(parents=True, exist_ok=True)

    compiler_versions = {args.clang: compiler_version(args.clang),
                         args.clangxx: compiler_version(args.clangxx)}
    labels_by_relative: dict[str, dict[str, dict[str, Any]]] = {}
    for label, items in verified_by_label.items():
        for item in items:
            labels_by_relative.setdefault(item["relative"], {})[label] = item

    header = {
        "record_type": "header",
        "manifest": str(args.manifest.resolve()),
        "manifest_sha256": manifest_sha,
        "summary": str(args.summary.resolve()),
        "summary_created_utc": summary.get("created_utc"),
        "clang_versions": compiler_versions,
        "runtime_labels": labels,
        "case_count": len(cases),
        "verified_clean_output_count_by_runtime": {
            label: len(items) for label, items in verified_by_label.items()
        },
        "syntax_scope": "the generated translation unit matching the input source basename; all generated files are included in the verified aggregate row hash",
        "timeout_seconds": args.timeout_seconds,
        "pairing_rule": "paired buckets compare PASS/FAIL/TIMEOUT only when each runtime reported that source CLEAN; a non-CLEAN row is recorded as NOT_CLEAN",
    }
    records: list[dict[str, Any]] = []
    counts_by_runtime = {label: {"PASS": 0, "FAIL": 0, "TIMEOUT": 0}
                         for label in labels}

    with cases_output.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(header, ensure_ascii=False) + "\n")
        stream.flush()
        for label in labels:
            for item in verified_by_label[label]:
                command = syntax_command(item, args.clang, args.clangxx)
                result = run_syntax(command, args.timeout_seconds)
                status = syntax_status(result)
                if status != "NOT_CLEAN":
                    counts_by_runtime[label][status] += 1
                record = {
                    "record_type": "case",
                    **item,
                    "compiler": command[0],
                    "compiler_version": compiler_versions[command[0]],
                    "command": command,
                    "command_shell_quoted": shlex.join(command),
                    "status": status,
                    **result,
                }
                records.append(record)
                stream.write(json.dumps(record, ensure_ascii=False) + "\n")
                stream.flush()

    results_by_case = {(row["label"], row["relative"]): row for row in records}
    paired: list[dict[str, str]] = []
    pair_counts: dict[str, int] = {}
    for relative in sorted(manifest_files) if {"eager", "text"}.issubset(labels) else []:
        eager_record = results_by_case.get(("eager", relative))
        text_record = results_by_case.get(("text", relative))
        eager_status = syntax_status(eager_record)
        text_status = syntax_status(text_record)
        bucket = paired_bucket(eager_status, text_status)
        pair_counts[bucket] = pair_counts.get(bucket, 0) + 1
        paired.append({"relative": relative, "eager": eager_status,
                       "text": text_status, "bucket": bucket})

    final = {
        "manifest": str(args.manifest.resolve()),
        "manifest_sha256": manifest_sha,
        "consumer_summary": str(args.summary.resolve()),
        "consumer_summary_created_utc": summary.get("created_utc"),
        "compilers": compiler_versions,
        "inputs_expected": len(cases),
        "syntax_checks_by_runtime": counts_by_runtime,
        "paired_bucket_counts": pair_counts,
        "paired_buckets": paired,
        "detailed_case_records": str(cases_output),
        "all_clean_row_hashes_verified_before_compilation": True,
        "syntax_scope": header["syntax_scope"],
        "source_tree_modified": False,
        "interpretation": "syntax buckets describe LLVM 18 acceptance only; they do not waive generator or roundtrip failures",
    }
    output.write_text(json.dumps(final, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({
        "summary": str(output),
        "case_records": str(cases_output),
        "manifest_sha256": manifest_sha,
        "syntax_checks_by_runtime": counts_by_runtime,
        "paired_bucket_counts": pair_counts,
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
