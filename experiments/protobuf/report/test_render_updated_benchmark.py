"""Tests for the updated Protobuf benchmark report input contract."""

from __future__ import annotations

import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import unittest


SCRIPT = Path(__file__).with_name("render_updated_benchmark.py")
SPEC = importlib.util.spec_from_file_location("render_updated_benchmark", SCRIPT)
REPORT = importlib.util.module_from_spec(SPEC)
assert SPEC is not None and SPEC.loader is not None
SPEC.loader.exec_module(REPORT)


def fixture_prior():
    app_rows = []
    wall_rows = []
    for suite in REPORT.SUITES:
        for mode in REPORT.MODES:
            stages = REPORT.STAGES if mode == "direct" else REPORT.STAGES[1:]
            for stage in stages:
                for repeat in REPORT.REPEATS:
                    offset = repeat / 100 + REPORT.STAGES.index(stage) / 10
                    app_rows.append({
                        "suite": suite, "stage": stage, "mode": mode, "repeat": repeat,
                        "app_elapsed_ms": 1000 + offset, "app_calls": 42,
                        "measurement_date": "2026-10-07", "reused": True,
                        "command": "/home/private/control-command",
                    })
                    if suite == "java":
                        counts = (116, 116, 0, 0)
                    else:
                        counts = (164, 158, 0, 6)
                    wall_rows.append({
                        "suite": suite, "stage": stage, "mode": mode, "repeat": repeat,
                        "elapsed_s": 60 + offset, "total_tests": counts[0],
                        "passed_tests": counts[1], "failed_tests": counts[2],
                        "skipped_tests": counts[3], "return_code": 0, "valid": True,
                        "measured": True, "measurement_date": "2026-10-07", "reused": True,
                        "raw_log": "/tmp/private/control.log",
                    })
    return {"app_plot_rows": app_rows, "wall_plot_rows": wall_rows}


def fixture_updated():
    observations = []
    for phase in ("app", "wall"):
        for suite in REPORT.SUITES:
            for mode in REPORT.MODES:
                for repeat in REPORT.REPEATS:
                    row = {
                        "phase": phase, "suite": suite, "stage": "protobuf", "mode": mode,
                        "repeat": repeat, "valid": True, "measured": True,
                        # App-phase wall_s is deliberately unrelated and must not be rendered.
                        "wall_s": 9999 if phase == "app" else 40 + repeat / 100,
                        "raw_log": "/home/private/updated.log",
                    }
                    if phase == "app":
                        row.update({
                            "app_elapsed_ms": 2000 + repeat, "app_calls": 216 if suite == "java" else 170,
                            "syntax_only_calls": 0 if suite == "java" else 130,
                        })
                    else:
                        counts = {"total_tests": 116, "passed_tests": 116,
                                  "failed_tests": 0, "skipped_tests": 0}
                        if suite == "clava-js":
                            counts = {"total_tests": 164, "passed_tests": 162,
                                      "failed_tests": 0, "skipped_tests": 2}
                        row["test_counts"] = counts
                    observations.append(row)
    release = fixture_release_manifest()
    release_bytes = json.dumps(release, separators=(",", ":")).encode()
    native_sha = release["assets"][2]["sha256"]
    return {
        "status": "complete", "created_utc": "2026-10-07T12:00:00Z",
        "completed_utc": "2026-10-07T13:00:00+00:00", "observations": observations,
        "native_sha256": {"protobuf": native_sha},
        "sources": {"protobuf": {
            "selected_release": {
                "manifest_sha256": hashlib.sha256(release_bytes).hexdigest(),
                "tool_sha256": native_sha,
                "tool_asset": {"sha256": native_sha},
            },
            "repositories": {
                name: {"revision": f"{index:040x}"}
                for index, name in enumerate(("clava", "specs-java-libs", "lara-framework", "clang-dumper"), 1)
            },
            "native_repository": {"revision": f"{4:040x}"},
            "clang_ast_parser_source_tree_sha256": "a" * 64,
        }},
    }


