from __future__ import annotations

import copy
import base64
import csv
import html
import io
import json
import re
import sys
import tempfile
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
                        observation = {**schedule}
                        observation.pop("group_id")  # Raw runner rows join by stable event_id.
                        observation.update(valid=True, app_returned_null=False, elapsed_ms=elapsed_ms)
                        observations.append(observation)
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

    def test_timed_null_app_is_valid_only_for_scheduled_syntax_only_group(self):
        schedules = copy.deepcopy(self.schedules)
        schedules[0]["parser_config"] = {"syntax_only": True}
        observations = copy.deepcopy(self.observations)
        observations[0]["app_returned_null"] = True
        summary = grouped.analyze_grouped_runs(
            schedules, observations, expected_group_counts=self.counts
        )
        self.assertEqual(summary["observation_row_count"], len(self.observations))

        schedules[0]["parser_config"] = {"syntax_only": False}
        with self.assertRaisesRegex(grouped.AnalysisError, "invalid': 1"):
            grouped.analyze_grouped_runs(
                schedules, observations, expected_group_counts=self.counts
            )

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

    def test_observation_event_id_is_strict_and_group_id_is_checked_when_emitted(self):
        observations = copy.deepcopy(self.observations)
        observations[0]["group_id"] = "wrong-group"
        with self.assertRaisesRegex(grouped.AnalysisError, "group_id differs"):
            grouped.analyze_grouped_runs(
                self.schedules, observations, expected_group_counts=self.counts
            )

        observations = copy.deepcopy(self.observations)
        observations[0]["event_id"] = "wrong-event"
        with self.assertRaisesRegex(grouped.AnalysisError, "event_id differs"):
            grouped.analyze_grouped_runs(
                self.schedules, observations, expected_group_counts=self.counts
            )

    def test_observation_provenance_hashes_are_required_and_matched(self):
        for field in ("source_sha256", "args_sha256", "options_sha256"):
            with self.subTest(field=field, case="mismatch"):
                observations = copy.deepcopy(self.observations)
                observations[0][field] = "e" * 64
                with self.assertRaisesRegex(grouped.AnalysisError, f"{field} differs"):
                    grouped.analyze_grouped_runs(
                        self.schedules, observations, expected_group_counts=self.counts
                    )

            with self.subTest(field=field, case="missing"):
                observations = copy.deepcopy(self.observations)
                observations[0].pop(field)
                with self.assertRaisesRegex(grouped.AnalysisError, f"{field} differs"):
                    grouped.analyze_grouped_runs(
                        self.schedules, observations, expected_group_counts=self.counts
                    )

    def test_loader_accepts_runner_measure_directory(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            schedules_by_file = {}
            observations_by_file = {}
            for schedule in self.schedules:
                name = (
                    f"measure-{schedule['suite']}-{schedule['protocol']}-"
                    f"{schedule['cache_mode']}-r{schedule['repeat']:02d}.jsonl"
                )
                schedules_by_file.setdefault(name, []).append(schedule)
            for observation in self.observations:
                name = (
                    f"measure-{observation['suite']}-{observation['protocol']}-"
                    f"{observation['cache_mode']}-r{observation['repeat']:02d}.jsonl"
                )
                observations_by_file.setdefault(name, []).append(observation)
            for name, rows in schedules_by_file.items():
                path = root / "schedules" / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("".join(json.dumps(row) + "\n" for row in rows))
            for name, rows in observations_by_file.items():
                path = root / "measure" / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("".join(json.dumps(row) + "\n" for row in rows))

            summary = grouped.load_grouped_analysis(
                root, expected_group_counts=self.counts
            )
        self.assertEqual(summary["group_counts"], self.counts)

    def test_rendered_fragment_has_mobile_svg_and_sanitized_download_links(self):
        summary = grouped.analyze_grouped_runs(
            self.schedules, self.observations, expected_group_counts=self.counts
        )
        fragment = grouped.render_reviewed_visuals_html(
            summary, expected_group_counts=self.counts
        )
        self.assertIn("@media(max-width:560px)", fragment)
        self.assertIn("log scale", fragment)
        self.assertIn("aria-label=", fragment)
        self.assertIn("download=\"grouped-ab-paired-rounds.csv\"", fragment)
        self.assertIn("data:text/csv;charset=utf-8;base64,", fragment)
        self.assertNotIn("/home/", fragment)
        self.assertNotIn("internal-group", fragment)
        csv_links = {
            filename: base64.b64decode(blob).decode("utf-8")
            for filename, blob in re.findall(
                r'download="([^"]+)" href="data:text/csv;charset=utf-8;base64,([^"]+)"',
                fragment,
            )
        }
        self.assertEqual(len(csv_links), 4)
        group_csv = csv_links["grouped-ab-group-medians-and-ratios.csv"]
        self.assertIn("source_sha256,source_file_sha256s,args_sha256,options_sha256", group_csv)
        self.assertIn("fixture-1.c", group_csv)
        self.assertIn("d" * 64, group_csv)
        self.assertNotIn("/private/", group_csv)
        paired_csv = csv_links["grouped-ab-paired-rounds.csv"]
        self.assertEqual(
            paired_csv.splitlines()[0],
            "suite,input_id,source_labels,cache_mode,repeat,text_ms,protobuf_ms,delta_ms,relative_pct",
        )
        self.assertNotIn("source_sha256", paired_csv.splitlines()[0])
        metadata_rows = list(csv.DictReader(io.StringIO(group_csv)))
        paired_rows = list(csv.DictReader(io.StringIO(paired_csv)))
        metadata_keys = {(row["suite"], row["input_id"]) for row in metadata_rows}
        pair_keys = {
            (row["suite"], row["input_id"], row["cache_mode"], row["repeat"])
            for row in paired_rows
        }
        self.assertTrue(all((row["suite"], row["input_id"]) in metadata_keys
                            for row in paired_rows))
        self.assertEqual(len(pair_keys), len(self.observations) // 2)
        self.assertTrue(all(row["text_ms"] and row["protobuf_ms"] for row in paired_rows))

        main, details = fragment.split("<details>", 1)
        self.assertEqual(main.count('<svg class="ga-main-svg"'), 2)
        self.assertEqual(main.count('viewBox="0 0 360 270"'), 2)
        self.assertIn(".ga-main-label,section.grouped-ab .ga-main-tick", fragment)
        self.assertIn("font-size:15px", fragment)
        self.assertNotIn("overflow-x", fragment)
        self.assertNotIn("nowrap", fragment)
        self.assertEqual(main.count("<tr>"), 7)  # header plus six scope/cache medians
        self.assertEqual(details.count('<svg class="ga-detail-svg"'), 6)
        prose = re.sub(r"<style.*?</style>|<svg.*?</svg>", " ", main, flags=re.DOTALL)
        prose = html.unescape(re.sub(r"<[^>]*>", " ", prose))
        self.assertLessEqual(len(re.findall(r"\b[\w×−%]+\b", prose)), 150)
        self.assertIn("format-only control", prose)
        self.assertIn("same build", prose.lower())
        self.assertIn("130 syntax-only js groups return no app", prose.lower())
        self.assertIn("not file, times", prose)
        self.assertIn("pointeeTypeAsWritten", details)
        self.assertIn("CXXPseudoDestructorExpr", details)
        self.assertIn("node/reference-graph identity", details)
        self.assertIn("Join paired-round timings", details)
        self.assertIn("suite", details)
        self.assertIn("input_id", details)
        ticks = re.findall(
            r'<text class="ga-main-tick" x="([0-9.]+)" y="23"[^>]*>([^<]+)</text>', main
        )
        self.assertEqual(len(ticks), 6)
        for x_value, label in ticks:
            self.assertNotRegex(label, r"\d[eE][+-]\d+")
            center, half_width = float(x_value), len(html.unescape(label)) * 4.5
            self.assertGreaterEqual(center - half_width, 0)
            self.assertLessEqual(center + half_width, 360)
        self.assertLess(len(fragment.encode("utf-8")), 4 * 1024 * 1024)

        with tempfile.TemporaryDirectory() as temporary:
            exports = grouped.write_grouped_analysis_exports(
                summary, Path(temporary), expected_group_counts=self.counts
            )
            expected_rows = {
                "grouped-ab-round-totals.csv": 72,
                "grouped-ab-paired-round-totals.csv": 36,
                "grouped-ab-group-medians-and-ratios.csv": 6,
                "grouped-ab-paired-rounds.csv": 36,
            }
            for filename, row_count in expected_rows.items():
                content = exports[filename].read_text(encoding="utf-8")
                self.assertNotIn("/private/", content)
                self.assertEqual(sum(1 for _ in csv.reader(io.StringIO(content))) - 1, row_count)
                self.assertEqual(content, csv_links[filename])
            self.assertTrue(exports["summary"].is_file())
            self.assertTrue(exports["html"].is_file())
            self.assertLess(exports["html"].stat().st_size, 4 * 1024 * 1024)


if __name__ == "__main__":
    unittest.main()
