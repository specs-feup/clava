from __future__ import annotations

import csv
import io
import unittest

import analyze_js_workload_components as analysis


SHA_A = "a" * 64
SHA_B = "b" * 64
SHA_C = "c" * 64


def record(ordinal: int, outcome: str, elapsed_ms: float, *, stage: str,
           repeat: int, basenames: list[str] | None = None,
           hashes: list[str] | None = None, source_sha: str = SHA_C,
           options_sha: str = SHA_A) -> dict:
    basenames = list(basenames or [])
    hashes = list(hashes or [])
    return {
        "record_type": "full_parse",
        "content_hash_phase": "after_parse",
        "suite": "clava-js",
        "mode": "direct",
        "stage": stage,
        "repeat": repeat,
        "call_ordinal": ordinal,
        "outcome": outcome,
        "identity_sha256": SHA_B,
        "options_sha256": options_sha,
        "source_content_sha256": source_sha,
        "input_count": len(basenames),
        "input_basenames": basenames,
        "input_content_sha256": hashes,
        "elapsed_ms": elapsed_ms,
    }


def full_pair_rows(stage: str, repeat: int, app_ms: float, null_ms: float) -> list[dict]:
    return [record(ordinal, "app" if ordinal <= 170 else "null",
                   app_ms if ordinal <= 170 else null_ms,
                   stage=stage, repeat=repeat)
            for ordinal in range(1, 301)]


def result(stage: str, elapsed_s: float, native_sha: str) -> dict:
    return {
        "stage": stage,
        "suite": "clava-js",
        "mode": "direct",
        "repeat": 1,
        "total_tests": 164,
        "passed_tests": 158,
        "failed_tests": 0,
        "skipped_tests": 6,
        "elapsed_s": elapsed_s,
        "native_binary_sha256": native_sha,
        "runtime_parser_jar_sha256": SHA_B,
    }


class CompareCallsTest(unittest.TestCase):
    def test_matches_content_and_basename_pairs_without_requiring_order(self):
        text = [record(1, "app", 10.0, stage="ccache-text", repeat=1,
                       basenames=["a.c", "b.h"], hashes=[SHA_A, SHA_B]),
                record(2, "null", 20.0, stage="ccache-text", repeat=1)]
        protobuf = [record(1, "app", 11.0, stage="protobuf", repeat=1,
                           basenames=["b.h", "a.c"], hashes=[SHA_B, SHA_A],
                           source_sha=SHA_B, options_sha=SHA_C),
                    record(2, "null", 19.0, stage="protobuf", repeat=1,
                           source_sha=SHA_B, options_sha=SHA_C)]

        counts = analysis.compare_call_logs(text, protobuf)

        self.assertEqual(counts["matched_call_positions"], 2)
        self.assertEqual(counts["matched_input_content_basename_sets"], 2)
        self.assertEqual(counts["matched_outcomes"], 2)
        self.assertEqual(counts["multisource_calls"], 1)
        self.assertEqual(counts["multisource_order_differences"], 1)
        self.assertEqual(counts["source_content_sha256_differences"], 2)
        self.assertEqual(counts["raw_options_sha256_differences"], 2)

    def test_rejects_changed_input_hash_or_outcome(self):
        text = [record(1, "app", 1.0, stage="ccache-text", repeat=1,
                       basenames=["a.c"], hashes=[SHA_A])]
        changed_input = [record(1, "app", 1.0, stage="protobuf", repeat=1,
                                basenames=["a.c"], hashes=[SHA_B])]
        with self.assertRaisesRegex(analysis.AnalysisError, "input content/basename set differs"):
            analysis.compare_call_logs(text, changed_input)

        changed_outcome = [record(1, "null", 1.0, stage="protobuf", repeat=1,
                                  basenames=["a.c"], hashes=[SHA_A])]
        with self.assertRaisesRegex(analysis.AnalysisError, "parse outcome differs"):
            analysis.compare_call_logs(text, changed_outcome)

    def test_rejects_malformed_input_fingerprint(self):
        malformed = record(1, "app", 1.0, stage="ccache-text", repeat=1,
                           basenames=["/private/a.c"], hashes=[SHA_A])
        with self.assertRaisesRegex(analysis.AnalysisError, "invalid input basename"):
            analysis.compare_call_logs([malformed], [malformed])


class ComponentsAndRenderingTest(unittest.TestCase):
    def test_negative_half_centisecond_rounds_away_from_zero(self):
        self.assertEqual(analysis._seconds(-0.685), "-0.69 s")

    def test_components_are_computed_from_rows_and_add_to_command_delta(self):
        text_rows = full_pair_rows("ccache-text", 1, 10.0, 5.0)
        protobuf_rows = full_pair_rows("protobuf", 1, 12.0, 4.0)
        match = analysis.compare_call_logs(text_rows, protobuf_rows)
        pair = analysis.analyze_pair(
            result("ccache-text", 4.35, SHA_A),
            result("protobuf", 5.06, SHA_C),
            text_rows, protobuf_rows, match, 1,
        )

        self.assertAlmostEqual(pair["app_delta_pb_minus_text_s"], 0.34)
        self.assertAlmostEqual(pair["null_delta_pb_minus_text_s"], -0.13)
        self.assertAlmostEqual(pair["remainder_delta_pb_minus_text_s"], 0.50)
        self.assertAlmostEqual(pair["suite_delta_pb_minus_text_s"], 0.71)
        self.assertAlmostEqual(
            pair["app_delta_pb_minus_text_s"]
            + pair["null_delta_pb_minus_text_s"]
            + pair["remainder_delta_pb_minus_text_s"],
            pair["suite_delta_pb_minus_text_s"],
        )

    def test_outputs_are_responsive_sanitized_and_have_two_csv_pairs(self):
        rows = full_pair_rows("ccache-text", 1, 10.0, 5.0)
        protobuf_rows = full_pair_rows("protobuf", 1, 12.0, 4.0)
        match = analysis.compare_call_logs(rows, protobuf_rows)
        first = analysis.analyze_pair(
            result("ccache-text", 4.35, SHA_A), result("protobuf", 5.06, SHA_C),
            rows, protobuf_rows, match, 1,
        )
        second = dict(first, pair=2)
        provenance = {
            "source_results_sha256": SHA_A,
            "source_execution_manifest_sha256": SHA_B,
            "source_outer_parse_sha256": SHA_C,
        }
        csv_text = analysis.render_csv([first, second], provenance)
        parsed_csv = list(csv.DictReader(io.StringIO(csv_text)))
        runtime = {
            "Text": {"native_sha256": SHA_A, "parser_jar_sha256": SHA_B,
                     "runtime_manifest_sha256": SHA_C},
            "Protobuf": {"native_sha256": SHA_C, "parser_jar_sha256": SHA_A,
                         "runtime_manifest_sha256": SHA_B},
        }
        html_text = analysis.render_html([first, second], provenance, runtime)

        self.assertEqual(len(parsed_csv), 2)
        self.assertIn('viewBox="0 0 360 420"', html_text)
        self.assertIn('name="viewport"', html_text)
        self.assertIn("Protobuf faster", html_text)
        self.assertIn("Native/runtime hashes differ.", html_text)
        self.assertNotIn("/home/", csv_text + html_text)
        self.assertNotIn("/private/", csv_text + html_text)


if __name__ == "__main__":
    unittest.main()
