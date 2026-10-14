import json
from pathlib import Path
import tempfile
import unittest

from artifact_metadata import installed_tool_metadata, sha256_file


class InstalledToolMetadataTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)

    def install(self):
        folder = self.root / "parse-1" / "resources"
        folder.mkdir(parents=True)
        tool = folder / "clang_ast_tool"
        tool.write_bytes(b"verified producer binary")
        (folder / "clang-dumper-release-manifest.json").write_text(json.dumps({
            "assets": [{"kind": "tool", "filename": tool.name, "sha256": sha256_file(tool)}],
            "protocol": {"schema_sha256": "schema"},
            "toolchain": {"version": "25.12.19"}}))
        return tool

    def test_records_actual_installed_binary_and_release_schema(self):
        tool = self.install()
        metadata = installed_tool_metadata(self.root)
        self.assertEqual(sha256_file(tool), metadata["native_tool_sha256"])
        self.assertEqual("schema", metadata["wire_schema_sha256"])
        self.assertEqual(str(tool), metadata["native_tool_path"])

    def test_rejects_installed_binary_tampering(self):
        self.install().write_bytes(b"changed")
        with self.assertRaisesRegex(ValueError, "differs from its manifest"):
            installed_tool_metadata(self.root)

    def test_local_build_without_installed_assets_has_no_release_metadata(self):
        self.assertIsNone(installed_tool_metadata(self.root))


if __name__ == "__main__":
    unittest.main()
