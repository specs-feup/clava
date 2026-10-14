from __future__ import annotations

import csv
import json
import os
from pathlib import Path
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch


APP_BUILD_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(APP_BUILD_DIR))
import run_app_build_matrix as matrix


class WorkloadNormalizationTests(unittest.TestCase):
    def test_normalizes_checkout_and_temp_roots_recursively(self):
        stage = {
            "root": Path("/checkout-a/stage"),
            "clava": Path("/checkout-a/stage/clava"),
        }
        workload = {
            "sources": ["/checkout-a/stage/clava/src/main.cpp", "/checkout-a/stage/include/api.h"],
            "options": ["-I/checkout-a/stage/clava/include", "-o/tmp/run-a/result.o"],
            "nested": {"work": "/tmp/run-a/context"},
        }
        normalized = matrix.normalize(workload, stage, Path("/tmp/run-a"))
        self.assertEqual(normalized["sources"], ["$CLAVA/src/main.cpp", "$STAGE/include/api.h"])
        self.assertEqual(normalized["options"], ["-I$CLAVA/include", "-o$TMP/result.o"])
        self.assertEqual(normalized["nested"]["work"], "$TMP/context")

    def test_excludes_cache_and_execution_switches_from_workload_identity(self):
        stage = {"root": Path("/checkout/stage"), "clava": Path("/checkout/stage/clava")}
        normalized = matrix.normalize({
            "ast_dump_cache": "/tmp/cache-a",
            "show_exec_info": True,
            "direct_mode": False,
            "nested": {"cache-mode": "warm", "stable_option": "-std=c++17"},
        }, stage, Path("/tmp/run"))
        self.assertEqual(normalized, {"nested": {"stable_option": "-std=c++17"}})

    def test_enriched_group_fingerprint_is_stable_across_checkout_locations(self):
        inputs = {
            "input_sources": ["{clava}/src/main.cpp", "{root}/include/api.h"],
            "resolved_sources": ["{clava}/src/main.cpp", "{root}/include/api.h"],
            "compiler_options": ["-I{root}/include", "-I{tmp}/include"],
            "parser_config": {"temp": "{tmp}/context"},
            "elapsed_ms": 4.5,
            "parser_class_origin": "/overlay/app-build.jar",
            "valid": True,
            "app_returned_null": False,
        }
        enriched = []
        for checkout, temp in (("/checkout-one", "/tmp/one"), ("/checkout-two", "/tmp/two")):
            root = Path(checkout) / "stage"
            clava = root / "clava"
            stage = {"root": root, "clava": clava}
            row = {
                **inputs,
                "input_sources": [item.format(root=root, clava=clava, tmp=temp) for item in inputs["input_sources"]],
                "resolved_sources": [item.format(root=root, clava=clava, tmp=temp) for item in inputs["resolved_sources"]],
                "compiler_options": [item.format(root=root, clava=clava, tmp=temp) for item in inputs["compiler_options"]],
                "parser_config": {"temp": inputs["parser_config"]["temp"].format(root=root, clava=clava, tmp=temp)},
            }
            with tempfile.TemporaryDirectory() as directory:
                metrics = Path(directory) / "calls.jsonl"
                metrics.write_text(json.dumps(row) + "\n", encoding="utf-8")
                enriched.append(matrix.enrich_calls(
                    metrics, "java", "protobuf", "cold", 1, stage, Path(temp)
                )[0])
        self.assertEqual(enriched[0]["group_fingerprint"], enriched[1]["group_fingerprint"])
        self.assertEqual(enriched[0]["group_id"], enriched[1]["group_id"])
        self.assertEqual(enriched[0]["normalized_workload"]["input_sources"][0], "$CLAVA/src/main.cpp")
        self.assertEqual(enriched[0]["normalized_workload"]["compiler_options"], ["-I$STAGE/include", "-I$TMP/include"])


