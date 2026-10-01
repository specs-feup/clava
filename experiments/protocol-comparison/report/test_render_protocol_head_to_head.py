import unittest
import re
import xml.etree.ElementTree as ET

from render_protocol_head_to_head import paired_data, render_html


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
