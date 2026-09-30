from __future__ import annotations

import subprocess
import tempfile
from pathlib import Path
from unittest.mock import patch
import unittest

import run_standalone as harness


class StandaloneHarnessTests(unittest.TestCase):
    def make_event(self, root: Path) -> dict:
        source = root / "source.cpp"
        dependency = root / "include" / "input.hpp"
        working_directory = root / "work"
        generated_root = root / "generated"
        dependency.parent.mkdir()
        working_directory.mkdir()
        generated_root.mkdir()
        source.write_text("int value;\n", encoding="utf-8")
        dependency.write_text("#define VALUE 1\n", encoding="utf-8")
        return {
            "event_id": "event-1", "suite": "clava-js", "ordinal": 1,
            "source": {
                "original_path": str(source), "sha256": harness.sha256_file(source),
                "size_bytes": source.stat().st_size,
            },
            "dependencies": [{
                "original_path": str(dependency), "sha256": harness.sha256_file(dependency),
                "size_bytes": dependency.stat().st_size,
            }],
            "working_directory": str(working_directory),
            "original_cwd": str(working_directory),
            "generated_parse_root": str(generated_root),
            "compiler_options": [f"-I{dependency.parent}", "relative.cpp", "-DKEEP=1"],
            "parser_config": {"generated_parse_root": str(generated_root)},
            "effective_libc_mode": "SYSTEM", "parse_id": "17", "standard": "-std=c++17",
            "args_sha256": "args-digest", "source_label": "source.cpp",
            "argv": [
                "/opt/clang-dumper", str(source), f"-I{dependency.parent}", "-id=17",
                "-o", str(root / "discarded-output.ast"), "-ast-dump-format=text", "-DKEEP=1",
            ],
        }

    def test_schedule_keeps_captured_input_paths_and_options(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            event = self.make_event(root)
            live = harness.describe_event_inputs(event)
            output_root = root / "runner-output"
            operation = harness.parse_operation(
                event, live, output_root, "text", "directbypass", "diagnostic", 1,
            )

            self.assertEqual(operation["source_path"], event["source"]["original_path"])
            self.assertEqual(operation["replay_cwd"], event["working_directory"])
            self.assertEqual(operation["generated_parse_root"], event["generated_parse_root"])
            self.assertEqual(operation["parser_config"]["generated_parse_root"], event["generated_parse_root"])
            self.assertEqual(operation["compiler_options"], event["compiler_options"])
            self.assertEqual(
                operation["expected_native_argv"],
                ["/opt/clang-dumper", event["source"]["original_path"],
                 event["compiler_options"][0], "-id=<id>", "-DKEEP=1"],
            )
            self.assertFalse((root / "inputs").exists())
            self.assertEqual(
                {row["original_path"] for row in live["files"]},
                {event["source"]["original_path"], event["dependencies"][0]["original_path"]},
            )

    def test_measure_batch_dispatch_keeps_captured_input_paths_and_options(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            event = self.make_event(root)
            live = {event["event_id"]: harness.describe_event_inputs(event)}
            rows = harness.build_full_batch(
                [event], live, root / "runner-output", "text", "directbypass", "measure", 2,
            )

            parse_rows = [row for row in rows if row.get("operation") is None]
            self.assertEqual({row["phase"] for row in parse_rows}, {"warmup", "measure"})
            for row in parse_rows:
                self.assertEqual(row["source_path"], event["source"]["original_path"])
                self.assertEqual(row["replay_cwd"], event["working_directory"])
                self.assertEqual(row["generated_parse_root"], event["generated_parse_root"])
                self.assertEqual(row["compiler_options"], event["compiler_options"])

    def test_harness_contains_no_blob_link_or_copy_staging(self) -> None:
        source = Path(harness.__file__).read_text(encoding="utf-8")

        self.assertNotIn("os.link(", source)
        self.assertNotIn("shutil.copyfile(", source)

    def test_conflicting_snapshots_fail_before_file_hashing(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            event = self.make_event(Path(temporary))
            conflicting = {**event, "event_id": "event-2", "source": {
                **event["source"], "sha256": "0" * 64,
            }}
            with patch.object(harness, "sha256_file") as hash_file:
                with self.assertRaisesRegex(RuntimeError, "conflicting SHA-256 snapshots"):
                    harness.preflight_live_inputs([event, conflicting], "before measurement")

            hash_file.assert_not_called()

    def test_missing_or_changed_live_inputs_fail_preflight(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            event = self.make_event(root)
            expected = harness.captured_input_hashes([event])
            source = Path(event["source"]["original_path"])
            source.write_text("changed\n", encoding="utf-8")

            with self.assertRaisesRegex(RuntimeError, "SHA-256 mismatch"):
                harness.verify_live_inputs([event], expected, "before measurement")

            source.unlink()
            with self.assertRaisesRegex(RuntimeError, "missing source/dependency"):
                harness.verify_live_inputs([event], expected, "before measurement")

    def test_changed_input_is_caught_before_java_batch_starts(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            event = self.make_event(root)
            expected = harness.captured_input_hashes([event])
            Path(event["source"]["original_path"]).write_text("changed\n", encoding="utf-8")

            with patch.object(harness.subprocess, "run") as java_run:
                with self.assertRaisesRegex(RuntimeError, "before measure-one"):
                    harness.run_schedule(
                        root / "output", [], {}, [event], expected,
                        root / "classes", root / "runtime", "Runner", root / "tool", "measure-one",
                    )

            java_run.assert_not_called()
            self.assertFalse((root / "output" / "schedules").exists())

    def test_input_mutation_during_java_batch_is_caught_after_batch(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            event = self.make_event(root)
            expected = harness.captured_input_hashes([event])
            source = Path(event["source"]["original_path"])

            def mutate_input(*_args, **_kwargs):
                source.write_text("changed during batch\n", encoding="utf-8")
                return subprocess.CompletedProcess(["java"], 0)

            with patch.object(harness, "environment_for", return_value={"TMPDIR": str(root / "tmp")}), \
                    patch.object(harness, "java_command", return_value=["java"]), \
                    patch.object(harness.subprocess, "run", side_effect=mutate_input) as java_run:
                with self.assertRaisesRegex(RuntimeError, "after measure-one"):
                    harness.run_schedule(
                        root / "output", [], {}, [event], expected,
                        root / "classes", root / "runtime", "Runner", root / "tool", "measure-one",
                    )

            java_run.assert_called_once()


if __name__ == "__main__":
    unittest.main()