def fixture_release_manifest():
    schema_sha = "b" * 64
    descriptor_sha = "c" * 64
    tool_sha = hashlib.sha256(b"fixture native tool").hexdigest()
    return {
        "protocol": {
            "id": "clava-ast-wire", "major": 1, "minor": 1,
            "schema_sha256": schema_sha, "descriptor_sha256": descriptor_sha,
        },
        "assets": [
            {"kind": "protocol", "filename": "clang-dumper-ast-wire.proto", "sha256": schema_sha},
            {"kind": "protocol", "filename": "clang-dumper-ast-wire.pb", "sha256": descriptor_sha},
            {"kind": "tool", "filename": "tool", "sha256": tool_sha},
        ],
    }


class UpdatedReportTest(unittest.TestCase):
    def test_replaces_only_protobuf_rows_and_keeps_control_rows_unchanged(self):
        prior = fixture_prior()
        updated = REPORT.validate_updated_manifest(fixture_updated())
        app_rows, wall_rows = REPORT.combine_rows(prior, updated)
        self.assertEqual(
            [row for row in prior["app_plot_rows"] if row["stage"] != "protobuf"],
            [row for row in app_rows if row["stage"] != "protobuf"],
        )
        self.assertEqual(
            [row for row in prior["wall_plot_rows"] if row["stage"] != "protobuf"],
            [row for row in wall_rows if row["stage"] != "protobuf"],
        )
        self.assertEqual(24, sum(row["stage"] == "protobuf" for row in app_rows))
        self.assertEqual(24, sum(row["stage"] == "protobuf" for row in wall_rows))

    def test_app_phase_wall_time_is_never_used_as_command_wall_time(self):
        html = REPORT.render_report(fixture_prior(), fixture_updated())
        self.assertNotIn("9999", html)
        self.assertIn("40.01", html)

    def test_csv_does_not_apply_new_session_dates_to_reused_controls(self):
        updated = REPORT.validate_updated_manifest(fixture_updated())
        app_rows, wall_rows = REPORT.combine_rows(fixture_prior(), updated)
        rows = REPORT._csv_data(app_rows, wall_rows, updated)
        header = rows[0]
        created_index = header.index("session_created_utc")
        reused = next(row for row in rows[1:] if row[2] == "ccache-text")
        current = next(row for row in rows[1:] if row[2] == "protobuf")
        self.assertEqual("", reused[created_index])
        self.assertEqual("2026-10-07 12:00:00 UTC", current[created_index])

    def test_standalone_report_has_dark_theme_sources_and_csv_export(self):
        html = REPORT.render_report(fixture_prior(), fixture_updated())
        self.assertIn('<html class="dark"', html)
        self.assertIn("html { color-scheme:light;", html)
        self.assertIn("html.dark { color-scheme:dark;", html)
        self.assertNotIn("prefers-color-scheme", html)
        self.assertNotIn("theme-toggle", html)
        self.assertIn("Created 2026-10-07 12:00:00 UTC", html)
        self.assertIn("Completed 2026-10-07 13:00:00 UTC", html)
        self.assertIn("Reused historical controls", html)
        self.assertIn("Download chart rows as CSV", html)
        self.assertIn("app-chart", html)
        self.assertEqual(4, html.count("<svg"))
        self.assertIn("All cache sections share one non-zero time axis", html)
        self.assertIn("updated Protobuf session", html)
        self.assertNotIn("paired percentage", html.lower())
        self.assertNotIn("file://", html.lower())
        self.assertNotIn("/home/private", html)
        self.assertNotIn("/tmp/private", html)

    def test_release_provenance_is_matched_and_renders_only_safe_hashes(self):
        updated = fixture_updated()
        manifest = fixture_release_manifest()
        manifest_sha = hashlib.sha256(json.dumps(manifest, separators=(",", ":")).encode()).hexdigest()
        report = REPORT.render_report(fixture_prior(), updated, release_manifest=manifest,
                                      release_manifest_sha256=manifest_sha)
        self.assertIn("Runtime source and wire provenance", report)
        self.assertIn("clang-dumper-ast-wire", report)
        self.assertIn("b" * 64, report)
        self.assertIn("c" * 64, report)
        self.assertIn("a" * 64, report)
        self.assertNotIn("/home/", report)
        self.assertNotIn("file://", report)

        with self.assertRaisesRegex(ValueError, "does not match the selected measurement release"):
            REPORT.render_report(fixture_prior(), updated, release_manifest=manifest,
                                 release_manifest_sha256="d" * 64)

        mismatch = copy.deepcopy(manifest)
        mismatch["assets"][0]["sha256"] = "d" * 64
        mismatch_bytes = json.dumps(mismatch, separators=(",", ":")).encode()
        updated["sources"]["protobuf"]["selected_release"]["manifest_sha256"] = hashlib.sha256(
            mismatch_bytes
        ).hexdigest()
        with self.assertRaisesRegex(ValueError, "schema hash does not match"):
            REPORT.validate_release_provenance(updated, mismatch, hashlib.sha256(mismatch_bytes).hexdigest())

    def test_rejects_missing_duplicate_or_invalid_observations(self):
        missing = fixture_updated()
        missing["observations"].pop()
        with self.assertRaisesRegex(ValueError, "exactly 48"):
            REPORT.validate_updated_manifest(missing)

        duplicate = fixture_updated()
        duplicate["observations"][-1] = copy.deepcopy(duplicate["observations"][0])
        with self.assertRaisesRegex(ValueError, "duplicate"):
            REPORT.validate_updated_manifest(duplicate)

        invalid = fixture_updated()
        invalid["observations"][0]["valid"] = False
        with self.assertRaisesRegex(ValueError, "valid and measured"):
            REPORT.validate_updated_manifest(invalid)

    def test_rejects_a_failed_wall_run_even_when_manifest_says_valid(self):
        payload = fixture_updated()
        failing = next(row for row in payload["observations"] if row["phase"] == "wall")
        failing["test_counts"]["passed_tests"] -= 1
        failing["test_counts"]["failed_tests"] = 1
        with self.assertRaisesRegex(ValueError, "failed tests"):
            REPORT.validate_updated_manifest(payload)

    def test_optional_memory_diagnostic_is_separate_and_descriptive(self):
        memory = []
        for label in ("NAS+", "C++ templates"):
            observations = []
            for repeat in range(1, 4):
                observations.append({
                    "repeat": repeat, "return_code": 0,
                    "gnu_time": {"max_rss_kb": 1024 + repeat, "exit_status": 0},
                    "heap_phases": [{
                        "phase": "parse_released", "repeat": phase,
                        "app_collected": True, "open_parser_files": 0,
                        "mapped_paths_under_work": 0, "leftover_clang_temp_folders": 0,
                        "observer_open_parser_fd_targets": {"1": "log", "2": "time", "3": "pipe"},
                        "observed_open_parser_fd_targets": {"1": "log", "2": "time", "3": "pipe"},
                        "unexpected_open_parser_fd_targets": {},
                        "live_heap_bytes": 1000 + phase,
                        "retained_heap_bytes": 800 + phase,
                        "jvm_peak_rss_bytes": 2000 + phase, "nodes": 250,
                    } for phase in range(1, 21)],
                })
            memory.append((label, {
                "created_utc": "2026-10-07T12:00:00Z", "repeats": 3,
                "retained_heap_contract": "production command emits phase used_bytes after explicit GC",
                "peak_rss_contract": "GNU time max_rss_kb",
                "failed": [], "source": "/home/private/memory.json",
                "observations": observations,
            }))
        html = REPORT.render_report(fixture_prior(), fixture_updated(), memory)
        self.assertIn("Separate memory diagnostics", html)
        self.assertIn("C++ templates", html)
        self.assertIn("<th scope=\"row\">3</th>", html)
        self.assertIn("Open parser files", html)
        self.assertIn("JVM VmHWM", html)
        self.assertIn("GNU time max RSS", html)
        self.assertIn("fitted slope", html)
        self.assertNotIn("/home/private", html)

        unclean = copy.deepcopy(memory)
        unclean[0][1]["observations"][0]["heap_phases"][0]["mapped_paths_under_work"] = 1
        with self.assertRaisesRegex(ValueError, "mapped_paths_under_work"):
            REPORT.render_report(fixture_prior(), fixture_updated(), unclean)


if __name__ == "__main__":
    unittest.main()
