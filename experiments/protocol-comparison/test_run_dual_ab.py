#!/usr/bin/env python3
"""Focused unit checks for the dual-format A/B runner's source and metric gates."""

from __future__ import annotations

import copy
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest


SCRIPT_ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_ROOT))
import run_dual_ab as runner  # noqa: E402


class SourceIdentityTests(unittest.TestCase):
    def setUp(self) -> None:
        self.js_source = {
            "workspace": "/scratch/Clava-JS",
            "repo": "/scratch",
            "revision": "host-before",
            "tree": "subtree-sha",
            "status": [],
            "tree_manifest": {"file_count": 12, "sha256": "manifest-sha"},
        }

    def test_unrelated_host_commit_does_not_change_js_source_identity(self) -> None:
        current = {**self.js_source, "revision": "host-after"}

        self.assertTrue(runner.source_identity_matches("clava_js", current, self.js_source))

    def test_js_subtree_identity_changes_are_rejected(self) -> None:
        for field in ("workspace", "repo", "tree", "status", "tree_manifest"):
            with self.subTest(field=field):
                current = copy.deepcopy(self.js_source)
                current[field] = "changed"
                self.assertFalse(runner.source_identity_matches("clava_js", current, self.js_source))

    def test_composite_dependency_revisions_remain_strict(self) -> None:
        recorded = {
            "clava": {"revision": "clava-rev"},
            "native": {"revision": "native-rev"},
            "clava_js": self.js_source,
            "java_build_dependencies": {"SPECS_JAVA_LIBS_HOME": {"revision": "deps-rev"}},
        }
        current = copy.deepcopy(recorded)
        current["clava_js"]["revision"] = "host-after"
        self.assertTrue(runner.source_metadata_matches(current, recorded))

        current["java_build_dependencies"]["SPECS_JAVA_LIBS_HOME"]["revision"] = "changed"
        self.assertFalse(runner.source_metadata_matches(current, recorded))


class MetricCollectionTests(unittest.TestCase):
    def test_junit_xml_metrics_take_precedence_over_gradle_log(self) -> None:
        with TemporaryDirectory() as temporary:
            root = Path(temporary)
            junit_root = root / "junit"
            junit_root.mkdir()
            (root / "run.log").write_text('CLAVA_AST_METRIC {"source":"log"}\n')
            (junit_root / "TEST-suite.xml").write_text(
                '<testsuites><testsuite><testcase><system-out>'
                'CLAVA_AST_METRIC {"source":"junit"}'
                '</system-out></testcase></testsuite></testsuites>'
            )

            events = runner.java_metrics(root / "run.log", junit_root)

        self.assertEqual([{"source": "junit"}], events)

    def test_log_is_fallback_when_junit_has_no_metric_events(self) -> None:
        with TemporaryDirectory() as temporary:
            root = Path(temporary)
            junit_root = root / "junit"
            junit_root.mkdir()
            (root / "run.log").write_text('CLAVA_AST_METRIC {"source":"log"}\n')
            (junit_root / "TEST-suite.xml").write_text(
                "<testsuite><testcase><system-out>no metrics</system-out></testcase></testsuite>"
            )

            events = runner.java_metrics(root / "run.log", junit_root)

        self.assertEqual([{"source": "log"}], events)

    def test_reference_count_is_enforced_only_for_full_timing_rows(self) -> None:
        with TemporaryDirectory() as temporary:
            root = Path(temporary)
            stage = {"key": "ab-protobuf", "label": "Protobuf", "wire": "protobuf"}
            event = {
                "format": "protobuf", "compressed": False,
                "ccache_disabled": True, "cached": False,
            }

            gate_result = runner.metric_validation(
                [event], stage, "clava-js", root / "probe", root / "ccache",
            )
            timing_result = runner.metric_validation(
                [event], stage, "clava-js", root / "probe", root / "ccache",
                enforce_event_count_reference=True,
            )

        self.assertFalse(gate_result["event_count_matches_reference"])
        self.assertFalse(gate_result["event_count_reference_enforced"])
        self.assertTrue(gate_result["passed"])
        self.assertTrue(timing_result["event_count_reference_enforced"])
        self.assertFalse(timing_result["passed"])


if __name__ == "__main__":
    unittest.main()
