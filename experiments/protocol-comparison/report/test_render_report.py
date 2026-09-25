"""Regression checks for readable chart axes."""

import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

import render_report


class AxisTickLabelsTest(unittest.TestCase):
    def test_zoomed_axis_uses_regular_fractional_seconds(self):
        ticks = render_report.time_axis_ticks(43.1, 46.0)
        labels = [label for _, label in ticks]
        self.assertEqual(len(labels), len(set(labels)))
        self.assertEqual(labels, ["43.5s", "44.0s", "44.5s", "45.0s", "45.5s", "46.0s"])

    def test_narrow_axis_adds_more_precision(self):
        labels = [label for _, label in render_report.time_axis_ticks(43.700, 43.704)]
        self.assertEqual(len(labels), len(set(labels)))
        self.assertEqual(labels[0], "43.700s")
        self.assertEqual(labels[-1], "43.704s")

    def test_wide_axis_uses_even_five_second_steps(self):
        labels = [label for _, label in render_report.time_axis_ticks(38.3, 58.9)]
        self.assertEqual(labels, ["40s", "45s", "50s", "55s"])

    def test_same_revision_candle_has_distinct_tick_labels(self):
        text_times = [43.37, 43.79, 43.57, 43.64, 43.94, 45.50]
        proto_times = [44.69, 43.51, 43.61, 44.34, 43.09, 43.74]
        rows = [
            {"suite": "clava-js", "stage": stage, "measured": True,
             "repeat": repeat, "elapsed_s": elapsed}
            for stage, times in (("ab-text", text_times), ("ab-protobuf", proto_times))
            for repeat, elapsed in enumerate(times, 1)
        ]
        svg = ET.fromstring(render_report.ab_candle_svg({"results": rows}, "clava-js"))
        labels = [node.text for node in svg.findall(".//text[@class='axis-text']")]
        self.assertGreaterEqual(len(labels), 4)
        self.assertEqual(len(labels), len(set(labels)))
        seconds = [float(label[:-1]) for label in labels]
        steps = [round(b - a, 6) for a, b in zip(seconds, seconds[1:])]
        self.assertEqual(len(set(steps)), 1)

    def test_java_gc_diagnostic_matches_ab_revision_and_shows_control(self):
        path = Path(__file__).resolve().parents[1] / "results/java-gc-diagnostic-20260925/summary.json"
        ab = {"sources": {
            "clava": {"revision": "a1a8d73c9f0f0e1f4d2d58fabc786cc2d9318ea0"},
            "native": {"revision": "16e97e1ee1f14b8f5751f48143b953b1c89a9a1a"},
        }}
        profile = render_report.load_gc_result(path, ab)
        html = render_report.gc_diagnostic_html(profile)
        self.assertIn("432 full collections", html)
        self.assertIn("31.32s vs 31.51s", html)
        self.assertIn("12.70s", html)
        self.assertIn("Why each Protobuf collection took longer", html)
        self.assertIn("No JIT, 256 MiB heap", html)
        self.assertIn("6.8 ms", html)
        self.assertNotIn("do not yet tell us", html)

        ab["sources"]["native"]["revision"] = "wrong"
        with self.assertRaisesRegex(ValueError, "native revision differs"):
            render_report.load_gc_result(path, ab)


if __name__ == "__main__":
    unittest.main()
