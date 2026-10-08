from __future__ import annotations

import copy
import csv
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

import analyze_deadline as analyzer


def fixture_manifest():
    def digest(value):
        return hashlib.sha256(value.encode()).hexdigest()

    stages = {}
    for stage in analyzer.STAGES:
        stages[stage] = {
            "clava_revision": f"clava-{stage}",
            "clava_patch_sha256": digest(f"patch-{stage}"),
            "native_revision": f"native-{stage}",
            "native_binary_sha256": digest(f"binary-{stage}"),
            "parser_jar_sha256": digest(f"parser-{stage}"),
            "runtime_manifest_sha256": digest(f"runtime-{stage}"),
            "specsutils_revision": f"specsutils-{stage}",
            "specsutils_jar_sha256": digest(f"specsutils-jar-{stage}"),
        }
    cells = []
    for suite, mode, stage in sorted(analyzer.expected_cells()):
        cells.append({"suite": suite, "mode": mode, "stage": stage,
                      "warmup_count": 1, "repeat_count": 6})
    fixture_fingerprints = {
        category: {
            stage: {f"fixtures/{category}/{stage}.dat": digest(f"sha-{category}-{stage}")}
            for stage in analyzer.STAGES
        }
        for category in ("java_parser_resources", "js_imported_resources", "clava_js_sources")
    }
    compression_policy = {
        stage: {
            mode: {"active": mode != "direct" and stage != "before-cache"}
            for mode, supported in analyzer.STAGES_BY_MODE.items() if stage in supported
        }
        for stage in analyzer.STAGES
    }
    plan = {
        "experiment": "synthetic-test",
        "created_at": "not used in arithmetic",
        "repeat_count": 6,
        "warmup_count": 1,
        "suites": list(analyzer.SUITES),
        "cells": cells,
        "expected_tests": copy.deepcopy(analyzer.EXPECTED_TESTS),
        "cache_states": copy.deepcopy(analyzer.EXPECTED_CACHE_STATES),
        "timing_boundary": "subprocess wall",
        "stages": stages,
        "fixture_fingerprints": fixture_fingerprints,
        "compression_policy": compression_policy,
    }
    test_ids = {
        "clava-js": [
            {"file": "fixture.js", "test": f"test-{index:03d}",
             "status": "passed" if index < 158 else "skipped"}
            for index in range(164)
        ],
        "java": [
            {"class": "Fixture", "name": f"test-{index:03d}", "status": "passed"}
            for index in range(116)
        ],
    }
    identities = {suite: analyzer.test_identity_digest(test_ids[suite]) for suite in analyzer.SUITES}
    references = {
        suite: {"source_cell": "baseline", "sha256": identities[suite],
                "count": analyzer.EXPECTED_TESTS[suite]["total"]}
        for suite in analyzer.SUITES
    }
    warmup_identities = {
        suite: {
            analyzer.cell_id(suite, mode, stage, None): {
                "sha256": identities[suite], "count": analyzer.EXPECTED_TESTS[suite]["total"]
            }
            for row_suite, mode, stage in sorted(analyzer.expected_cells()) if row_suite == suite
        }
        for suite in analyzer.SUITES
    }
    results = []
    for suite, mode, stage in sorted(analyzer.expected_cells()):
        for repeat in (None, *analyzer.REPEATS):
            measured = repeat is not None
            attempt_id = analyzer.cell_id(suite, mode, stage, repeat)
            test_counts = analyzer.EXPECTED_TESTS[suite]
            cacheable = hits = misses = uncacheable = 0
            if mode == "cold":
                cacheable, misses = 2, 2
            elif mode == "warm":
                cacheable = 2
                if measured:
                    hits = 2
                else:
                    misses = 2
            compression = compression_policy[stage][mode]
            fixture_payload = {category: fixture_fingerprints[category][stage]
                               for category in fixture_fingerprints}
            fixture_digest = hashlib.sha256(json.dumps(
                fixture_payload, sort_keys=True, separators=(",", ":")
            ).encode("utf-8")).hexdigest()
            row = {
                "cell_id": attempt_id,
                "suite": suite,
                "mode": mode,
                "stage": stage,
                "measured": measured,
                "repeat": repeat,
                "attempt": 1,
                "selected": True,
                "superseded_by_attempt": None,
                "valid": True,
                "return_code": 0,
                "elapsed_s": (20 if suite == "java" else 10) + repeat_or_zero(repeat) +
                             (0 if mode == "direct" else 1 if mode == "cold" else 2) +
                             analyzer.STAGES.index(stage) * .1,
                "driver_elapsed_s": 1.0,
                "total_tests": test_counts["total"],
                "passed_tests": test_counts["passed"],
                "failed_tests": test_counts["failed"],
                "skipped_tests": test_counts["skipped"],
                "cacheable_calls": cacheable,
                "cache_hits": hits,
                "cache_misses": misses,
                "uncacheable_calls": uncacheable,
                "cache_validation": {
                    "applicable": stage != "before-cache", "passed": True,
                    "cacheable_calls": cacheable, "hits": hits, "misses": misses,
                    "uncacheable_calls": uncacheable,
                },
                **stages[stage],
                "compression": compression,
                "test_identity_sha256": identities[suite],
                "test_identity": test_ids[suite],
                "workload_identity_match": True,
                "fixture_fingerprint_sha256": fixture_digest,
                "compile_tasks_clean": True,
                "failure_names": [],
            }
            results.append(row)
    return {
        "schema_version": 1,
        "plan": plan,
        "identity_preflight": {
            "passed": True,
            "reference_test_ids": references,
            "warmup_test_identities": warmup_identities,
            "mismatches": [],
        },
        "results": results,
    }


