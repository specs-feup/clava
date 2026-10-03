from __future__ import annotations

import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch


sys.path.insert(0, str(Path(__file__).resolve().parent))
import run_java_runtime_comparison as java_comparison  # noqa: E402
import run_runtime_comparison as js_comparison  # noqa: E402


class JavaClasspathProvenanceTest(unittest.TestCase):
    def test_same_size_content_change_before_observation_fails(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            artifact = Path(temporary) / "test-classes.bin"
            artifact.write_bytes(b"AAAA")
            preflight = java_comparison.fingerprint_test_classpath([], [str(artifact)])["sha256"]

            stat = artifact.stat()
            artifact.write_bytes(b"BBBB")
            os.utime(artifact, ns=(stat.st_atime_ns, stat.st_mtime_ns))
            pre_run = java_comparison.fingerprint_test_classpath([], [str(artifact)])["sha256"]

        self.assertNotEqual(preflight, pre_run)
        self.assertIn(
            "test classpath content differs from the preflight fingerprint before timing",
            java_comparison.classpath_fingerprint_errors(preflight, pre_run, pre_run),
        )

    def test_content_change_during_observation_fails(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            artifact = Path(temporary) / "test-classes.bin"
            artifact.write_bytes(b"before")
            preflight = java_comparison.fingerprint_test_classpath([], [str(artifact)])["sha256"]
            pre_run = java_comparison.fingerprint_test_classpath([], [str(artifact)])["sha256"]
            artifact.write_bytes(b"during")
            post_run = java_comparison.fingerprint_test_classpath([], [str(artifact)])["sha256"]

        self.assertIn(
            "test classpath content changed during the observation",
            java_comparison.classpath_fingerprint_errors(preflight, pre_run, post_run),
        )


class JavaRuntimeClasspathPreparationTest(unittest.TestCase):
    def test_default_local_options_are_seeded_before_fingerprint(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            main_classes = Path(temporary) / "ClangAstParser" / "build" / "classes" / "java" / "main"
            main_classes.mkdir(parents=True)
            checkout = Path(temporary)
            classpath = {"test_classpath": [str(main_classes)]}

            prepared = java_comparison.prepare_empty_local_options(checkout, classpath)
            self.assertTrue(prepared["created_before_classpath_fingerprint"])
            self.assertEqual(
                prepared["sha256"], java_comparison.sha256_file(Path(prepared["path"]))
            )
            self.assertFalse(java_comparison.prepare_empty_local_options(checkout, classpath)[
                "created_before_classpath_fingerprint"
            ])

            Path(prepared["path"]).write_text("custom parser options\n")
            with self.assertRaisesRegex(RuntimeError, "non-default local parser options"):
                java_comparison.prepare_empty_local_options(checkout, classpath)


class HistoricalGlobalAttributesFixtureTest(unittest.TestCase):
    def test_overlay_uses_control_script_and_golden_with_same_input(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            current = root / "main" / "clava"
            current_js = current / "Clava-JS"
            current_resources = current / "ClavaWeaver" / "resources" / "clava"
            text_checkout = root / "text-build"
            protobuf_checkout = root / "protobuf-build"

            for directory in (current_js / "api", current_js / "code", current_js / "node_modules",
                              current_js / "woven_code", current_resources / "test/weaver/cpp/results",
                              current_resources / "test/weaver/cpp/src"):
                directory.mkdir(parents=True)
            (current_js / "package.json").write_text('{"name":"@specs-feup/clava"}\n')
            (current_js / "api" / "suite.test.ts").write_text("export {};\n")
            (current_js / "node_modules" / "cache.txt").write_text("cache\n")
            (current_resources / "test/weaver/GlobalAttributes.js").write_text("type.getValue('kind');\n")
            (current_resources / "test/weaver/cpp/results/GlobalAttributes.js.txt").write_text("kind golden\n")
            (current_resources / "test/weaver/cpp/src/global_attributes.cpp").write_text("int main() {}\n")
            (current_resources / "preserved.txt").write_text("current suite resources\n")

            for checkout in (text_checkout, protobuf_checkout):
                resources = checkout / "clava" / "ClavaWeaver" / "resources" / "clava"
                (resources / "test/weaver/cpp/results").mkdir(parents=True)
                (resources / "test/weaver/cpp/src").mkdir(parents=True)
                (resources / "test/weaver").mkdir(exist_ok=True)
                (resources / "test/weaver/GlobalAttributes.js").write_text("type.getValue('builtinKind');\n")
                (resources / "test/weaver/cpp/results/GlobalAttributes.js.txt").write_text("legacy golden\n")
                (resources / "test/weaver/cpp/src/global_attributes.cpp").write_text("int main() {}\n")

            with patch.object(js_comparison, "CLAVA_ROOT", current), patch.object(
                js_comparison, "CLAVA_JS_ROOT", current_js
            ):
                expected = js_comparison.compare_global_attributes_fixtures({
                    "eager": current,
                    "text": text_checkout,
                    "protobuf": protobuf_checkout,
                })
                workspace, staged = js_comparison.stage_control_workspace(
                    root / "results", "text", text_checkout
                )

            staged_resources = Path(staged["resource_root"])
            self.assertEqual(staged["files"], expected["text"]["files"])
            self.assertEqual(
                staged["files"]["test/weaver/GlobalAttributes.js"],
                js_comparison.sha256_file(
                    text_checkout / "clava/ClavaWeaver/resources/clava/test/weaver/GlobalAttributes.js"
                ),
            )
            self.assertEqual(
                staged["files"]["test/weaver/cpp/results/GlobalAttributes.js.txt"],
                js_comparison.sha256_file(
                    text_checkout
                    / "clava/ClavaWeaver/resources/clava/test/weaver/cpp/results/GlobalAttributes.js.txt"
                ),
            )
            self.assertEqual(
                staged["files"]["test/weaver/cpp/src/global_attributes.cpp"],
                expected["eager"]["files"]["test/weaver/cpp/src/global_attributes.cpp"],
            )
            self.assertEqual(
                (staged_resources / "preserved.txt").read_text(), "current suite resources\n"
            )
            self.assertEqual(
                staged["staged_resource_tree_sha256"], js_comparison.sha256_tree(staged_resources)
            )
            self.assertTrue((workspace / "api").is_symlink())
            registry = json.loads((workspace.parent / "package.json").read_text())
            self.assertEqual(registry["workspaces"], ["Clava-JS"])
            self.assertEqual(
                (current_resources / "test/weaver/GlobalAttributes.js").read_text(),
                "type.getValue('kind');\n",
            )


if __name__ == "__main__":
    unittest.main()
