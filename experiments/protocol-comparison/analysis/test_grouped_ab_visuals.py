from __future__ import annotations

import copy
import base64
import re
import sys
from pathlib import Path
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
import grouped_ab_visuals as grouped


def fixture_rows():
    ids_by_suite = {
        "clava-js": ["js-group-0001", "js-group-0002"],
        "java": ["java-group-0001"],
    }
    schedules = []
    observations = []
    for suite, input_ids in ids_by_suite.items():
        for protocol in grouped.PROTOCOLS:
            for cache_mode in grouped.CACHE_MODES:
                for repeat in grouped.REPEATS:
                    for index, input_id in enumerate(input_ids, start=1):
                        schedule = {
                            "suite": suite,
                            "input_id": input_id,
                            "event_id": f"event-{suite}-{index}",
                            "group_id": f"internal-group-{suite}-{index}",
                            "repeat": repeat,
                            "phase": "measure",
                            "protocol": protocol,
                            "cache_mode": cache_mode,
                            "compression_policy": "raw_control",
                            "source_label": f"src/fixture-{index}.c",
                            "source_paths": [f"/private/corpus/fixture-{index}.c"],
                            "source_files": [{
                                "original_path": f"/private/corpus/fixture-{index}.c",
                                "sha256": "d" * 64,
                            }],
                            "source_count": 1,
                            "source_sha256": "a" * 64,
                            "args_sha256": "b" * 64,
                            "options_sha256": "c" * 64,
                        }
                        schedules.append(schedule)
                        text_ms = 10.0 + index + repeat
                        multiplier = 0.8 if cache_mode == "warm" else 1.1
                        elapsed_ms = text_ms * multiplier
                        if protocol == "protobuf":
                            elapsed_ms *= 1.2
                        observations.append({
                            **schedule,
                            "valid": True,
                            "app_null": False,
                            "elapsed_ms": elapsed_ms,
                        })
    return schedules, observations, {suite: len(ids) for suite, ids in ids_by_suite.items()}


class GroupedABVisualsTest(unittest.TestCase):
    def setUp(self):
        self.schedules, self.observations, self.counts = fixture_rows()

    def test_complete_matrix_pairs_rounds_without_pooling_them_as_groups(self):
        summary = grouped.analyze_grouped_runs(
            self.schedules, self.observations, expected_group_counts=self.counts
        )
        self.assertEqual(summary["group_counts"], self.counts)
        self.assertEqual(summary["observation_row_count"], 3 * 2 * 2 * 6)
        self.assertEqual(len(summary["group_summaries"]), 3 * 2)
        self.assertTrue(all(row["n_rounds"] == 6 for row in summary["group_summaries"]))
        self.assertEqual(len(summary["group_rounds"]), 3 * 2 * 6)
        self.assertEqual(len(summary["round_totals"]), 3 * 2 * 6 * 2)
        self.assertEqual(len(summary["paired_round_totals"]), 3 * 2 * 6)
        self.assertAlmostEqual(summary["group_summaries"][0]["proto_text_ratio"], 1.2)

    def test_missing_selected_pair_is_rejected(self):
        observations = copy.deepcopy(self.observations)
        observations.pop(0)
        with self.assertRaisesRegex(grouped.AnalysisError, "complete matrix"):
            grouped.analyze_grouped_runs(
                self.schedules, observations, expected_group_counts=self.counts
            )

    def test_invalid_and_unselected_rows_are_excluded_then_completeness_is_enforced(self):
        observations = copy.deepcopy(self.observations)
        observations[0]["valid"] = False
        observations[1]["selected"] = False
        with self.assertRaises(grouped.AnalysisError) as caught:
            grouped.analyze_grouped_runs(
                self.schedules, observations, expected_group_counts=self.counts
            )
        self.assertIn("'invalid': 1", str(caught.exception))
        self.assertIn("'unselected': 1", str(caught.exception))

    def test_non_measure_and_untimed_rows_do_not_enter_statistics(self):
        observations = copy.deepcopy(self.observations)
        observations.append({"phase": "fidelity", "valid": True})
        observations.append({
            "suite": "java", "input_id": "java-group-0001", "phase": "measure",
            "protocol": "text", "cache_mode": "direct", "repeat": 1,
            "valid": True,
        })
        summary = grouped.analyze_grouped_runs(
            self.schedules, observations, expected_group_counts=self.counts
        )
        self.assertEqual(summary["excluded_rows"], {
            "non_measure": 1, "unselected": 0, "invalid": 0, "untimed": 1,
        })
        self.assertEqual(summary["observation_row_count"], len(self.observations))

    def test_schedule_id_drift_is_rejected(self):
        schedules = copy.deepcopy(self.schedules)
        schedules[0]["input_id"] = "js-group-9999"
        with self.assertRaisesRegex(grouped.AnalysisError, "event/group identity is not unique|stable IDs"):
            grouped.analyze_grouped_runs(
                schedules, self.observations, expected_group_counts=self.counts
            )

    def test_rendered_fragment_has_mobile_svg_and_sanitized_download_links(self):
        summary = grouped.analyze_grouped_runs(
            self.schedules, self.observations, expected_group_counts=self.counts
        )
        fragment = grouped.render_reviewed_visuals_html(
            summary, expected_group_counts=self.counts
        )
        self.assertIn("@media(max-width:700px)", fragment)
        self.assertIn("log scale", fragment)
        self.assertIn("aria-label=", fragment)
        self.assertIn("download=\"grouped-ab-paired-rounds.csv\"", fragment)
        self.assertIn("data:text/csv;charset=utf-8;base64,", fragment)
        self.assertNotIn("/home/", fragment)
        self.assertNotIn("internal-group", fragment)
        csv_blobs = re.findall(r'href="data:text/csv;charset=utf-8;base64,([^"]+)"', fragment)
        decoded_csvs = [base64.b64decode(blob).decode("utf-8") for blob in csv_blobs]
        group_csv = next(content for content in decoded_csvs
                         if content.startswith("suite,input_id,source_labels"))
        self.assertIn("source_sha256,source_file_sha256s,args_sha256,options_sha256", group_csv)
        self.assertIn("fixture-1.c", group_csv)
        self.assertIn("d" * 64, group_csv)
        self.assertNotIn("/private/", group_csv)


if __name__ == "__main__":
    unittest.main()
