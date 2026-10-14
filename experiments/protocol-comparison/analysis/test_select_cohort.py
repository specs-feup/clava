from __future__ import annotations

import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

import analyze_deadline as analyzer
import select_cohort as selector
from test_analyze_deadline import fixture_manifest


def java_partition(primary):
    java = copy.deepcopy(primary)
    java["plan"]["suites"] = ["java"]
    java["plan"]["cells"] = [cell for cell in java["plan"]["cells"] if cell["suite"] == "java"]
    java["identity_preflight"]["reference_test_ids"] = {
        "java": java["identity_preflight"]["reference_test_ids"]["java"]
    }
    java["identity_preflight"]["warmup_test_identities"] = {
        "java": java["identity_preflight"]["warmup_test_identities"]["java"]
    }
    java["results"] = [row for row in java["results"] if row["suite"] == "java"]
    return java


def add_rejected_js_attempt(primary):
    selected = next(row for row in primary["results"]
                    if row["cell_id"] == "clava-js/direct/protobuf/repeat-03")
    selected["attempt"] = 2
    rejected = copy.deepcopy(selected)
    rejected.update({
        "attempt": 1,
        "selected": False,
        "superseded_by_attempt": 2,
        "valid": False,
        "return_code": 1,
        "failure_names": ["repaired fixture failure"],
        "elapsed_s": 99999.0,
    })
    primary["results"].append(rejected)


class SelectCohortTest(unittest.TestCase):
    def setUp(self):
        self.primary = fixture_manifest()
        add_rejected_js_attempt(self.primary)
        self.java = java_partition(self.primary)
        self.primary_source = Path("primary-results.json")
        self.java_source = Path("java-noagent-results.json")

    def test_selects_only_js_while_preserving_rows_and_selected_flags(self):
        selected, provenance = selector.select_js_partition(
            self.primary, self.primary_source, self.java, self.java_source
        )

        self.assertEqual(selected["plan"]["suites"], ["clava-js"])
        self.assertTrue(all(cell["suite"] == "clava-js" for cell in selected["plan"]["cells"]))
        self.assertEqual(set(selected["identity_preflight"]["reference_test_ids"]), {"clava-js"})
        self.assertEqual(set(selected["identity_preflight"]["warmup_test_identities"]), {"clava-js"})
        self.assertEqual(selected["results"], [row for row in self.primary["results"]
                                                if row["suite"] == "clava-js"])
        self.assertTrue(any(row["selected"] is False for row in selected["results"]))

        _, planned, _, _, audit = analyzer.validate_manifest(selected, Path("js-partition.json"))
        self.assertEqual(planned, {cell for cell in analyzer.expected_cells() if cell[0] == "clava-js"})
        self.assertEqual(len(audit), 1)
        self.assertEqual(provenance["timing_handling"],
                         "result rows copied verbatim; no timing or repeat arithmetic performed")
        self.assertEqual(provenance["cross_suite_rounds"], "not combined by this selector")

    def test_rejects_primary_that_is_not_complete(self):
        incomplete = copy.deepcopy(self.primary)
        incomplete["results"].pop()
        with self.assertRaises(analyzer.AnalysisError):
            selector.select_js_partition(incomplete, self.primary_source,
                                          self.java, self.java_source)

    def test_rejects_java_input_that_is_not_its_own_partition(self):
        java_with_js = copy.deepcopy(self.primary)
        with self.assertRaisesRegex(analyzer.AnalysisError, "Java-only"):
            selector.select_js_partition(self.primary, self.primary_source,
                                         java_with_js, self.java_source)

    def test_rejects_java_manifest_with_different_primary_provenance(self):
        java = copy.deepcopy(self.java)
        java["plan"]["experiment"] = "different-experiment"
        with self.assertRaisesRegex(analyzer.AnalysisError, "plan.experiment differs"):
            selector.select_js_partition(self.primary, self.primary_source,
                                         java, self.java_source)

    def test_cli_records_source_sha_and_leaves_inputs_unchanged(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            primary_path = root / "primary.json"
            java_path = root / "java-off40.json"
            output_dir = root / "selected"
            primary_bytes = json.dumps(self.primary).encode("utf-8")
            java_bytes = json.dumps(self.java).encode("utf-8")
            primary_path.write_bytes(primary_bytes)
            java_path.write_bytes(java_bytes)

            self.assertEqual(selector.main([
                "--primary", str(primary_path), "--java", str(java_path),
                "--output-dir", str(output_dir),
            ]), 0)

            self.assertEqual(primary_path.read_bytes(), primary_bytes)
            self.assertEqual(java_path.read_bytes(), java_bytes)
            output_path = output_dir / "clava-js.results.json"
            provenance_path = output_dir / "selection-provenance.json"
            output = json.loads(output_path.read_text(encoding="utf-8"))
            provenance = json.loads(provenance_path.read_text(encoding="utf-8"))
            self.assertEqual(output["plan"]["suites"], ["clava-js"])
            self.assertEqual(provenance["primary_source"]["sha256"],
                             hashlib.sha256(primary_bytes).hexdigest())
            self.assertEqual(provenance["java_source"]["sha256"],
                             hashlib.sha256(java_bytes).hexdigest())
            self.assertEqual(provenance["derived_manifest"]["sha256"],
                             hashlib.sha256(output_path.read_bytes()).hexdigest())


if __name__ == "__main__":
    unittest.main()
