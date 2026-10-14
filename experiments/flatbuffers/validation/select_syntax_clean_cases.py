#!/usr/bin/env python3
"""Select every fixed-corpus eager CLEAN case that passed the full syntax audit.

The resulting manifest retains the original manifest metadata and exact selected
case objects. The provenance file pins all inputs and records every selection
and exclusion; this tool never invokes a compiler or Clava.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

EXPECTED_MANIFEST_SHA256 = "e2d71d99190ec95c345214bc69064e438934660961fc56e302e6d9ed574fe6bd"
EXPECTED_CASE_COUNT = 1062


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_sha256(value: Any) -> str:
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    return sha256_bytes(encoded)


def read_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise SystemExit(f"cannot read JSON {path}: {exc}") from exc


def validation_probe_hash(paths: list[Path]) -> str:
    digest = hashlib.sha256()
    for path in sorted(paths, key=lambda item: item.name):
        digest.update(path.name.encode("utf-8"))
        digest.update(b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\xff")
    return digest.hexdigest()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", required=True, type=Path,
                        help="the pinned 1,062-case consumer-inputs.json")
    parser.add_argument("--consumer-summary", required=True, type=Path,
                        help="full eager+text one-shot summary.json")
    parser.add_argument("--syntax-summary", required=True, type=Path,
                        help="full_generated_syntax.json emitted by audit_generated_corpus_syntax.py")
    parser.add_argument("--syntax-cases", required=True, type=Path,
                        help="the paired .cases.jsonl file emitted by the syntax audit")
    parser.add_argument("--output-manifest", required=True, type=Path)
    parser.add_argument("--provenance", required=True, type=Path)
    parser.add_argument("--expected-manifest-sha256", default=EXPECTED_MANIFEST_SHA256)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    manifest_path = args.manifest.resolve(strict=True)
    consumer_summary_path = args.consumer_summary.resolve(strict=True)
    syntax_summary_path = args.syntax_summary.resolve(strict=True)
    syntax_cases_path = args.syntax_cases.resolve(strict=True)
    output_manifest_path = args.output_manifest.resolve()
    provenance_path = args.provenance.resolve()

    if output_manifest_path == provenance_path:
        raise SystemExit("output manifest and provenance paths must differ")
    for path in (output_manifest_path, provenance_path):
        if path.exists():
            raise SystemExit(f"refusing to overwrite existing output: {path}")

    manifest_bytes = manifest_path.read_bytes()
    manifest_sha = sha256_bytes(manifest_bytes)
    if manifest_sha != args.expected_manifest_sha256:
        raise SystemExit(f"fixed manifest hash drift: {manifest_sha}")
    manifest = json.loads(manifest_bytes)
    manifest_cases = manifest.get("files")
    if not isinstance(manifest_cases, list) or len(manifest_cases) != EXPECTED_CASE_COUNT:
        raise SystemExit(f"fixed manifest must contain {EXPECTED_CASE_COUNT} cases")
    manifest_by_relative: dict[str, dict[str, Any]] = {}
    for case in manifest_cases:
        if not isinstance(case, dict) or not isinstance(case.get("relative"), str):
            raise SystemExit("manifest contains a case without a relative path")
        relative = case["relative"]
        if relative in manifest_by_relative:
            raise SystemExit(f"duplicate manifest case: {relative}")
        manifest_by_relative[relative] = case

    consumer_summary = read_json(consumer_summary_path)
    if consumer_summary.get("consumer_manifest_sha256") != manifest_sha:
        raise SystemExit("consumer summary does not match the pinned manifest")
    if consumer_summary.get("reparse_generated") is not False:
        raise SystemExit("expected a full one-shot consumer summary")
    if Path(consumer_summary.get("consumer_manifest", "")).resolve() != manifest_path:
        raise SystemExit("consumer summary names a different manifest path")
    consumer_results = consumer_summary.get("results")
    if not isinstance(consumer_results, list):
        raise SystemExit("consumer summary has no runtime results")
    by_label = {item.get("label"): item for item in consumer_results if isinstance(item, dict)}
    if len(by_label) != len(consumer_results) or set(by_label) != {"eager", "text"}:
        raise SystemExit("full consumer summary must contain exactly eager and text results")

    eager_rows: dict[str, dict[str, Any]] = {}
    for label in ("eager", "text"):
        rows = by_label[label].get("rows")
        if not isinstance(rows, list) or len(rows) != EXPECTED_CASE_COUNT:
            raise SystemExit(f"{label} consumer rows do not cover all fixed inputs")
        seen: dict[str, dict[str, Any]] = {}
        for row in rows:
            if not isinstance(row, dict) or not isinstance(row.get("relative"), str):
                raise SystemExit(f"invalid {label} consumer result row")
            if row["relative"] in seen:
                raise SystemExit(f"duplicate {label} consumer result: {row['relative']}")
            seen[row["relative"]] = row
        if set(seen) != set(manifest_by_relative):
            raise SystemExit(f"{label} result rows do not match the exact fixed manifest")
        if label == "eager":
            eager_rows = seen

    syntax_summary = read_json(syntax_summary_path)
    if syntax_summary.get("manifest_sha256") != manifest_sha:
        raise SystemExit("syntax audit does not match the pinned manifest")
    if Path(syntax_summary.get("manifest", "")).resolve() != manifest_path:
        raise SystemExit("syntax audit names a different manifest path")
    if syntax_summary.get("consumer_summary") != str(consumer_summary_path):
        raise SystemExit("syntax audit names a different full consumer summary")
    if syntax_summary.get("inputs_expected") != EXPECTED_CASE_COUNT:
        raise SystemExit("syntax audit did not cover the full fixed corpus")
    if syntax_summary.get("all_clean_row_hashes_verified_before_compilation") is not True:
        raise SystemExit("syntax audit lacks its pre-compilation generated-output hash gate")
    if syntax_summary.get("source_tree_modified") is not False:
        raise SystemExit("syntax audit reports source-tree modification")
    if Path(syntax_summary.get("detailed_case_records", "")).resolve() != syntax_cases_path:
        raise SystemExit("syntax summary names a different detailed case file")

    lines = syntax_cases_path.read_text(encoding="utf-8").splitlines()
    if not lines:
        raise SystemExit("syntax case file is empty")
    try:
        header = json.loads(lines[0])
        case_records = [json.loads(line) for line in lines[1:]]
    except json.JSONDecodeError as exc:
        raise SystemExit(f"invalid syntax case JSONL: {exc}") from exc
    if header.get("record_type") != "header":
        raise SystemExit("syntax case file has no header record")
    if header.get("manifest_sha256") != manifest_sha or header.get("manifest") != str(manifest_path):
        raise SystemExit("syntax case header does not match the fixed manifest")
    if header.get("summary") != str(consumer_summary_path):
        raise SystemExit("syntax case header names a different consumer summary")
    labels = header.get("runtime_labels")
    if not isinstance(labels, list) or set(labels) != {"eager", "text"}:
        raise SystemExit("syntax audit does not contain eager and text runtimes")

    records_by_label: dict[str, dict[str, dict[str, Any]]] = {"eager": {}, "text": {}}
    allowed_statuses = {"PASS", "FAIL", "TIMEOUT"}
    for row in case_records:
        if not isinstance(row, dict) or row.get("record_type") != "case":
            raise SystemExit("invalid non-case record in syntax case file")
        label = row.get("label")
        relative = row.get("relative")
        if label not in records_by_label or relative not in manifest_by_relative:
            raise SystemExit(f"unexpected syntax record identity: {label}/{relative}")
        if relative in records_by_label[label]:
            raise SystemExit(f"duplicate syntax record: {label}/{relative}")
        if row.get("status") not in allowed_statuses:
            raise SystemExit(f"unexpected syntax status for {label}/{relative}")
        records_by_label[label][relative] = row

    eager_clean = {relative for relative, row in eager_rows.items() if row.get("bucket") == "CLEAN"}
    audited_eager_clean = set(records_by_label["eager"])
    if audited_eager_clean != eager_clean:
        missing = sorted(eager_clean - audited_eager_clean)
        extra = sorted(audited_eager_clean - eager_clean)
        raise SystemExit(f"eager syntax records do not cover exact CLEAN rows: missing={missing}, extra={extra}")

    selected_relatives: set[str] = set()
    exclusions: list[dict[str, str]] = []
    status_counts = {status: 0 for status in sorted(allowed_statuses)}
    selected_evidence: dict[str, dict[str, Any]] = {}
    for relative, case in manifest_by_relative.items():
        consumer_row = eager_rows[relative]
        if consumer_row.get("bucket") != "CLEAN":
            exclusions.append({"relative": relative, "reason": "EAGER_NOT_CLEAN"})
            continue
        audit_row = records_by_label["eager"][relative]
        status = audit_row["status"]
        status_counts[status] += 1
        if status != "PASS":
            exclusions.append({"relative": relative, "reason": f"EAGER_SYNTAX_{status}"})
            continue

        source = Path(str(case.get("source", ""))).resolve(strict=True)
        if str(source) != audit_row.get("source") or audit_row.get("source") != consumer_row.get("source"):
            raise SystemExit(f"source path drift for eager/{relative}")
        source_sha = sha256_file(source)
        if source_sha != audit_row.get("source_sha256") or source_sha != consumer_row.get("source_sha256"):
            raise SystemExit(f"source SHA-256 drift for eager/{relative}")
        expected_standard = case.get("standard") or ("c11" if source.suffix.lower() == ".c" else "c++17")
        expected_options = case.get("options") or []
        if audit_row.get("standard") != expected_standard or consumer_row.get("standard") != expected_standard:
            raise SystemExit(f"standard drift for eager/{relative}")
        if audit_row.get("options") != expected_options or consumer_row.get("options") != expected_options:
            raise SystemExit(f"compiler option drift for eager/{relative}")
        if audit_row.get("index") != consumer_row.get("index"):
            raise SystemExit(f"generated index drift for eager/{relative}")
        if audit_row.get("generated_code_sha256") != consumer_row.get("generated_code_sha256"):
            raise SystemExit(f"generated-code hash drift for eager/{relative}")

        generated_files = [Path(str(value)).resolve(strict=True) for value in audit_row.get("generated_files", [])]
        if not generated_files:
            raise SystemExit(f"no generated files recorded for eager/{relative}")
        if validation_probe_hash(generated_files) != audit_row.get("generated_code_sha256"):
            raise SystemExit(f"generated output bytes drift for eager/{relative}")
        translation_unit = Path(str(audit_row.get("translation_unit", ""))).resolve(strict=True)
        if sha256_file(translation_unit) != audit_row.get("translation_unit_sha256"):
            raise SystemExit(f"generated translation unit drift for eager/{relative}")
        if audit_row.get("return_code") != 0 or audit_row.get("timed_out") is not False:
            raise SystemExit(f"PASS syntax row has inconsistent compiler result for eager/{relative}")

        selected_relatives.add(relative)
        selected_evidence[relative] = {
            "original_manifest_entry": case,
            "original_manifest_entry_sha256": canonical_sha256(case),
            "consumer_bucket": consumer_row["bucket"],
            "consumer_generated_code_sha256": consumer_row["generated_code_sha256"],
            "syntax_status": status,
            "syntax_case_record_sha256": canonical_sha256(audit_row),
            "source_sha256": source_sha,
            "standard": expected_standard,
            "options": expected_options,
            "generated_index": audit_row["index"],
            "generated_files": [str(path) for path in generated_files],
            "generated_code_sha256": audit_row["generated_code_sha256"],
            "translation_unit": str(translation_unit),
            "translation_unit_sha256": audit_row["translation_unit_sha256"],
            "compiler": audit_row["compiler"],
            "compiler_version": audit_row["compiler_version"],
            "syntax_command": audit_row["command"],
            "syntax_command_shell_quoted": audit_row["command_shell_quoted"],
        }

    if not selected_relatives:
        raise SystemExit("no eager CLEAN cases passed the full syntax audit")
    selected_cases = [case for case in manifest_cases if case["relative"] in selected_relatives]
    if len(selected_cases) != len(selected_relatives):
        raise SystemExit("selected manifest entries are not one-to-one")
    subset_manifest = dict(manifest)
    subset_manifest["files"] = selected_cases
    subset_bytes = (json.dumps(subset_manifest, indent=2, ensure_ascii=False) + "\n").encode("utf-8")

    syntax_sha = sha256_file(syntax_summary_path)
    syntax_cases_sha = sha256_file(syntax_cases_path)
    consumer_sha = sha256_file(consumer_summary_path)
    script_sha = sha256_file(Path(__file__).resolve())
    provenance = {
        "format": "eager-syntax-clean-selection-v1",
        "selection_rule": "include every original fixed-manifest entry whose full eager consumer row is CLEAN and whose corresponding full syntax-audit case status is PASS",
        "fixed_manifest": str(manifest_path),
        "fixed_manifest_sha256": manifest_sha,
        "fixed_manifest_case_count": len(manifest_cases),
        "consumer_summary": str(consumer_summary_path),
        "consumer_summary_sha256": consumer_sha,
        "eager_runtime": by_label["eager"].get("runtime"),
        "syntax_summary": str(syntax_summary_path),
        "syntax_summary_sha256": syntax_sha,
        "syntax_case_records": str(syntax_cases_path),
        "syntax_case_records_sha256": syntax_cases_sha,
        "selector_script": str(Path(__file__).resolve()),
        "selector_script_sha256": script_sha,
        "selection_counts": {
            "eager_consumer_clean": len(eager_clean),
            "eager_clean_syntax_status": status_counts,
            "selected_eager_clean_syntax_pass": len(selected_cases),
            "excluded_eager_not_clean": sum(item["reason"] == "EAGER_NOT_CLEAN" for item in exclusions),
            "excluded_eager_clean_syntax_not_pass": sum(item["reason"] != "EAGER_NOT_CLEAN" for item in exclusions),
        },
        "subset_manifest": str(output_manifest_path),
        "subset_manifest_sha256": sha256_bytes(subset_bytes),
        "subset_case_count": len(selected_cases),
        "excluded_cases": exclusions,
        "selected_cases": [selected_evidence[case["relative"]] for case in selected_cases],
        "manual_exclusions": [],
        "waivers_applied": 0,
    }
    provenance_bytes = (json.dumps(provenance, indent=2, ensure_ascii=False) + "\n").encode("utf-8")

    output_manifest_path.parent.mkdir(parents=True, exist_ok=True)
    provenance_path.parent.mkdir(parents=True, exist_ok=True)
    if output_manifest_path.exists() or provenance_path.exists():
        raise SystemExit("refusing to overwrite a selection output created concurrently")
    output_manifest_path.write_bytes(subset_bytes)
    provenance_path.write_bytes(provenance_bytes)
    print(json.dumps({
        "subset_manifest": str(output_manifest_path),
        "subset_manifest_sha256": sha256_bytes(subset_bytes),
        "selection_provenance": str(provenance_path),
        "selected_cases": len(selected_cases),
        "eager_consumer_clean": len(eager_clean),
        "eager_clean_syntax_status": status_counts,
        "excluded_cases": len(exclusions),
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
