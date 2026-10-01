import unittest
import base64
import csv
import io
import re
import xml.etree.ElementTree as ET

from render_decision import candle, comparison, csv_viewer, diagnostic_chart, measurements_download, render, validate_matrix, validate_uninstrumented_headlines
from render_report import SUITES, STAGE_ORDER, MODE_ORDER


def complete_rows():
    return [dict(suite=suite, stage=stage, mode=mode, measured=True,
                 repeat=repeat, valid=True, return_code=0, elapsed_s=30+repeat,
                 total_tests=spec["tests"], passed_tests=spec["passed"],
                 failed_tests=0, skipped_tests=spec["skipped"])
            for suite, spec in SUITES.items() for mode in MODE_ORDER
            for stage in STAGE_ORDER if mode == "direct" or stage != "before-cache"
            for repeat in range(1, 7)]


class DecisionReportTest(unittest.TestCase):
    def test_head_to_head_evidence_precedes_text_relative_comparisons(self):
        analysis = dict(recommendation="Choose one format", reasons=[],
                        discrepancy=[dict(title="Cause", text="Evidence")] * 2,
                        tradeoffs=[], method="Matched workloads", limitations="Four pairs",
                        evidence=[], decision_visuals_html='<section id="direct-choice"></section>')
        output = render([dict(plan=dict(repeat_count=6), results=complete_rows())], {}, analysis)
        self.assertLess(output.index('id="direct-choice"'), output.index('What changes the runtime?'))

    def test_diagnostic_uses_paired_values_and_readable_units(self):
        output = diagnostic_chart({"title": "AST & validation", "caption": "Paired changes",
            "rows": [{"label": "AST construction", "values": [1.1, 1.0]},
                     {"label": "Syntax checks", "values": [-1.5, -1.4]}]})
        self.assertIn("AST &amp; validation", output)
        self.assertIn("+1.05s", output)
        self.assertIn("-1.45s", output)
        self.assertEqual(output.count("<circle"), 4)
        ET.fromstring(re.search(r"(<svg.*</svg>)", output).group(1))

    def test_uninstrumented_headline_requires_actual_worker_proof(self):
        rows = complete_rows()
        with self.assertRaises(ValueError):
            validate_uninstrumented_headlines(rows)
        for row in rows:
            if row["suite"] == "java":
                row.update(agent="off", actual_test_executor_args=["java", "-Xmx512m", "GradleWorkerMain"])
        validate_uninstrumented_headlines(rows)
        java = next(row for row in rows if row["suite"] == "java")
        java["actual_test_executor_args"].append("-javaagent:coverage.jar")
        with self.assertRaises(ValueError):
            validate_uninstrumented_headlines(rows)

    def test_mixed_paired_results_are_not_coloured_as_consistent_wins(self):
        rows = complete_rows()
        for row in rows:
            if row["stage"] == "protobuf":
                row["elapsed_s"] *= 0.9 if row["repeat"] <= 3 else 1.01
        rendered = comparison(rows)
        self.assertIn("3/6 faster", rendered)
        self.assertIn('class="delta muted"', rendered)

    def test_csv_viewer_works_without_download_or_network_permissions(self):
        viewer = csv_viewer()
        self.assertIn("event.preventDefault()", viewer)
        self.assertIn("text.select()", viewer)
        self.assertIn("new TextDecoder()", viewer)
        self.assertNotIn("fetch(", viewer)
        self.assertNotIn("clipboard", viewer)

    def test_four_round_matrix_is_explicit_and_complete(self):
        rows = [row for row in complete_rows() if row["repeat"] <= 4]
        validate_matrix(rows, 4)
        self.assertIn("80 accepted", measurements_download(rows))
        rows.pop()
        with self.assertRaises(ValueError):
            validate_matrix(rows, 4)

    def test_download_has_only_accepted_timings_and_no_local_paths(self):
        rows = complete_rows()
        rows[0]["command"] = "/private/local/file"
        rows.append(dict(rows[0], selected=False, elapsed_s=10000))
        link = measurements_download(rows)
        payload = re.search(r'base64,([^\"]+)', link).group(1)
        decoded = base64.b64decode(payload).decode()
        records = list(csv.DictReader(io.StringIO(decoded)))
        self.assertEqual(len(records), 120)
        self.assertNotIn("/private/local/file", decoded)
        self.assertNotIn("10000", decoded)

    def test_requires_complete_valid_matrix(self):
        rows = complete_rows()
        validate_matrix(rows)
        rows[0]["failed_tests"] = 1
        with self.assertRaises(ValueError):
            validate_matrix(rows)

    def test_rejects_duplicate_repeats(self):
        rows = complete_rows()
        rows[0]["repeat"] = rows[1]["repeat"]
        with self.assertRaises(ValueError):
            validate_matrix(rows)

    def test_superseded_valid_attempt_does_not_enter_chart(self):
        rows = complete_rows()
        old = dict(rows[0], selected=False, elapsed_s=10000)
        rows.append(old)
        validate_matrix(rows)
        self.assertNotIn("10000", candle("clava-js", rows))

    def test_chart_has_every_run_and_shared_axis(self):
        root = ET.fromstring(candle("java", complete_rows()))
        self.assertEqual(len(root.findall("circle")), 60)
        ticks = [n for n in root.findall("text") if n.get("class") == "tick"]
        self.assertEqual(len(ticks), 3)
        self.assertEqual(len(set(n.text for n in ticks)), 3)
        height = float(root.get("viewBox").split()[-1])
        for n in root.findall("text"):
            self.assertLess(float(n.get("y")), height)
        self.assertIn("Non-zero axis", ''.join(root.itertext()))


if __name__ == "__main__":
    unittest.main()
