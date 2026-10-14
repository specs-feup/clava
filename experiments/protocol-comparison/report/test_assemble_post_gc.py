"""Checks provenance validation used by the post-GC report assembler."""

import hashlib
import json
import tempfile
import unittest
from pathlib import Path

import assemble_post_gc


class BeforeCacheDumperEvidenceTest(unittest.TestCase):
    def _write_release(self, binary: Path, contents: bytes) -> str:
        binary.parent.mkdir(parents=True, exist_ok=True)
        binary.write_bytes(contents)
        digest = hashlib.sha256(contents).hexdigest()
        (binary.parent / "clang-dumper-release-manifest.json").write_text(
            json.dumps({"assets": [{"filename": binary.name, "sha256": digest}]}),
            encoding="utf-8",
        )
        return digest

    def test_both_suite_downloads_must_match_the_pinned_release_hash(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            release = Path("clang-dumper/releases/v18.1.8_4/clang-dumper-linux-x64")
            java = root / "matrix/direct/before-cache/temp/java/before-cache/clang_ast_exe_lmsousa" / release
            clava = root / "matrix/direct/before-cache/cache/clava-js/before-cache/@specs-feup/clava" / release
            expected = self._write_release(java, b"pinned dumper")
            self._write_release(clava, b"pinned dumper")

            evidence = assemble_post_gc.before_cache_dumper_evidence(root)

            self.assertEqual(evidence["dumper_sha256"], expected)
            self.assertIn("checksum verified", evidence["dumper_artifact_ref"])

    def test_rejects_different_executables_across_suites(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            release = Path("clang-dumper/releases/v18.1.8_4/clang-dumper-linux-x64")
            java = root / "matrix/direct/before-cache/temp/java/before-cache/clang_ast_exe_lmsousa" / release
            clava = root / "matrix/direct/before-cache/cache/clava-js/before-cache/@specs-feup/clava" / release
            self._write_release(java, b"java dumper")
            self._write_release(clava, b"clava dumper")

            with self.assertRaisesRegex(ValueError, "loaded different dumper executables"):
                assemble_post_gc.before_cache_dumper_evidence(root)


if __name__ == "__main__":
    unittest.main()