class ContextPartitionTests(unittest.TestCase):
    @staticmethod
    def _call(fingerprint: str, context: str, occurrence: int = 0) -> dict:
        return {
            "group_fingerprint": fingerprint,
            "group_id": f"{fingerprint}:{occurrence:04d}",
            "context_id": context,
            "elapsed_ms": 1.0,
            "syntax_only": False,
        }

    def test_context_partition_ignores_call_order_and_opaque_context_labels(self):
        before = [
            self._call("empty-app", "context-1"),
            self._call("c-file-a", "context-1"),
            self._call("c-file-b", "context-2"),
            self._call("c-file-c", "context-3"),
        ]
        after = [
            self._call("c-file-b", "opaque-77"),
            self._call("empty-app", "opaque-99"),
            self._call("c-file-a", "opaque-99"),
            self._call("c-file-c", "opaque-13"),
        ]
        self.assertEqual(matrix.context_pattern(before), matrix.context_pattern(after))

    def test_compare_rejects_a_real_change_in_which_apps_share_context(self):
        reference_rows = [
            self._call("app-a", "shared"),
            self._call("app-b", "shared"),
            self._call("app-c", "separate"),
        ]
        changed_rows = [
            self._call("app-a", "one"),
            self._call("app-b", "two"),
            self._call("app-c", "three"),
        ]
        self.assertEqual(matrix.workload_counter(reference_rows), matrix.workload_counter(changed_rows))
        reference = {"java": matrix.workload_counter(reference_rows)}
        contexts = {"java": matrix.context_pattern(reference_rows)}
        result = {"suite": "java", "stage": "protobuf", "mode": "direct", "repeat": 2}
        with patch.object(matrix, "load_calls", return_value=changed_rows):
            with self.assertRaisesRegex(RuntimeError, "ClavaContext sharing pattern changed"):
                matrix.compare_workloads([result], reference, contexts)

    def test_verified_preflight_first_seen_labels_convert_to_same_partition(self):
        rows = [
            self._call("app-a", "opaque-1"),
            self._call("app-b", "opaque-1"),
            self._call("app-c", "opaque-2"),
        ]
        legacy = [[row["group_fingerprint"], label] for row, label in zip(rows, ("0", "0", "1"))]
        self.assertEqual(matrix.canonical_context_reference(legacy), matrix.context_pattern(rows))


