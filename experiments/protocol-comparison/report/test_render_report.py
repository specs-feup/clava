"""Regression checks for readable chart axes."""

import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
import json
import tempfile

import render_report


class AxisTickLabelsTest(unittest.TestCase):
    def test_candle_panels_share_a_tight_suite_scale(self):
        rows = []
        for mode, stages in {
            "direct": {"before-cache": 35.1, "ccache-text": 40.0, "protobuf": 42.2,
                       "flatbuffers": 39.3},
            "cold": {"ccache-text": 40.5, "protobuf": 43.0, "flatbuffers": 40.4},
            "warm": {"ccache-text": 29.7, "protobuf": 31.4, "flatbuffers": 29.2},
        }.items():
            for stage, elapsed in stages.items():
                rows.append({"suite": "java", "mode": mode, "stage": stage,
                             "measured": True, "repeat": 1, "valid": True,
                             "return_code": 0, "total_tests": 116,
                             "passed_tests": 116, "failed_tests": 0,
                             "skipped_tests": 0, "elapsed_s": elapsed})

        low, high = render_report.suite_domain(rows, "java")
        self.assertLess(low, 29.2)
        self.assertGreater(high, 43.0)
        self.assertLess(high - low, 15)
        cold_chart = render_report.chart_svg("java", "cold", rows, {}, (low, high))
        warm_chart = render_report.chart_svg("java", "warm", rows, {}, (low, high))
        self.assertNotIn("Pre-cache reference", cold_chart)
        self.assertNotIn("Before cache", cold_chart)
        def ticks(svg_text):
            return [node.text for node in ET.fromstring(svg_text).findall(".//text[@class='axis-text']")]
        self.assertEqual(ticks(cold_chart), ticks(warm_chart))
        self.assertIn("30.0s", ticks(cold_chart))

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


