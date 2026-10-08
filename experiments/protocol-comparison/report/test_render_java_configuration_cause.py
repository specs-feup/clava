import re
import unittest
import xml.etree.ElementTree as ET

from render_java_configuration_cause import cause_data, csv_text, render_html


def _timeline_fixture():
    results = []
    phase_rows = []
    for round_id in (1, 2):
        for stage in ("ccache-text", "protobuf"):
            delta = 0.5 if round_id == 1 else 0.6
            is_pb = stage == "protobuf"
            results.append({
                "round": round_id,
                "stage": stage,
                "actual_mode": "per-command isolated cold",
                "valid": True,
                "return_code": 0,
                "total_tests": 116,
                "passed_tests": 116,
                "failed_tests": 0,
                "skipped_tests": 0,
                "wall_minus_junit_residual_s": 5.0 + (delta if is_pb else 0),
                "report_tasks_skipped": {
                    "jacocoTestReport": True,
                    "jacocoTestCoverageVerification": True,
                },
            })
            phase_rows.append({
                "round": str(round_id), "stage": stage,
                "phase": "project_configuration",
                "duration_s": str(1.5 + (delta if is_pb else 0)),
                "task_failure": "",
            })
    plan = {
        "schema_version": 1,
        "status": "complete",
        "diagnostic_only_not_headline": True,
        "actual_cache_mode": "per-command isolated cold",
        "expected_test_count": 116,
        "primary_matrix_sha256": "source-matrix",
        "worker_evidence_matrix_sha256": "worker-matrix",
        "worker_policy": {
            "JaCoCo_test_worker_agent": "off",
            "explicit_gc_override": False,
            "JaCoCo_report_tasks": "skipped",
        },
    }
    return {"plan": plan, "results": results}, phase_rows


def _config_fixture(timeline):
    plan = {
        "schema_version": 1,
        "status": "complete",
        "isolated_build_file_restored_to_original": True,
        "source_matrix_sha256": "source-matrix",
        "source_worker_evidence_sha256": "worker-matrix",
        "intervention": {
            "disabled": [
                "com.google.protobuf Gradle plugin 0.9.4 application",
                "custom schema-hash/protoc/bindings/hash task registrations and compileJava dependencies",
            ],
            "preserved": [
                "protobuf-java dependency declaration and dependency resolution context",
                "Java Test, JaCoCo, report-skip, and fixed-test filter configuration",
            ],
        },
        "control_invariants": [
            "protobuf-java dependency block retained byte-for-byte",
            "Java Test/task/JaCoCo configuration retained byte-for-byte",
        ],
    }
    rows = []
    pairs = []
    costs = (0.4, 0.5, 0.3, 0.6)
    for index, cost in enumerate(costs, 1):
        label = f"pair-{index:02d}"
        control = 1.0
        original = control + cost
        for arm, phase in (("original", original), ("control", control)):
            rows.append({
                "label": label,
                "arm": arm,
                "measured": True,
                "valid": True,
                "return_code": 0,
                "whole_command_wall_s": 5.0,
                "phases": {
                    "project_configuration_s": phase,
                    "test_worker_spawned": False,
                    "test_actions_run": False,
                    "all_tasks_dry_run_skipped": True,
                },
            })
        pairs.append({
            "pair": index,
            "original_minus_control_project_configuration_s": cost,
            "control_minus_original_project_configuration_s": -cost,
        })
    results = {"results": rows, "paired_summary": {"n_pairs": 4, "pairs": pairs}}
    return plan, results


class JavaConfigurationCauseTest(unittest.TestCase):
    def test_pairs_timeline_residual_config_and_intervention_costs_separately(self):
        timeline, phases = _timeline_fixture()
        plan, config = _config_fixture(timeline)
        data = cause_data(timeline, phases, plan, config)
        expected = [[0.5, 0.6], [0.5, 0.6], [0.4, 0.5, 0.3, 0.6]]
        for row, expected_values in zip(data["rows"], expected):
            self.assertEqual(len(row["values"]), len(expected_values))
            for actual, expected_value in zip(row["values"], expected_values):
                self.assertAlmostEqual(actual, expected_value)
        self.assertEqual([len(row["values"]) for row in data["rows"]], [2, 2, 4])

    def test_respects_360px_layout_and_keeps_wall_out_of_export(self):
        timeline, phases = _timeline_fixture()
        plan, config = _config_fixture(timeline)
        data = cause_data(timeline, phases, plan, config)
        fragment = render_html(data)
        self.assertIn("font-size:15px", fragment)
        self.assertIn("separate comparisons, not times to add together", fragment)
        roots = [ET.fromstring(svg) for svg in re.findall(r"<svg .*?</svg>", fragment)]
        self.assertEqual(len(roots), 1)
        self.assertTrue(roots[0].get("viewBox", "").startswith("0 0 360 "))
        self.assertEqual(len(roots[0].findall("circle")), 8)
        exported = csv_text(data["rows"])
        self.assertIn("phase_delta_s", exported)
        self.assertIn("validation_gate", exported)
        self.assertIn("true", exported)
        self.assertNotIn("/home/", exported)
        self.assertNotIn("whole_command_wall", exported)

    def test_rejects_intervention_that_runs_test_actions(self):
        timeline, phases = _timeline_fixture()
        plan, config = _config_fixture(timeline)
        config["results"][0]["phases"]["test_actions_run"] = True
        with self.assertRaisesRegex(ValueError, "unexpectedly launched or ran JUnit"):
            cause_data(timeline, phases, plan, config)

    def test_rejects_negative_cost_instead_of_reversing_its_sign(self):
        timeline, phases = _timeline_fixture()
        plan, config = _config_fixture(timeline)
        config["results"][0]["phases"]["project_configuration_s"] = 0.5
        config["results"][1]["phases"]["project_configuration_s"] = 1.0
        config["paired_summary"]["pairs"][0]["original_minus_control_project_configuration_s"] = -0.5
        config["paired_summary"]["pairs"][0]["control_minus_original_project_configuration_s"] = 0.5
        with self.assertRaisesRegex(ValueError, "Original-minus-generation-disabled"):
            cause_data(timeline, phases, plan, config)


if __name__ == "__main__":
    unittest.main()