class ScheduleTests(unittest.TestCase):
    def test_four_rounds_each_cover_all_twenty_suite_mode_stage_cells(self):
        expected = {
            (suite, stage, mode)
            for suite in matrix.SUITES
            for stage in matrix.STAGE_KEYS
            for mode in (("direct",) if stage == "before-cache" else ("direct", "cold", "warm"))
        }
        self.assertEqual(len(expected), 20)
        round_orders = []
        for repeat in range(1, 5):
            scheduled = matrix.schedule(repeat)
            cells = [(suite, item["key"], mode) for suite, mode, item in scheduled]
            self.assertEqual(len(cells), 20)
            self.assertEqual(len(set(cells)), 20)
            self.assertEqual(set(cells), expected)
            self.assertFalse(any(stage == "before-cache" and mode != "direct"
                                 for _, stage, mode in cells))
            round_orders.append(tuple(item["key"] for _, _, item in scheduled[::2]))
        self.assertEqual(len(set(round_orders)), 4)

    def test_repeat_count_cannot_change_the_four_round_contract(self):
        with patch.object(sys, "argv", ["run_app_build_matrix.py", "--phase", "measure", "--repeat-count", "3"]):
            with self.assertRaises(SystemExit):
                matrix.parse_args()

    def test_default_repeat_count_is_four(self):
        with patch.object(sys, "argv", ["run_app_build_matrix.py", "--phase", "preflight"]):
            self.assertEqual(matrix.parse_args().repeat_count, 4)

    def test_csv_keeps_report_repeats_one_based(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            results = []
            for repeat in range(1, 5):
                metrics = root / f"calls-{repeat}.jsonl"
                metrics.write_text(json.dumps({
                    "group_id": f"group-{repeat}", "elapsed_ms": 1.25,
                }) + "\n", encoding="utf-8")
                results.append({
                    "suite": "java", "stage": "protobuf", "mode": "cold",
                    "repeat": repeat, "valid": True, "metrics": str(metrics),
                })
            matrix.write_rows(results, root)
            with (root / "app-build-calls.csv").open(newline="", encoding="utf-8") as source:
                rows = list(csv.DictReader(source))
        self.assertEqual([int(row["repeat"]) for row in rows], [1, 2, 3, 4])


class CacheEnvironmentTests(unittest.TestCase):
    def test_base_environment_sets_stage_metadata_and_only_flatbuffers_native_root(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            common = {
                "root": root / "stage",
                "clava": root / "stage/clava",
                "native_root": root / "native",
                "wire": "flatbuffers",
            }
            with patch.dict(os.environ, {
                "FLAT_NATIVE": "stale-native",
                "JDK_JAVA_OPTIONS": "stale-jdk-options",
                "_JAVA_OPTIONS": "stale-java-options",
                "DEADLINE_JFR_PATH": "stale-recording.jfr",
                "DEADLINE_JFR_SETTINGS": "profile",
            }, clear=False):
                flat = matrix.base_environment(
                    {**common, "key": "flatbuffers"}, "java", "warm", 3,
                    root / "run", root / "calls.jsonl", root / "tmp", root / "overlay.jar", root / "out",
                )
                text = matrix.base_environment(
                    {**common, "key": "ccache-text"}, "java", "direct", 1,
                    root / "run", root / "calls.jsonl", root / "tmp", root / "overlay.jar", root / "out",
                )
        self.assertEqual(flat["FLAT_NATIVE"], str(root / "native"))
        self.assertNotIn("FLAT_NATIVE", text)
        self.assertEqual(flat["APP_BUILD_SUITE"], "java")
        self.assertEqual(flat["APP_BUILD_STAGE"], "flatbuffers")
        self.assertEqual(flat["APP_BUILD_MODE"], "warm")
        self.assertEqual(flat["APP_BUILD_REPEAT"], "3")
        self.assertIn("-Dclava.astWire=flatbuffers", flat["JAVA_TOOL_OPTIONS"])
        for name in ("JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "DEADLINE_JFR_PATH", "DEADLINE_JFR_SETTINGS"):
            self.assertNotIn(name, flat)

    def test_cache_mode_flags_and_directory_transitions_are_explicit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            stage = {"key": "protobuf", "cache": True}

            direct_dir = root / "direct-cache"
            direct_dir.mkdir()
            environment = {"CCACHE_DISABLE": "stale"}
            matrix.comparison.configure_cache_mode(stage, "direct", True, environment, direct_dir)
            self.assertEqual(environment["CCACHE_DISABLE"], "true")
            self.assertFalse(direct_dir.exists())

            cold_dir = root / "cold-cache"
            cold_dir.mkdir()
            environment = {"CCACHE_DISABLE": "stale"}
            matrix.comparison.configure_cache_mode(stage, "cold", True, environment, cold_dir)
            self.assertNotIn("CCACHE_DISABLE", environment)
            self.assertFalse(cold_dir.exists())

            warm_seed_dir = root / "warm-seed"
            warm_seed_dir.mkdir()
            environment = {"CCACHE_DISABLE": "stale"}
            matrix.comparison.configure_cache_mode(stage, "warm", False, environment, warm_seed_dir)
            self.assertNotIn("CCACHE_DISABLE", environment)
            self.assertFalse(warm_seed_dir.exists())

            warm_measure_dir = root / "warm-measure"
            warm_measure_dir.mkdir()
            environment = {"CCACHE_DISABLE": "stale"}
            with patch.object(matrix.comparison, "zero_ccache_stats") as zero_stats:
                matrix.comparison.configure_cache_mode(stage, "warm", True, environment, warm_measure_dir)
            self.assertNotIn("CCACHE_DISABLE", environment)
            self.assertTrue(warm_measure_dir.is_dir())
            zero_stats.assert_called_once_with(warm_measure_dir)

            no_cache_environment = {"CCACHE_DISABLE": "stale"}
            matrix.comparison.configure_cache_mode(
                {"key": "before-cache", "cache": False}, "direct", True,
                no_cache_environment, root / "unused-cache",
            )
            self.assertNotIn("CCACHE_DISABLE", no_cache_environment)


class CellValidationTests(unittest.TestCase):
    @staticmethod
    def _row(overlay: Path, **changes):
        overlay.parent.mkdir(parents=True, exist_ok=True)
        if not overlay.is_file():
            overlay.write_bytes(b"test-only overlay")
        row = {
            "input_sources": ["/source/main.cpp"],
            "resolved_sources": [{
                "path": "/source/main.cpp",
                "sha256": "a" * 64,
            }],
            "compiler_options": [],
            "parser_config": {},
            "parser_class_origin": str(overlay),
            "helper_class_origin": str(overlay),
            "overlay_sha256": matrix.sha256_file(overlay),
            "jvm_input_arguments": [],
            "jvm_max_memory_bytes": 512 * 1024 * 1024,
            "metadata_complete": True,
            "metadata_errors": [],
            "elapsed_ms": 2.5,
            "valid": True,
            "app_returned_null": False,
            "context_id": "ctx-1",
        }
        row.update(changes)
        return row

    def _execute_mocked_cell(self, root: Path, row: dict, *, process_code: int = 0):
        overlay = root / "overlay.jar"
        stage = {
            "key": "ccache-text",
            "root": root / "stage",
            "clava": root / "stage/clava",
            "runtime": root / "stage/clava/Clava-JS/java-binaries",
            "native_root": root / "native",
            "wire": "text-json",
            "cache": True,
            "cache_enabled": True,
            "overlay": {"overlay_jar": overlay},
        }

        def fake_run(command, cwd, env, stdout, stderr, check):
            metrics = Path(env["APP_BUILD_METRICS_PATH"])
            metrics.write_text(json.dumps(row) + "\n", encoding="utf-8")
            return SimpleNamespace(returncode=process_code)

        patches = (
            patch.object(matrix, "write_js_config", return_value=root / "vitest.config.ts"),
            patch.object(matrix.comparison, "configure_cache_mode"),
            patch.object(matrix.comparison, "cache_validation", return_value={"passed": True}),
            patch.object(matrix, "check_test_counts", return_value=matrix.EXPECTED_TESTS["clava-js"]),
            patch.object(matrix.subprocess, "run", side_effect=fake_run),
        )
        with patches[0], patches[1], patches[2], patches[3], patches[4]:
            return matrix.execute_cell(
                stage, "clava-js", "cold", 1, root / "out", 1, True
            )

    def test_accepts_timed_call_only_when_origin_and_result_are_valid(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            result = self._execute_mocked_cell(root, self._row(root / "overlay.jar"))
            self.assertEqual(result["repeat"], 1)
            self.assertEqual(result["app_calls"], 1)
            self.assertEqual(result["app_returned_null"], 0)
            self.assertEqual(result["app_elapsed_ms"], 2.5)

    def test_class_origin_fails_closed_when_missing_or_from_another_jar(self):
        cases = (
            {"parser_class_origin": ""},
            {"parser_class_origin": "/other/parser.jar"},
            {"helper_class_origin": ""},
            {"helper_class_origin": "/other/helper.jar"},
        )
        for change in cases:
            with self.subTest(change=change), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                with self.assertRaises(RuntimeError):
                    self._execute_mocked_cell(root, self._row(root / "overlay.jar", **change))

    def test_overlay_digest_and_jvm_policy_fail_closed(self):
        cases = (
            {"overlay_sha256": "0" * 64},
            {"jvm_input_arguments": None},
            {"jvm_input_arguments": ["-javaagent:coverage.jar"]},
            {"jvm_input_arguments": ["-XX:+DisableExplicitGC"]},
        )
        for change in cases:
            with self.subTest(change=change), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                with self.assertRaises(RuntimeError):
                    self._execute_mocked_cell(root, self._row(root / "overlay.jar", **change))

        with tempfile.TemporaryDirectory() as directory:
            overlay = Path(directory) / "overlay.jar"
            row = self._row(overlay, jvm_max_memory_bytes=1024)
            with self.assertRaises(RuntimeError):
                matrix.validate_capture_rows([row], overlay, "java", "protobuf")

    def test_source_hash_and_parser_configuration_errors_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            overlay = Path(directory) / "app-build-overlay.jar"
            good = self._row(overlay)
            bad_rows = (
                {**good, "resolved_sources": [{"path": "/src/a.cpp", "sha256_error": "permission denied"}]},
                {**good, "resolved_sources": [{"path": "/src/a.cpp", "sha256": ""}]},
                {**good, "resolved_sources": [{"path": "/src/a.cpp", "sha256": "not-a-sha256"}]},
                {**good, "resolved_sources": [{"resolution_error": "source enumeration failed"}]},
                {**good, "metadata_complete": False},
                {**good, "metadata_errors": ["input path hash failed"]},
                {**good, "parser_config": {"resource_dir_error": "inaccessible"}},
                {**good, "parser_config": []},
            )
            for row in bad_rows:
                with self.subTest(row=row):
                    with self.assertRaises(RuntimeError):
                        matrix.validate_capture_rows([row], overlay, "java", "protobuf")

    def test_group_empty_app_is_valid_when_its_source_manifest_is_complete(self):
        with tempfile.TemporaryDirectory() as directory:
            overlay = Path(directory) / "app-build-overlay.jar"
            row = self._row(overlay, input_sources=[], resolved_sources=[])
            matrix.validate_capture_rows([row], overlay, "java", "protobuf")

    def test_rejects_null_invalid_negative_and_nonfinite_app_rows(self):
        invalid_rows = (
            {"app_returned_null": True},
            {"valid": False},
            {"failure_reason": "parser call failed"},
            {"elapsed_ms": -0.1},
            {"elapsed_ms": float("nan")},
            {"elapsed_ms": float("inf")},
            {"elapsed_ms": "2.5"},
        )
        for change in invalid_rows:
            with self.subTest(change=change), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                with self.assertRaises(RuntimeError):
                    self._execute_mocked_cell(root, self._row(root / "overlay.jar", **change))

    def test_test_count_guard_rejects_failed_suite_process(self):
        with tempfile.TemporaryDirectory() as directory:
            run_dir = Path(directory)
            assertions = ([{"status": "passed"}] * 158
                          + [{"status": "skipped"}] * 6)
            (run_dir / "vitest.json").write_text(
                json.dumps({"assertionResults": assertions}), encoding="utf-8"
            )
            with self.assertRaises(RuntimeError):
                matrix.check_test_counts("clava-js", run_dir, 1)


class MainSmokeTests(unittest.TestCase):
    def _stages(self, root: Path):
        stages = {}
        for key in matrix.STAGE_KEYS:
            dumper = root / "native" / key / "tool"
            dumper.parent.mkdir(parents=True, exist_ok=True)
            dumper.write_bytes(key.encode("utf-8"))
            stage_root = root / "stages" / key
            stages[key] = {
                "key": key,
                "root": stage_root,
                "clava": stage_root / "clava",
                "dumper": dumper,
                "native_root": dumper.parent,
                "wire": "text-json" if key == "ccache-text" else key,
                "cache_enabled": key != "before-cache",
                "parser_jar_sha256": "frozen-parser-digest",
                "overlay": None,
            }
        return stages

    @staticmethod
    def _build_overlays(stages, output_root, selected_keys):
        for key in selected_keys:
            overlay = output_root / "overlays" / f"{key}.jar"
            overlay.parent.mkdir(parents=True, exist_ok=True)
            overlay.write_bytes(f"overlay:{key}".encode("utf-8"))
            stages[key]["overlay"] = {
                "overlay_jar": overlay,
                "overlay_sha256": matrix.sha256_file(overlay),
                "provenance": f"provenance:{key}",
                "provenance_sha256": "provenance-digest",
            }

    @staticmethod
    def _execute_cell(stage, suite, mode, repeat, output_root, ordinal, measured):
        metrics = output_root / f"calls-{ordinal}.jsonl"
        metrics.write_text(json.dumps({
            "suite": suite,
            "stage": stage["key"],
            "mode": mode,
            "repeat": repeat,
            "group_id": f"{suite}-group",
            "group_fingerprint": f"{suite}-fingerprint",
            "normalized_workload": {"suite": suite, "source": "$STAGE/source.cpp"},
            "elapsed_ms": 1.25,
            "source_count": 1,
            "valid": True,
            "app_returned_null": False,
        }) + "\n", encoding="utf-8")
        cache_dir = output_root / "cache" / suite / stage["key"]
        cache_dir.mkdir(parents=True, exist_ok=True)
        (cache_dir / "seed.txt").write_text("cache", encoding="utf-8")
        return {
            "suite": suite,
            "stage": stage["key"],
            "mode": mode,
            "repeat": repeat,
            "measured": measured,
            "valid": True,
            "app_calls": 1,
            "app_elapsed_ms": 1.25,
            "app_returned_null": 0,
            "metrics": str(metrics),
            "run_dir": str(output_root / f"run-{ordinal}"),
        }

    def _write_frozen_input(self, root: Path) -> Path:
        frozen_root = root / "frozen"
        manifest = frozen_root / "enriched-final-verified/js-results.json"
        manifest.parent.mkdir(parents=True, exist_ok=True)
        manifest.write_text("{}\n", encoding="utf-8")
        return frozen_root

    def test_preflight_main_writes_complete_manifest_and_call_csv(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            frozen_root = self._write_frozen_input(root)
            output_root = root / "preflight-output"
            stages = self._stages(root)
            with patch.object(sys, "argv", [
                "run_app_build_matrix.py", "--phase", "preflight",
                "--frozen-root", str(frozen_root), "--output-root", str(output_root),
            ]), patch.object(matrix, "read_plan", return_value={}), \
                    patch.object(matrix, "stage_records", return_value=stages), \
                    patch.object(matrix, "build_overlays", side_effect=self._build_overlays), \
                    patch.object(matrix, "execute_cell", side_effect=self._execute_cell):
                self.assertEqual(matrix.main(), 0)

            plan = json.loads((output_root / "plan.json").read_text(encoding="utf-8"))
            preflight = json.loads((output_root / "app-build-preflight.json").read_text(encoding="utf-8"))
            with (output_root / "app-build-calls.csv").open(newline="", encoding="utf-8") as source:
                calls = list(csv.DictReader(source))
            self.assertEqual(plan["completed"], 8)
            self.assertEqual(set(plan["stages"]), set(matrix.STAGE_KEYS))
            self.assertTrue(preflight["valid"])
            self.assertEqual(len(calls), 8)
            self.assertFalse((output_root / "app-build-matrix.json").exists())

    def test_measure_main_writes_requested_report_payload_for_four_repeats(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            frozen_root = self._write_frozen_input(root)
            output_root = root / "measure-output"
            preflight_path = root / "preflight.json"
            preflight_path.write_text(json.dumps({
                "valid": True,
                "workload_counters": {
                    suite: {f"{suite}-group": 1} for suite in matrix.SUITES
                },
                "context_patterns": {suite: [] for suite in matrix.SUITES},
            }), encoding="utf-8")
            report_path = root / "requested-report.json"
            stages = self._stages(root)

            def cache_dir_for(stage, suite, requested_output_root):
                return requested_output_root / "cache" / suite / stage["key"]

            with patch.object(sys, "argv", [
                "run_app_build_matrix.py", "--phase", "measure",
                "--frozen-root", str(frozen_root), "--output-root", str(output_root),
                "--preflight-manifest", str(preflight_path), "--report-payload", str(report_path),
                "--host-lock-confirmed",
            ]), patch.object(matrix, "read_plan", return_value={}), \
                    patch.object(matrix, "stage_records", return_value=stages), \
                    patch.object(matrix, "build_overlays", side_effect=self._build_overlays), \
                    patch.object(matrix, "execute_cell", side_effect=self._execute_cell), \
                    patch.object(matrix, "cache_dir_for", side_effect=cache_dir_for), \
                    patch.object(matrix, "schedule", return_value=[("java", "warm", {"key": "ccache-text"})]):
                self.assertEqual(matrix.main(), 0)

            plan = json.loads((output_root / "plan.json").read_text(encoding="utf-8"))
            report = json.loads(report_path.read_text(encoding="utf-8"))
            with (output_root / "app-build-calls.csv").open(newline="", encoding="utf-8") as source:
                calls = list(csv.DictReader(source))
            self.assertTrue(plan["valid"])
            self.assertEqual(plan["completed_rounds"], 4)
            self.assertEqual(plan["app_call_count"], 4)
            self.assertEqual(report["repeat_count"], 4)
            self.assertEqual([row["repeat"] for row in report["rows"]], [1, 2, 3, 4])
            self.assertEqual([row["repeat"] for row in calls], ["1", "2", "3", "4"])
            self.assertEqual(report["rows"][0]["group_id"], "java-group")


if __name__ == "__main__":
    unittest.main()