class PostFixReportEvidenceTest(unittest.TestCase):
    def test_incomplete_gc_control_is_reported_without_timing_claim(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            reference = {"runtime_parser_jar_sha256": "parser", "sources": {
                "native": {"tool_sha256": "native"},
                "java_build_dependencies": {"SPECS_JAVA_LIBS_HOME": {"revision": "specs"}},
            }}
            paths = []
            for attempt in (1, 2):
                path = root / f"attempt-{attempt}" / "results.json"
                path.parent.mkdir()
                path.write_text(json.dumps({
                    "complete": False, "fidelity_gate": {"passed": True},
                    "runtime_parser_jar_sha256": "parser", "sources": reference["sources"],
                    "results": [{"valid": False, "gc_policy": "disabled", "stage": "ab-protobuf",
                                 "passed_tests": 103, "failed_tests": 13, "metric_event_count": 247,
                                 "worker_gc_policy_verified": True, "run_dir": str(path.parent / "run")}
                                ]}), encoding="utf-8")
                paths.append(path)
            attempts = render_report.load_java_gc_ab_attempts(paths, root, reference)
            html = render_report.java_gc_ab_blocked_section(attempts)
            self.assertIn("No complete six-pair GC-off sample exists", html)
            self.assertEqual(html.count("103/116"), 2)
            self.assertNotIn(str(root), html)

    def test_java_gc_factorial_requires_complete_matching_runs(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            rows = []
            for policy in ("normal", "disabled"):
                for stage in render_report.AB_STAGES:
                    for repeat in (None, *range(1, 7)):
                        rows.append({
                            "suite": "java", "gc_policy": policy, "stage": stage,
                            "repeat": repeat, "measured": repeat is not None,
                            "valid": True, "total_tests": 116, "passed_tests": 116,
                            "failed_tests": 0, "skipped_tests": 0,
                            "metric_event_count": 247, "ccache_disabled": True,
                            "metrics": {"native_ms": 1000, "read_ms": 500},
                            "worker_gc_policy_verified": True,
                            "elapsed_s": 40.0 if stage == "ab-text" else 42.0,
                            "run_dir": str(root / policy / stage / str(repeat)),
                        })
            manifest = {
                "complete": True, "valid": True, "repeat_count": 6,
                "fidelity_gate": {"passed": True}, "runtime_parser_jar_sha256": "parser",
                "sources": {"native": {"tool_sha256": "native"},
                            "java_build_dependencies": {"SPECS_JAVA_LIBS_HOME": {"revision": "specs"}}},
                "results": rows,
            }
            reference = {"runtime_parser_jar_sha256": "parser",
                         "sources": json.loads(json.dumps(manifest["sources"]))}
            path = root / "results.json"
            path.write_text(json.dumps(manifest))
            loaded = render_report.load_java_gc_ab(path, root, reference)
            svg = ET.fromstring(render_report.java_gc_factorial_svg(loaded))
            self.assertEqual(len(svg.findall(".//text[@class='median-label']")), 4)
            self.assertEqual(len(svg.findall(".//circle")), 24)
            self.assertNotIn(str(root), loaded["results"][0]["evidence_ref"])

            manifest["results"][1]["passed_tests"] = 115
            path.write_text(json.dumps(manifest))
            with self.assertRaisesRegex(ValueError, "outside the matched 116-test"):
                render_report.load_java_gc_ab(path, root, reference)

            manifest["results"][1]["passed_tests"] = 116
            manifest["sources"]["java_build_dependencies"]["SPECS_JAVA_LIBS_HOME"]["revision"] = "fixed"
            path.write_text(json.dumps(manifest))
            with self.assertRaisesRegex(ValueError, "unexpected SpecsUtils/jOptions revision"):
                render_report.load_java_gc_ab(path, root, reference)
            loaded = render_report.load_java_gc_ab(path, root, reference,
                                                   expected_dependency_revision="fixed")
            self.assertTrue(loaded["dependency_revision_changed"])

    def test_merge_post_fix_ab_keeps_separate_valid_suite_reruns(self):
        def manifest(suite):
            return {
                "sources": {"clava": {"revision": "same-clava"},
                            "native": {"revision": "same-native"}},
                "runtime_parser_jar_sha256": "same-jar",
                "created_at": suite,
                "results": [{"suite": suite, "stage": stage, "repeat": repeat,
                             "measured": True, "valid": True, "elapsed_s": 40 + repeat,
                             "passed_tests": 116 if suite == "java" else 158,
                             "total_tests": 116 if suite == "java" else 164,
                             "failed_tests": 0, "metric_event_count": 247,
                             "metrics": {"native_ms": 100, "read_ms": 20,
                                         "ast_ms": 10, "dump_bytes": 1000},
                             "evidence_id": f"{suite}-{stage}-{repeat}",
                             "evidence_ref": f"{suite}/{stage}/{repeat}/summary.json"}
                            for stage in render_report.AB_STAGES for repeat in range(1, 7)],
            }

        original = manifest("clava-js")
        original["results"].append({"suite": "java", "stage": "ab-protobuf",
                                    "repeat": 2, "measured": True, "valid": False})
        combined = render_report.merge_post_fix_ab(original, manifest("java"))
        self.assertEqual(len([row for row in combined["results"] if row["suite"] == "java"]), 12)
        self.assertEqual(len(combined["java_recheck_audit"]), 1)
        self.assertIn("same build", render_report.post_fix_ab_section(combined))

    def test_post_fix_ab_run_links_have_retained_targets(self):
        rows = []
        for suite, passed, total in (("clava-js", 158, 164), ("java", 116, 116)):
            for stage in render_report.AB_STAGES:
                rows.append({
                    "suite": suite,
                    "stage": stage,
                    "repeat": 2,
                    "measured": True,
                    "valid": True,
                    "passed_tests": passed,
                    "total_tests": total,
                    "failed_tests": 0,
                    "metric_event_count": 247,
                    "evidence_id": f"ab-{suite}-{stage}-r2",
                    "evidence_ref": f"timing/{suite}/{stage}/summary.json",
                    "junit_ref": None,
                })
        html = render_report.post_fix_ab_section({"results": rows})
        self.assertIn('href="#ab-java-ab-protobuf-r2"', html)
        self.assertIn('id="ab-java-ab-protobuf-r2"', html)

    def test_gc_summary_explains_external_pre_fix_recordings(self):
        profiles = [
            {"format": mode, "explicit_requests_from_used_memory": count,
             "sha256": f"{mode}-{when}", "source_ref": f"{when}/{mode}.jfr"}
            for when, count in (("pre_fix", 432), ("post_fix", 216))
            for mode in ("Text", "Protobuf")
        ]
        html = render_report.gc_fix_evidence_html({
            "pre_fix": {"per_run_requests": 432, "profiles": profiles[:2]},
            "post_fix": {"per_run_requests": 216, "profiles": profiles[2:]},
            "request_reduction_percent": 50,
        })
        self.assertIn("not bundled here", html)
        self.assertIn("JFR event counts, hashes, and recording provenance", html)
        self.assertIn("not completed-collection counts or total pause time", html)


if __name__ == "__main__":
    unittest.main()
