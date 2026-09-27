#!/usr/bin/env python3
"""Assemble selected six-valid-run manifests and audit attempts for the post-GC rerun."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import subprocess
from typing import Any


MODES: dict[str, dict[str, str]] = {
    "direct": {
        "before-cache": "direct/before-cache-retry",
        "ccache-text": "direct/ccache-text",
        "protobuf": "direct/protobuf",
        "flatbuffers": "direct/flatbuffers",
    },
    "cold": {
        "ccache-text": "cold/ccache-text",
        "protobuf": "cold/protobuf",
        "flatbuffers": "cold/flatbuffers",
    },
    "warm": {
        "ccache-text": "warm/ccache-text",
        "protobuf": "warm/protobuf",
        "flatbuffers": "warm/flatbuffers",
    },
}
RECOVERY = {
    ("direct", "protobuf"): "direct/protobuf-recovery",
    ("cold", "protobuf"): "cold/protobuf-recovery",
}
SUPERSEDED_CELLS = {("direct", "before-cache"): "direct/before-cache"}
EXPECTED_SUITES = ("clava-js", "java")
EXPECTED_REPEATS = 6


def read_json(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"expected JSON object: {path}")
    return value


def attach_evidence(row: dict[str, Any], result_root: Path, role: str) -> dict[str, Any]:
    copy = dict(row)
    run_dir_text = copy.get("run_dir")
    if not isinstance(run_dir_text, str):
        raise ValueError(f"benchmark row has no run_dir: {row}")
    run_dir = Path(run_dir_text).resolve()
    try:
        relative = run_dir.relative_to(result_root.resolve())
    except ValueError as error:
        raise ValueError(f"run is outside the evidence root: {run_dir}") from error
    copy["evidence_id"] = (
        f"{copy.get('mode')}-{copy.get('stage')}-{copy.get('suite')}-"
        f"{'warmup' if not copy.get('measured') else 'r' + str(copy.get('repeat'))}-"
        f"{role}"
    )
    copy["evidence_ref"] = (relative / "summary.json").as_posix()
    xml_dir = run_dir / "junit-results"
    recorded_xml = copy.get("junit_archive")
    if isinstance(recorded_xml, str) and Path(recorded_xml).exists():
        xml_dir = Path(recorded_xml)
    if xml_dir.is_dir():
        try:
            copy["junit_ref"] = xml_dir.resolve().relative_to(result_root.resolve()).as_posix()
        except ValueError:
            copy["junit_ref"] = None
    else:
        copy["junit_ref"] = None
    copy["junit_status"] = (
        "archived" if copy["junit_ref"] else
        "not archived before a later invocation replaced Gradle test output"
        if copy.get("failed_tests", 0) or copy.get("return_code", 0) != 0 else "not required for a passing run"
    )
    copy["selection_role"] = role
    copy.pop("run_dir", None)
    copy.pop("junit_archive", None)
    copy.pop("command", None)
    return copy


def load_rows(root: Path, relative: str) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    path = root / "matrix" / relative / "results.json"
    manifest = read_json(path)
    return manifest, [dict(row) for row in manifest.get("results", []) if isinstance(row, dict)]


def before_cache_dumper_evidence(root: Path) -> dict[str, str]:
    release_root = Path("clang-dumper/releases/v18.1.8_4")
    artifacts = {
        "Java": root / "matrix/direct/before-cache/temp/java/before-cache/clang_ast_exe_lmsousa"
                / release_root / "clang-dumper-linux-x64",
        "Clava-JS": root / "matrix/direct/before-cache/cache/clava-js/before-cache/@specs-feup/clava"
                    / release_root / "clang-dumper-linux-x64",
    }
    expected_sha: str | None = None
    for suite, binary in artifacts.items():
        manifest_path = binary.parent / "clang-dumper-release-manifest.json"
        manifest = read_json(manifest_path)
        expected = next((item.get("sha256") for item in manifest.get("assets", [])
                         if isinstance(item, dict) and item.get("filename") == binary.name), None)
        if not isinstance(expected, str):
            raise ValueError(f"release manifest has no checksum for {suite} dumper: {manifest_path}")
        actual = hashlib.sha256(binary.read_bytes()).hexdigest()
        if actual != expected:
            raise ValueError(f"{suite} before-cache dumper checksum mismatch: {actual} != {expected}")
        if expected_sha is not None and actual != expected_sha:
            raise ValueError("Clava-JS and Java before-cache stages loaded different dumper executables")
        expected_sha = actual
    assert expected_sha is not None
    return {
        "dumper_sha256": expected_sha,
        "dumper_artifact_ref": (
            "v18.1.8_4 Linux x64 release asset, checksum verified in retained Clava-JS and Java downloads"
        ),
    }


def jfr_gc_summary(path: Path, label: str, source_ref: str) -> dict[str, Any]:
    result = subprocess.run(
        ["jfr", "print", "--events", "jdk.SystemGC", "--json", str(path.resolve())],
        text=True, capture_output=True, check=False,
    )
    if result.returncode != 0:
        raise ValueError(f"jfr could not read {path}: {result.stderr.strip()}")
    recording = json.loads(result.stdout)
    events = recording.get("recording", {}).get("events", [])
    matched = []
    for event in events:
        frames = event.get("values", {}).get("stackTrace", {}).get("frames", [])
        signature = [
            (frame.get("method", {}).get("type", {}).get("name"),
             frame.get("method", {}).get("name"))
            for frame in frames[:4]
        ]
        if signature == [
            ("java/lang/Runtime", "gc"),
            ("java/lang/System", "gc"),
            ("pt/up/fe/specs/util/SpecsSystem", "getUsedMemory"),
            ("pt/up/fe/specs/clang/codeparser/ParallelCodeParser", "parse"),
        ]:
            matched.append(event)
    return {
        "format": label,
        "source_ref": source_ref,
        "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
        "jdk_system_gc_events": len(events),
        "explicit_requests_from_used_memory": len(matched),
        "stack": "Runtime.gc -> System.gc -> SpecsSystem.getUsedMemory -> ParallelCodeParser.parse",
    }


def write_gc_fix_evidence(output: Path, paths: dict[str, tuple[Path, str, str]]) -> None:
    if len(paths) != 4:
        raise ValueError("GC evidence needs the four pre/post Text/Protobuf JFR recordings")
    records = {key: jfr_gc_summary(path, label, source_ref)
               for key, (path, label, source_ref) in paths.items()}
    before = [records[key]["explicit_requests_from_used_memory"]
              for key in ("before_text", "before_protobuf")]
    after = [records[key]["explicit_requests_from_used_memory"]
             for key in ("after_text", "after_protobuf")]
    if len(set(before)) != 1 or len(set(after)) != 1:
        raise ValueError(f"unexpected per-format explicit-GC event counts: before={before}, after={after}")
    summary = {
        "fix_revision": "cea5be9123be8508805c03be381a26af796ae7ff",
        "measurement": "JFR jdk.SystemGC events with the exact Runtime.gc -> System.gc -> SpecsSystem.getUsedMemory -> ParallelCodeParser.parse stack",
        "important_distinction": "These are explicit GC request events, not completed collection counts or total pause time.",
        "pre_fix": {"per_run_requests": before[0], "profiles": [records["before_text"], records["before_protobuf"]]},
        "post_fix": {"per_run_requests": after[0], "profiles": [records["after_text"], records["after_protobuf"]]},
        "request_reduction_percent": round((1 - after[0] / before[0]) * 100, 1) if before[0] else None,
        "interpretation": (
            "The change removes the second unconditional System.gc() from getUsedMemory(boolean). "
            "ParallelCodeParser still calls getUsedMemory(true), so one conditional explicit request per parse remains."
        ),
    }
    (output / "gc-fix-evidence.json").write_text(
        json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--results-root", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--pre-fix-jfr-text", type=Path)
    parser.add_argument("--pre-fix-jfr-protobuf", type=Path)
    parser.add_argument("--post-fix-jfr-text", type=Path)
    parser.add_argument("--post-fix-jfr-protobuf", type=Path)
    args = parser.parse_args()
    root = args.results_root.resolve()
    output = args.output_root.resolve()
    output.mkdir(parents=True, exist_ok=True)

    primary: dict[str, list[dict[str, Any]]] = {mode: [] for mode in MODES}
    audit: dict[str, list[dict[str, Any]]] = {mode: [] for mode in MODES}
    stages_by_mode: dict[str, dict[str, dict[str, Any]]] = {mode: {} for mode in MODES}

    for mode, stage_paths in MODES.items():
        for stage, relative in stage_paths.items():
            manifest, rows = load_rows(root, relative)
            stage_metadata = next((item for item in manifest.get("stages", [])
                                   if isinstance(item, dict) and item.get("key") == stage), None)
            if stage_metadata is None:
                raise ValueError(f"missing stage metadata for {mode}/{stage} in {relative}")
            stage_metadata = dict(stage_metadata)
            if mode == "direct" and stage == "before-cache":
                stage_metadata.update(before_cache_dumper_evidence(root))
            stages_by_mode[mode][stage] = stage_metadata

            cell_rows = [row for row in rows if row.get("stage") == stage]
            recovery_rows: list[dict[str, Any]] = []
            recovery_relative = RECOVERY.get((mode, stage))
            if recovery_relative:
                recovery_manifest, recovery_rows = load_rows(root, recovery_relative)
                recovery_stage = next((item for item in recovery_manifest.get("stages", [])
                                       if isinstance(item, dict) and item.get("key") == stage), None)
                if recovery_stage != stage_metadata:
                    raise ValueError(f"recovery provenance differs for {mode}/{stage}")

            selected_by_suite: dict[str, list[dict[str, Any]]] = {}
            for suite in EXPECTED_SUITES:
                suite_rows = [row for row in cell_rows if row.get("suite") == suite]
                warmups = [row for row in suite_rows if row.get("measured") is False]
                measured = [row for row in suite_rows if row.get("measured") is True]
                if len(warmups) != 1 or len(measured) != EXPECTED_REPEATS:
                    raise ValueError(
                        f"unexpected primary cell size {mode}/{stage}/{suite}: "
                        f"{len(warmups)} warm-ups, {len(measured)} measured"
                    )
                if not warmups[0].get("valid"):
                    raise ValueError(f"selected warm-up is invalid: {mode}/{stage}/{suite}")

                selected = [row for row in measured if row.get("valid") is True]
                invalid = [row for row in measured if row.get("valid") is not True]
                for failed in invalid:
                    audit[mode].append(attach_evidence(failed, root, "failed-attempt"))
                if invalid:
                    if len(invalid) != 1 or not recovery_relative:
                        raise ValueError(f"unexpected invalid measured rows in {mode}/{stage}/{suite}")
                    supplements = [row for row in recovery_rows
                                   if row.get("suite") == suite and row.get("measured") is True
                                   and row.get("valid") is True]
                    if len(supplements) != 1:
                        raise ValueError(f"expected one valid replacement in {recovery_relative}/{suite}")
                    replacement = dict(supplements[0])
                    replacement["replaces_repeat"] = invalid[0].get("repeat")
                    replacement["source_repeat"] = replacement.get("repeat")
                    replacement["repeat"] = invalid[0].get("repeat")
                    selected.append(replacement)
                    recovery_warmups = [row for row in recovery_rows
                                        if row.get("suite") == suite and row.get("measured") is False]
                    if len(recovery_warmups) != 1:
                        raise ValueError(f"expected one recovery warm-up in {recovery_relative}/{suite}")
                    audit[mode].append(attach_evidence(
                        recovery_warmups[0], root, "replacement-cell-warmup"
                    ))

                if len(selected) != EXPECTED_REPEATS:
                    raise ValueError(f"expected six valid measured samples for {mode}/{stage}/{suite}")
                if any(row.get("valid") is not True for row in selected):
                    raise ValueError(f"invalid sample selected for headline timing: {mode}/{stage}/{suite}")
                selected_by_suite[suite] = [warmups[0], *sorted(selected, key=lambda row: int(row["repeat"]))]

            # Record the original direct/before-cache cell as a separate audit series. A failed
            # first Java warm-up prompted a complete recovery cell; the original six measurements
            # are retained to compare medians but are not mixed into the primary six-run sample.
            superseded_relative = SUPERSEDED_CELLS.get((mode, stage))
            if superseded_relative:
                superseded_manifest, superseded_rows = load_rows(root, superseded_relative)
                for row in superseded_rows:
                    if row.get("stage") != stage:
                        continue
                    audit[mode].append(attach_evidence(row, root, "superseded-initial-cell"))

            for suite in EXPECTED_SUITES:
                for row in selected_by_suite[suite]:
                    role = "replacement" if row.get("run_dir", "").find("-recovery/") >= 0 else "selected"
                    primary[mode].append(attach_evidence(row, root, role))

    # Ensure every observed failed direct/cold/warm matrix row is represented exactly once,
    # including failures in suites other than the two cells that required replacement.
    known_failures = {item["evidence_ref"] for items in audit.values() for item in items
                      if item.get("valid") is False and item.get("evidence_ref")}
    for mode, stage_paths in MODES.items():
        for stage, relative in stage_paths.items():
            _, rows = load_rows(root, relative)
            for row in rows:
                if row.get("valid") is False:
                    candidate = attach_evidence(row, root, "failed-attempt")
                    if candidate["evidence_ref"] not in known_failures:
                        audit[mode].append(candidate)
                        known_failures.add(candidate["evidence_ref"])

    for mode in MODES:
        output_manifest = {
            "schema": "post-gc-selected-six-valid-v1",
            "generation": "post-gc",
            "mode": mode,
            "created_at": datetime.now(timezone.utc).isoformat(),
            "specsutils_fix_revision": "cea5be9123be8508805c03be381a26af796ae7ff",
            "repeat_count": EXPECTED_REPEATS,
            "selection_policy": (
                "Use exactly six valid measured samples per suite/stage/mode. A single invalid measured "
                "Protobuf Java attempt is retained under audit_attempts and replaced by one valid same-cell "
                "recovery sample; warm-ups are never timed. The original failed attempt still counts in the "
                "correctness failure rate."
            ),
            "stages": list(stages_by_mode[mode].values()),
            "results": primary[mode],
            "audit_attempts": audit[mode],
            "headline_cell_counts": {
                f"{stage}/{suite}": {
                    "selected_measured": sum(row.get("stage") == stage and row.get("suite") == suite
                                              and row.get("measured") is True for row in primary[mode]),
                    "failed_measured_attempts": sum(row.get("stage") == stage and row.get("suite") == suite
                                                    and row.get("measured") is True and row.get("valid") is False
                                                    for row in audit[mode]),
                }
                for stage in stages_by_mode[mode]
                for suite in EXPECTED_SUITES
            },
        }
        (output / f"{mode}.json").write_text(
            json.dumps(output_manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8"
        )

    jfr_inputs = (
        args.pre_fix_jfr_text, args.pre_fix_jfr_protobuf,
        args.post_fix_jfr_text, args.post_fix_jfr_protobuf,
    )
    if any(jfr_inputs):
        if not all(jfr_inputs):
            raise ValueError("supply all four pre/post Text/Protobuf JFR files")
        write_gc_fix_evidence(output, {
            "before_text": (args.pre_fix_jfr_text, "Text", "separate dual-wire A/B scratch worktree; historical pre-fix profile"),
            "before_protobuf": (args.pre_fix_jfr_protobuf, "Protobuf", "separate dual-wire A/B scratch worktree; historical pre-fix profile"),
            "after_text": (args.post_fix_jfr_text, "Text", "current post-fix results bundle; preflight profile"),
            "after_protobuf": (args.post_fix_jfr_protobuf, "Protobuf", "current post-fix results bundle; preflight profile"),
        })

    print(json.dumps({
        "results_root": str(root),
        "output_root": str(output),
        "selected_invocations": {mode: len(rows) for mode, rows in primary.items()},
        "audit_attempts": {mode: len(rows) for mode, rows in audit.items()},
        "selected_invalid": sum(row.get("valid") is False for rows in primary.values() for row in rows),
        "audit_invalid": sum(row.get("valid") is False for rows in audit.values() for row in rows),
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
