"""Guard the narrowly reviewed source-fidelity exception path."""

import copy
import json
from pathlib import Path
import tempfile
import unittest

from run_corpus_consumer import apply_reviewed_differences, sha256_file


class ReviewedDifferenceTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        source = self.root / "case.c"
        source.write_text('void f(void) { asm("%0"); }\n')
        self.source_hash = sha256_file(source)
        self.manifest = {"files": [{"relative": "case.c", "source": str(source)}]}
        self.runtime = {"jar_manifest_sha256": "jars", "native_tool_sha256": "tool"}
        self.results = [{"label": "eager", "runtime": self.runtime}]
        self.comparisons = [{"control": "text", "relative": "case.c",
                             "status": "GENERATED_CODE_MISMATCH",
                             "eager_code_sha256": "source-spelling",
                             "control_code_sha256": "ir-spelling"}]
        self.proof = {"reparse_generated": True, "results": [{
            "label": "eager", "runtime": copy.deepcopy(self.runtime), "rows": [{
                "relative": "case.c", "bucket": "CLEAN",
                "source_sha256": self.source_hash,
                "generated_code_sha256": "source-spelling",
                "regenerated_code_sha256": "source-spelling",
                "source_equal_after_reparse": True}]}]}
        self.entry = {"control": "text", "relative": "case.c",
                      "source_sha256": self.source_hash,
                      "eager_code_sha256": "source-spelling",
                      "control_code_sha256": "ir-spelling",
                      "reason": "Preserve assembly source operands instead of LLVM IR spelling.",
                      "roundtrip_summary": "proof.json"}

    def apply(self):
        proof = self.root / "proof.json"
        proof.write_text(json.dumps(self.proof))
        self.entry["roundtrip_summary_sha256"] = sha256_file(proof)
        inventory = self.root / "reviewed.json"
        inventory.write_text(json.dumps({"version": 1, "differences": [self.entry]}))
        return apply_reviewed_differences(self.comparisons, self.results, self.manifest, inventory)

    def test_accepts_exact_reviewed_correction_with_stable_roundtrip(self):
        self.assertEqual(1, self.apply()["corrections"])
        self.assertEqual("REVIEWED_SOURCE_FIDELITY_CORRECTION", self.comparisons[0]["status"])

    def test_rejects_changed_source(self):
        Path(self.manifest["files"][0]["source"]).write_text("changed")
        with self.assertRaisesRegex(ValueError, "source drift"):
            self.apply()

    def test_rejects_changed_generated_code(self):
        self.comparisons[0]["eager_code_sha256"] = "changed"
        with self.assertRaisesRegex(ValueError, "eager_code_sha256 drift"):
            self.apply()

    def test_rejects_different_ast_runtime_jars(self):
        self.proof["results"][0]["runtime"]["jar_manifest_sha256"] = "changed"
        with self.assertRaisesRegex(ValueError, "different runtime jars"):
            self.apply()

    def test_rejects_different_native_tool(self):
        self.proof["results"][0]["runtime"]["native_tool_sha256"] = "changed"
        with self.assertRaisesRegex(ValueError, "different runtime jars or native tool"):
            self.apply()

    def test_rejects_failed_reparse(self):
        self.proof["results"][0]["rows"][0]["bucket"] = "CONSUMER_FAIL"
        with self.assertRaisesRegex(ValueError, "no matching stable round-trip"):
            self.apply()

    def test_rejects_unstable_regeneration(self):
        self.proof["results"][0]["rows"][0]["regenerated_code_sha256"] = "changed"
        with self.assertRaisesRegex(ValueError, "no matching stable round-trip"):
            self.apply()

    def test_cannot_waive_consumer_regressions(self):
        self.comparisons[0]["status"] = "EAGER_REGRESSION"
        with self.assertRaisesRegex(ValueError, "no longer a code mismatch"):
            self.apply()


if __name__ == "__main__":
    unittest.main()
