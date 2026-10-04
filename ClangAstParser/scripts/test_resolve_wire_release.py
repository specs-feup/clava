"""Tests for release-selected schema and toolchain verification."""

import hashlib
import io
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import resolve_wire_release as resolver


def manifest():
    return {
        "schema_version": 2,
        "flatbuffers": {"version": resolver.FLATBUFFERS_VERSION,
                        "commit": resolver.FLATBUFFERS_COMMIT},
        "wire_schema": {
            "version": 2, "entrypoint": "wire/v2/complete.fbs",
            "asset": "clang-dumper-wire-schema-v2.zip",
            "flatbuffers_version": resolver.FLATBUFFERS_VERSION,
            "sha256": "a" * 64, "asset_sha256": "b" * 64,
        },
        "assets": [{"filename": "tool", "platform": "linux", "arch": "x64",
                    "kind": "tool", "sha256": "c" * 64}],
    }


def archive(entries):
    data = io.BytesIO()
    with zipfile.ZipFile(data, "w") as target:
        for name, content in entries:
            target.writestr(name, content)
    return data.getvalue()


class ReleaseSchemaTest(unittest.TestCase):
    def test_exact_selected_tag_fetches_its_manifest(self):
        data = json.dumps(manifest()).encode()
        with patch.object(resolver, "fetch", return_value=data) as fetch:
            result, tag, folder = resolver.resolve_manifest("v18.1.8_5")
        self.assertEqual(manifest(), result)
        self.assertEqual("v18.1.8_5", tag)
        self.assertIsNone(folder)
        fetch.assert_called_once_with(
            resolver.RELEASE_ROOT + "/v18.1.8_5/clang-dumper-release-manifest.json")

    def test_local_build_requires_manifest_without_remote_fallback(self):
        with tempfile.TemporaryDirectory() as temporary:
            with patch.object(resolver, "fetch") as fetch:
                with self.assertRaises(FileNotFoundError):
                    resolver.resolve_manifest(str(Path(temporary).resolve()))
                fetch.assert_not_called()

    def test_unsupported_manifest_or_toolchain_is_rejected(self):
        changes = [("schema_version", 1), ("flatbuffers", {"version": "old"}),
                   ("assets", [])]
        for key, value in changes:
            with self.subTest(key=key):
                bad = manifest()
                bad[key] = value
                with self.assertRaises(ValueError):
                    resolver.validate_manifest(bad)
        for key, value in [("sha256", "z" * 64), ("asset_sha256", "a" * 63),
                           ("version", 1), ("flatbuffers_version", "old"),
                           ("entrypoint", "wire/old.fbs"), ("asset", "../schema.zip")]:
            with self.subTest(key=key):
                bad = manifest()
                bad["wire_schema"][key] = value
                with self.assertRaises(ValueError):
                    resolver.validate_manifest(bad)

    def test_hash_uses_sorted_relative_paths_and_exact_bytes(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "wire/v2").mkdir(parents=True)
            (root / "wire/v2/z.fbs").write_bytes(b"last\n")
            (root / "wire/v2/a.fbs").write_bytes(b"first\n")
            expected = hashlib.sha256(
                b"wire/v2/a.fbs\0first\n\0wire/v2/z.fbs\0last\n\0").hexdigest()
            self.assertEqual(expected, resolver.canonical_schema_hash(root))
            (root / "wire/v2/a.fbs").write_bytes(b"first")
            self.assertNotEqual(expected, resolver.canonical_schema_hash(root))

    def test_archive_rejects_escape_duplicate_and_missing_root(self):
        cases = [
            [("../outside.fbs", "bad")],
            [("/wire/v2/complete.fbs", "bad")],
            [("wire/v1/complete.fbs", "bad")],
            [("wire/v2/other.fbs", "missing root")],
            [("wire/v2/complete.fbs", "one"), ("wire/v2/complete.fbs", "two")],
            [("wire/v2/complete.fbs", "root"), ("wire/v2/script.py", "bad")],
            [("wire/v2/complete.fbs", "root"), ("wire/v2/nested/hidden.fbs", "unhashed")],
        ]
        for entries in cases:
            with self.subTest(entries=entries), tempfile.TemporaryDirectory() as temporary:
                with self.assertRaises(ValueError):
                    resolver.extract_schema(archive(entries), Path(temporary))

    def test_flatc_version_mismatch_fails(self):
        result = subprocess.CompletedProcess([], 0, stdout="flatc version 1.0.0\n")
        with patch.object(resolver.subprocess, "run", return_value=result):
            with self.assertRaisesRegex(ValueError, "Expected flatc"):
                resolver.verify_flatc(Path("flatc"))

    def test_cached_compiler_is_checked_against_verified_archive(self):
        compiler = b"verified compiler"
        data = archive([("flatc", compiler)])
        with tempfile.TemporaryDirectory() as temporary:
            with patch.object(resolver, "flatc_asset", return_value=("compiler.zip", resolver.sha256(data))), \
                    patch.object(resolver, "fetch", return_value=data) as fetch, \
                    patch.object(resolver, "verify_flatc"):
                path = resolver.ensure_flatc(Path(temporary))
                self.assertEqual(compiler, path.read_bytes())
                resolver.ensure_flatc(Path(temporary))
                path.write_bytes(b"tampered")
                resolver.ensure_flatc(Path(temporary))
                self.assertEqual(compiler, path.read_bytes())
                fetch.assert_called_once()

    def test_java_runtime_requires_pinned_archive_hash(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            with patch.object(resolver, "fetch", return_value=b"wrong commit"):
                with self.assertRaisesRegex(ValueError, "runtime source archive hash"):
                    resolver.resolve_java_runtime(root / "cache", root / "runtime")

    def test_arm_hosts_bootstrap_compiler_from_verified_source(self):
        for system, machine in [("Linux", "aarch64"), ("Windows", "ARM64")]:
            with self.subTest(system=system), \
                    patch.object(resolver.platform, "system", return_value=system), \
                    patch.object(resolver.platform, "machine", return_value=machine), \
                    patch.object(resolver, "build_flatc_from_source", return_value=Path("native-flatc")) as build:
                self.assertEqual(Path("native-flatc"), resolver.ensure_flatc(Path("cache")))
                build.assert_called_once_with(Path("cache"))


if __name__ == "__main__":
    unittest.main()
