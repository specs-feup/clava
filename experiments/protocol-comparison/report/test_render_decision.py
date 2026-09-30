import unittest
import base64
import csv
import io
import re
import xml.etree.ElementTree as ET

from render_decision import candle, measurements_download, validate_matrix
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