def repeat_or_zero(repeat):
    return 0 if repeat is None else repeat


class AnalyzeDeadlineTest(unittest.TestCase):
    def setUp(self):
        self.source = Path("fresh-results.json")
        self.manifest = fixture_manifest()

    def test_complete_matrix_emits_distributions_and_paired_metrics(self):
        result = analyzer.analyze_cohort([self.source], [self.manifest])
        self.assertEqual(len(result["summaries"]), 20)
        self.assertEqual(len(result["comparisons"]), 26)
        self.assertEqual(len(result["combined_two_suite_time"]), 10)
        self.assertEqual(result["combined_two_suite_time"][0]["n"], 6)
        self.assertIn("not production weighting", result["combined_two_suite_time"][0]["label"])
        self.assertTrue(all(len(item["pairs"]) == 6 for item in result["comparisons"]))

    def test_nested_uncacheable_counter_fills_only_an_absent_root_field(self):
        for row in self.manifest["results"]:
            row.pop("uncacheable_calls")
        result = analyzer.analyze_cohort([self.source], [self.manifest])
        self.assertEqual(len(result["summaries"]), 20)

    def test_root_uncacheable_counter_must_match_nested_validation(self):
        self.manifest["results"][0]["uncacheable_calls"] = 1
        with self.assertRaisesRegex(analyzer.AnalysisError, "cache counters disagree"):
            analyzer.analyze_cohort([self.source], [self.manifest])

    def test_missing_or_non_integer_uncacheable_proof_is_rejected(self):
        row = self.manifest["results"][0]
        row.pop("uncacheable_calls")
        row["cache_validation"].pop("uncacheable_calls")
        with self.assertRaisesRegex(analyzer.AnalysisError, "invalid nested uncacheable_calls"):
            analyzer.analyze_cohort([self.source], [self.manifest])

        row["cache_validation"]["uncacheable_calls"] = False
        with self.assertRaisesRegex(analyzer.AnalysisError, "invalid nested uncacheable_calls"):
            analyzer.analyze_cohort([self.source], [self.manifest])

    def test_four_round_plan_requires_exactly_four_complete_rounds(self):
        self.manifest["plan"]["repeat_count"] = 4
        for cell in self.manifest["plan"]["cells"]:
            cell["repeat_count"] = 4
        self.manifest["results"] = [row for row in self.manifest["results"]
                                    if not row["measured"] or row["repeat"] <= 4]
        result = analyzer.analyze_cohort([self.source], [self.manifest])
        self.assertEqual(result["repeat_count"], 4)
        self.assertTrue(all(len(item["pairs"]) == 4 for item in result["comparisons"]))
        self.manifest["results"].pop()
        with self.assertRaises(analyzer.AnalysisError):
            analyzer.analyze_cohort([self.source], [self.manifest])

    def test_incomplete_measured_cell_is_rejected(self):
        self.manifest["results"] = [
            row for row in self.manifest["results"]
            if row["cell_id"] != "java/direct/before-cache/repeat-06"
        ]
        with self.assertRaisesRegex(analyzer.AnalysisError, "incomplete"):
            analyzer.analyze_cohort([self.source], [self.manifest])

    def test_failed_row_is_rejected(self):
        row = next(row for row in self.manifest["results"] if row["cell_id"].endswith("repeat-03"))
        row["valid"] = False
        with self.assertRaisesRegex(analyzer.AnalysisError, "failed/invalid"):
            analyzer.analyze_cohort([self.source], [self.manifest])

    def test_duplicate_row_is_rejected(self):
        duplicate = copy.deepcopy(self.manifest["results"][1])
        self.manifest["results"].append(duplicate)
        with self.assertRaisesRegex(analyzer.AnalysisError, "duplicate"):
            analyzer.analyze_cohort([self.source], [self.manifest])

    def test_wrong_suite_row_is_rejected(self):
        row = self.manifest["results"][0]
        row["suite"] = "rust"
        with self.assertRaisesRegex(analyzer.AnalysisError, "unexpected suite/mode/stage"):
            analyzer.analyze_cohort([self.source], [self.manifest])

    def test_disjoint_mode_manifests_join_but_are_not_pooled_as_cohorts(self):
        manifests = []
        sources = []
        for mode in analyzer.MODES:
            shard = copy.deepcopy(self.manifest)
            shard["results"] = [row for row in shard["results"] if row["mode"] == mode]
            shard["plan"]["cells"] = [cell for cell in shard["plan"]["cells"] if cell["mode"] == mode]
            shard["plan"]["cache_states"] = {mode: analyzer.EXPECTED_CACHE_STATES[mode]}
            used_stages = {cell["stage"] for cell in shard["plan"]["cells"]}
            shard["plan"]["stages"] = {key: value for key, value in shard["plan"]["stages"].items() if key in used_stages}
            manifests.append(shard)
            sources.append(Path(f"{mode}.json"))
        result = analyzer.analyze_cohort(sources, manifests)
        self.assertEqual(len(result["summaries"]), 20)

    def test_repaired_attempt_is_selected_and_failed_attempt_stays_audit_only(self):
        selected = next(
            row for row in self.manifest["results"]
            if row["cell_id"] == "clava-js/direct/protobuf/repeat-03"
        )
        selected["attempt"] = 2
        selected["elapsed_s"] = 777.0
        failed = copy.deepcopy(selected)
        failed.update({
            "attempt": 1,
            "selected": False,
            "superseded_by_attempt": 2,
            "valid": False,
            "return_code": 1,
            "failure_names": ["synthetic failure"],
            "elapsed_s": 99999.0,
        })
        self.manifest["results"].append(failed)

        result = analyzer.analyze_cohort([self.source], [self.manifest])
        comparison = next(
            item for item in result["comparisons"]
            if item["kind"] == "vs_text_same_mode" and item["suite"] == "clava-js"
            and item["mode"] == "direct" and item["stage"] == "protobuf"
        )
        selected_pair = next(pair for pair in comparison["pairs"] if pair["repeat"] == 3)
        self.assertEqual(selected_pair["candidate_s"], 777.0)
        self.assertEqual(len(result["attempt_audit"]), 1)
        self.assertNotIn("elapsed_s", result["attempt_audit"][0])
        self.assertEqual(result["attempt_audit"][0]["superseded_by_attempt"], 2)

    def test_retry_without_prior_attempt_linkage_is_rejected(self):
        selected = next(
            row for row in self.manifest["results"]
            if row["cell_id"] == "clava-js/direct/protobuf/repeat-03"
        )
        selected["attempt"] = 2
        with self.assertRaisesRegex(analyzer.AnalysisError, "not contiguous"):
            analyzer.analyze_cohort([self.source], [self.manifest])

    def test_cli_writes_machine_readable_json_and_csv(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            input_path = root / "results.json"
            output_path = root / "analysis.json"
            input_path.write_text(json.dumps(self.manifest), encoding="utf-8")
            self.assertEqual(analyzer.main(["--input", str(input_path), "--output", str(output_path)]), 0)
            payload = json.loads(output_path.read_text(encoding="utf-8"))
            self.assertEqual(len(payload["cohorts"][0]["summaries"]), 20)
            with (root / "analysis.csv").open(encoding="utf-8", newline="") as csv_file:
                rows = list(csv.DictReader(csv_file))
            self.assertTrue(any(row["record_type"] == "paired_delta" for row in rows))
            self.assertTrue(any(row["record_type"] == "combined_two_suite_summary" for row in rows))


if __name__ == "__main__":
    unittest.main()
