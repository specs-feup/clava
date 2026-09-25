"""Regression checks for readable chart axes."""

import unittest
import xml.etree.ElementTree as ET

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


if __name__ == "__main__":
    unittest.main()
