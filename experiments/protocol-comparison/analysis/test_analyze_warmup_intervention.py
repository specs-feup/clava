from __future__ import annotations

import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import uuid

sys.path.insert(0, str(Path(__file__).resolve().parent))
import analyze_warmup_intervention as intervention


def _sha(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def _write_jsonl(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in rows), encoding="utf-8")


def _read_jsonl(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def _schedule_row(suite: str, input_id: str, protocol: str, repeat: int,
                  phase: str = "measure") -> dict:
    event_id = str(uuid.uuid5(uuid.NAMESPACE_URL, f"event:{suite}:{input_id}"))
    group_id = str(uuid.uuid5(uuid.NAMESPACE_URL, f"group:{suite}:{input_id}"))
    label = "<empty source group>" if input_id.endswith("0001") and suite == "clava-js" else f"src/{input_id}.cpp"
    call = {
        "cache_enabled": True,
        "ccache_disabled": False,
        "ccache_nocompress": True,
        "compressed": False,
        "wire_format": protocol,
    }
    eligible_limit = 166 if suite == "clava-js" else 208
    ordinal = int(input_id[-4:])
    native_calls = [call] if ordinal <= eligible_limit else []
    return {
        "phase": phase,
        "suite": suite,
        "input_id": input_id,
        "source_label": label,
        "event_id": event_id,
        "group_id": group_id,
        "source_sha256": _sha(f"source:{suite}:{input_id}"),
        "args_sha256": _sha(f"args:{suite}:{input_id}"),
        "options_sha256": _sha(f"options:{suite}:{input_id}"),
        "protocol": protocol,
        "wire_format": protocol,
        "cache_mode": "warm",
        "compression_policy": "raw_control",
        "ccache_output_compression": "disabled_by_CCACHE_NOCOMPRESS",
        "cache_policy_requested": "enabled_for_eligible_sources",
        "expected_ccache_disabled": False,
        "compression_enabled": False,
        "disable_compression": True,
        "expected_compressed": False,
        "expected_native_count": len(native_calls),
        "expected_native_calls": native_calls,
        "parser_config": {"show_exec_info": False, "ast_dump_cache": True},
        "repeat": 0 if phase == "warmup" else repeat,
    }


def _make_fixture(root: Path) -> None:
    for treatment in intervention.TREATMENTS:
        for suite in intervention.SUITES:
            count = intervention.GROUP_COUNTS[suite]
            for protocol in intervention.PROTOCOLS:
                for repeat in intervention.REPEATS:
                    prefix = treatment == "prefix-java-nas"
                    schedule_rows = []
                    if prefix:
                        schedule_rows.extend(
                            _schedule_row("java", input_id, protocol, 0, "warmup")
                            for input_id in intervention.PREFIX_IDS
                        )
                    schedule_rows.extend(
                        _schedule_row(suite, input_id, protocol, repeat)
                        for input_id in intervention._group_ids(suite, count)
                    )
                    schedule_path = root / "schedules" / intervention._schedule_filename(
                        treatment, suite, protocol, repeat
                    )
                    observation_path = root / "observations" / intervention._observation_filename(
                        treatment, suite, protocol, repeat
                    )
                    proof_path = root / "warm-counters" / intervention._counter_filename(
                        treatment, suite, protocol, repeat
                    )
                    manifest_path = root / "run-manifests" / intervention._manifest_filename(
                        treatment, suite, protocol, repeat
                    )
                    _write_jsonl(schedule_path, schedule_rows)

                    observations = []
                    treatment_offset = 0.5 if prefix else 0.0
                    protocol_offset = 2.0 if protocol == "protobuf" else 0.0
                    for line_number, schedule in enumerate(schedule_rows, 1):
                        ordinal = int(schedule["input_id"][-4:])
                        observation = {
                            "record_type": "parse",
                            "schedule_line": line_number,
                            "phase": schedule["phase"],
                            "suite": schedule["suite"],
                            "protocol": protocol,
                            "cache_mode": "warm",
                            "repeat": schedule["repeat"],
                            "input_id": schedule["input_id"],
                            "event_id": schedule["event_id"],
                            "group_id": schedule["group_id"],
                            "source_sha256": schedule["source_sha256"],
                            "args_sha256": schedule["args_sha256"],
                            "options_sha256": schedule["options_sha256"],
                            "source_label": schedule["source_label"],
                            "valid": True,
                            "app_returned_null": False,
                        }
                        if schedule["phase"] == "measure":
                            observation["elapsed_ms"] = (
                                10.0 + ordinal / 100.0 + repeat / 10.0
                                + protocol_offset + treatment_offset
                            )
                        observations.append(observation)
                    _write_jsonl(observation_path, observations)

                    expected_hits = intervention.EXPECTED_ELIGIBLE_CALLS[(treatment, suite)]
                    proof_rows = [
                        {"record_type": "ccache", "input_id": "__ccache__",
                         "operation": "ccache_zero", "command_status": 0,
                         "output": "Statistics zeroed\n"},
                        *observations,
                        {"record_type": "ccache", "input_id": "__ccache__",
                         "operation": "ccache_stats", "command_status": 0,
                         "output": (f"Cacheable calls: {expected_hits}\n"
                                    f"Hits: {expected_hits}\nMisses: 0\n")},
                    ]
                    _write_jsonl(proof_path, proof_rows)
                    manifest = {
                        "schema_version": 1,
                        "treatment": "control" if not prefix else "prefix-java-nas",
                        "suite": suite,
                        "protocol": protocol,
                        "cache_mode": "warm",
                        "repeat": repeat,
                        "schedule_path": schedule_path.name,
                        "schedule_sha256": intervention._jsonl_sha256(schedule_path),
                        "observation_path": observation_path.name,
                        "ccache_proof_path": proof_path.name,
                        "java_argv": ["java", "-cp", "runner.jar"],
                        "cwd": "/test/worktree",
                        "env": {name: None for name in intervention.JVM_OPTION_ENV},
                        "explicit_gc_policy_flags": [],
                        "gc_policy": intervention.GC_POLICY,
                        "runner_has_no_explicit_gc_call": True,
                        "show_exec_info": False,
                        "runner_class_sha256": "a" * 64,
                        "overlay_class_manifest_sha256": "b" * 64,
                        "jar_manifest_sha256": "c" * 64,
                        "native_tool_sha256": "d" * 64,
                        "started_utc": "2026-10-01T00:00:00Z",
                        "finished_utc": "2026-10-01T00:01:00Z",
                        "exit_code": 0,
                    }
                    manifest_path.parent.mkdir(parents=True, exist_ok=True)
                    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


class AnalyzeWarmupInterventionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix="warmup-intervention-test-")
        cls.root = Path(cls.temp.name) / "fixture"
        _make_fixture(cls.root)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_complete_fixture_aggregates_only_the_two_measured_rounds(self):
        summary = intervention.analyze_warmup_intervention(self.root)
        self.assertEqual(summary["validation"]["schedule_cells"], 16)
        self.assertEqual(summary["validation"]["measured_observation_count"], 4128)
        self.assertEqual(summary["validation"]["unmeasured_prefix_rows"], 64)
        self.assertEqual(len(summary["cell_totals"]), 16)
        self.assertEqual(len(summary["paired_format_deltas"]), 8)
        self.assertEqual(len(summary["same_format_treatment_deltas"]), 8)
        self.assertEqual(len(summary["key_group_rounds"]), 16)
        self.assertEqual({row["round_count"] for row in summary["group_summaries"]}, {2})
        self.assertNotIn("/test/worktree", json.dumps(summary))

        fragment = intervention.render_warmup_intervention_html(summary)
        self.assertEqual(fragment.count("<svg "), 2)
        self.assertIn('viewBox="0 0 360 360"', fragment)
        self.assertIn("Two rounds only, separate from the primary six-round comparison", fragment)
        self.assertIn("@media(max-width:560px)", fragment)
        self.assertNotIn("overflow-x", fragment)
        self.assertNotIn("/test/worktree", fragment)

    def test_missing_provenance_hash_is_rejected(self):
        path = self.root / "observations" / "control-clava-js-text-warm-r01.jsonl"
        original = _read_jsonl(path)
        damaged = copy.deepcopy(original)
        del damaged[0]["options_sha256"]
        _write_jsonl(path, damaged)
        try:
            with self.assertRaisesRegex(intervention.AnalysisError, "options_sha256 differs from schedule"):
                intervention.analyze_warmup_intervention(self.root)
        finally:
            _write_jsonl(path, original)

    def test_source_label_can_be_derived_from_the_joined_schedule(self):
        path = self.root / "observations" / "control-clava-js-text-warm-r01.jsonl"
        original = _read_jsonl(path)
        damaged = copy.deepcopy(original)
        del damaged[1]["source_label"]
        _write_jsonl(path, damaged)
        try:
            summary = intervention.analyze_warmup_intervention(self.root)
            selected = next(row for row in summary["group_summaries"]
                            if row["treatment"] == "control"
                            and row["suite"] == "clava-js"
                            and row["protocol"] == "text"
                            and row["input_id"] == "js-group-0002")
            self.assertEqual(selected["source_label"], "js-group-0002.cpp")
        finally:
            _write_jsonl(path, original)

    def test_invalid_observation_is_rejected_not_silently_dropped(self):
        path = self.root / "observations" / "control-java-protobuf-warm-r02.jsonl"
        original = _read_jsonl(path)
        damaged = copy.deepcopy(original)
        damaged[0]["valid"] = False
        _write_jsonl(path, damaged)
        try:
            with self.assertRaisesRegex(intervention.AnalysisError, "invalid row"):
                intervention.analyze_warmup_intervention(self.root)
        finally:
            _write_jsonl(path, original)

    def test_treatment_schedule_must_keep_exact_measured_tail(self):
        path = self.root / "schedules" / "prefix-java-nas-java-text-warm-r01.jsonl"
        original = _read_jsonl(path)
        damaged = copy.deepcopy(original)
        damaged[8]["source_sha256"] = "0" * 64
        _write_jsonl(path, damaged)
        try:
            with self.assertRaisesRegex(intervention.AnalysisError, "measured schedule tail differs"):
                intervention.analyze_warmup_intervention(self.root)
        finally:
            _write_jsonl(path, original)

    def test_gc_flag_and_wrong_counter_proof_are_rejected(self):
        manifest_path = self.root / "run-manifests" / "run-control-java-text-warm-r01.json"
        original_manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest = copy.deepcopy(original_manifest)
        manifest["java_argv"].append("-XX:+DisableExplicitGC")
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        try:
            with self.assertRaisesRegex(intervention.AnalysisError, "explicit GC policy flag"):
                intervention.analyze_warmup_intervention(self.root)
        finally:
            manifest_path.write_text(json.dumps(original_manifest, indent=2) + "\n", encoding="utf-8")

        proof_path = self.root / "warm-counters" / "counterproof-control-clava-js-protobuf-warm-r02.txt"
        original_proof = _read_jsonl(proof_path)
        damaged = copy.deepcopy(original_proof)
        damaged[-1]["output"] = damaged[-1]["output"].replace("Misses: 0", "Misses: 1")
        _write_jsonl(proof_path, damaged)
        try:
            with self.assertRaisesRegex(intervention.AnalysisError, "eligible hits and zero misses"):
                intervention.analyze_warmup_intervention(self.root)
        finally:
            _write_jsonl(proof_path, original_proof)

    def test_synthetic_fixture_is_never_written_as_measured_output(self):
        summary = intervention.analyze_warmup_intervention(self.root)
        summary["evidence_label"] = "SYNTHETIC TEST FIXTURE"
        fragment = intervention.render_warmup_intervention_html(summary)
        self.assertIn("not measured report data", fragment)
        with tempfile.TemporaryDirectory(prefix="warmup-output-test-") as output:
            with self.assertRaisesRegex(intervention.AnalysisError, "refusing to write synthetic"):
                intervention.write_intervention_outputs(summary, Path(output))
            self.assertEqual(list(Path(output).iterdir()), [])


if __name__ == "__main__":
    unittest.main()
