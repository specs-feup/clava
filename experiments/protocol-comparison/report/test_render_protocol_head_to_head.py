import unittest
import re
import xml.etree.ElementTree as ET

from render_protocol_head_to_head import paired_data, render_html, js_svg


def _row(suite, stage, mode, repeat, wall, junit=None, *, fast_syntax=False):
    is_java = suite == "java"
    result = {
        "suite": suite,
        "stage": stage,
        "mode": mode,
        "repeat": repeat,
        "selected": True,
        "measured": True,
        "valid": True,
        "return_code": 0,
        "total_tests": 116 if is_java else 164,
        "passed_tests": 116 if is_java else 158,
        "failed_tests": 0,
        "skipped_tests": 0 if is_java else 6,
        "workload_identity_match": True,
        "test_identity_sha256": "same-test-set",
        "fixture_fingerprint_sha256": "same-fixtures",
        "elapsed_s": wall,
    }
    if is_java:
        result.update({
            "agent": "off",
            "no_explicit_gc_flags": True,
            "test_task_executed": True,
            "actual_test_executor_args": ["java", "-Xmx512m", "GradleWorkerMain"],
            "report_tasks_skipped": {
                "jacocoTestReport": True,
                "jacocoTestCoverageVerification": True,
            },
            "junit_aggregate_s": junit,
        })
    else:
        result["fast_syntax"] = fast_syntax
    return result


def _matrices():
    java_rows = []
    for mode in ("direct", "cold", "warm"):
        for stage in ("ccache-text", "protobuf", "flatbuffers"):
            for repeat in range(1, 5):
                if mode == "warm" and stage == "protobuf":
                    wall = (1, 2, 99, 100)[repeat - 1]
                elif mode == "warm" and stage == "flatbuffers":
                    wall = (100, 99, 1, 2)[repeat - 1]
                else:
                    wall = 20 + repeat
                java_rows.append(_row("java", stage, mode, repeat, wall, wall / 2))
    for repeat in range(1, 5):
        java_rows.append(_row("java", "before-cache", "direct", repeat, 30 + repeat, 15 + repeat / 2))

    js_rows = []
    normalized_rows = []
    for stage in ("protobuf", "flatbuffers"):
        for repeat in range(1, 5):
            js_rows.append(_row("clava-js", stage, "warm", repeat, 30 + repeat))
            normalized_rows.append(_row("clava-js", stage, "warm", repeat, 31 + repeat, fast_syntax=True))
    js_matrix = {"plan": {"suites": ["clava-js"], "repeat_count": 4}, "results": js_rows}
    java_matrix = {"schema_version": 1,
                   "plan": {"suites": ["java"], "repeat_count": 4}, "results": java_rows}
    return js_matrix, java_matrix, normalized_rows


class PairedProtocolReportTest(unittest.TestCase):
    def test_js_one_sided_deltas_do_not_waste_half_the_axis(self):
        root = ET.fromstring(js_svg({"direct": {"values": [1, 1.1, 1.2, 1.3], "median_delta_s": 1.15}}))
        ticks = [node for node in root if node.tag == "text" and node.get("class") == "axis"]
        self.assertEqual(ticks[0].text, "0 s")
        self.assertEqual(ticks[0].get("x"), "20.0")
        self.assertFalse(any(node.text.startswith("-") for node in ticks))

    def test_js_mixed_deltas_scale_each_side_to_observed_extents(self):
        root = ET.fromstring(js_svg({"cold": {"values": [-2.01, -.36, .11, -.27], "median_delta_s": -.315},
                                    "warm": {"values": [.58, .32, -.09, -.17], "median_delta_s": .115}}))
        ticks = [node.text for node in root if node.tag == "text" and node.get("class") == "axis"]
        self.assertEqual(ticks, ["-2.5 s", "0 s", "+1.0 s"])

    def test_matched_matrix_compares_every_js_cache_state_without_historical_rows(self):
        js, java, _ = _matrices()
        js["results"] = [_row("clava-js", stage, mode, repeat, 30 + repeat,
                              fast_syntax=True)
                         for mode in ("direct", "cold", "warm")
                         for stage in ("before-cache", "ccache-text", "protobuf", "flatbuffers")
                         if mode == "direct" or stage != "before-cache"
                         for repeat in range(1, 5)]
        for row in java["results"]:
            row["fast_syntax"] = True
        data = paired_data(js, java, matched_validation=True)
        self.assertEqual(list(data["js"]), ["direct", "cold", "warm"])
        fragment = render_html(data)
        self.assertIn("Clava-JS · all cache states · same fast validation", fragment)
        self.assertNotIn("Original implementations", fragment)
        self.assertIn("Whole command", fragment)
        self.assertIn("Test bodies", fragment)
        js["results"][0]["fast_syntax"] = False
        with self.assertRaisesRegex(ValueError, "fast_syntax=true"):
            paired_data(js, java, matched_validation=True)

    def test_matched_matrix_refuses_a_historical_control(self):
        js, java, normalized = _matrices()
        with self.assertRaisesRegex(ValueError, "historical"):
            paired_data(js, java, normalized, matched_validation=True)

    def test_java_deltas_are_median_of_matched_round_differences(self):
        js, java, _ = _matrices()
        data = paired_data(js, java)
        warm_wall = next(row for row in data["java"]
                         if row["mode"] == "warm" and row["metric"] == "elapsed_s")
        self.assertEqual(warm_wall["values"], [99, 97, -98, -98])
        self.assertEqual(warm_wall["median_delta_s"], -0.5)
        # The separate stage medians are equal; their difference is not the paired median.
        self.assertNotEqual(warm_wall["median_delta_s"], 0)

    def test_fragment_keeps_suites_and_optional_normalized_js_separate(self):
        js, java, normalized = _matrices()
        data = paired_data(js, java, normalized)
        fragment = render_html(data)
        self.assertIn("Java · no coverage agent · 116 tests", fragment)
        self.assertIn("Clava-JS · warm", fragment)
        self.assertIn("Both use fast validation", fragment)
        self.assertIn("Outside-JUnit paired residuals", fragment)
        self.assertIn("font-size:15px", fragment)
        roots = [ET.fromstring(svg) for svg in re.findall(r"<svg .*?</svg>", fragment)]
        self.assertEqual(len(roots), 2)
        self.assertTrue(all(root.get("viewBox", "").startswith("0 0 360 ") for root in roots))
        java_metrics = [node.text for node in roots[0].findall("text") if node.get("class") == "metric"]
        self.assertEqual(java_metrics, ["Wall", "JUnit"] * 3)

    def test_normalized_js_requires_an_explicit_fast_syntax_flag(self):
        js, java, normalized = _matrices()
        normalized[0]["fast_syntax"] = False
        with self.assertRaisesRegex(ValueError, "fast_syntax=true"):
            paired_data(js, java, normalized)

    def test_java_headline_rejects_agent_on_rows(self):
        js, java, _ = _matrices()
        java["results"][0]["agent"] = "on"
        with self.assertRaisesRegex(ValueError, "JaCoCo-off"):
            paired_data(js, java)


if __name__ == "__main__":
    unittest.main()
