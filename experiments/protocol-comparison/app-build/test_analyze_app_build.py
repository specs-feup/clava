from __future__ import annotations

import unittest

from analyze_app_build import analyze
from render_app_build import STAGES, TIMING_BOUNDARY


def fixture() -> dict:
    groups = [
        {"suite": suite, "group_id": f"group-{suite}-{index}", "source_count": index}
        for suite in ("clava-js", "java")
        for index in (0, 1)
    ]
    rows = []
    for group in groups:
        for mode in ("direct", "cold", "warm"):
            stages = STAGES if mode == "direct" else STAGES[1:]
            for stage in stages:
                for repeat in range(1, 5):
                    base = 1000 + repeat * 100 + group["source_count"] * 10
                    stage_delta = {
                        "before-cache": 250,
                        "ccache-text": 0,
                        "protobuf": 100,
                        "flatbuffers": -50,
                    }[stage]
                    mode_delta = {"direct": 0, "cold": 50, "warm": -200}[mode]
                    rows.append({
                        "suite": group["suite"],
                        "stage": stage,
                        "mode": mode,
                        "repeat": repeat,
                        "group_id": group["group_id"],
                        "elapsed_ms": base + stage_delta + mode_delta,
                        "valid": True,
                        "app_returned_null": False,
                    })
    return {
        "schema_version": 1,
        "timing_boundary": TIMING_BOUNDARY,
        "repeat_count": 4,
        "forced_gc": False,
        "startup_included": False,
        "groups": groups,
        "rows": rows,
    }


class AnalyzeAppBuildTests(unittest.TestCase):
    def test_totals_and_requested_paired_contrasts_use_rounds(self) -> None:
        result = analyze(fixture())
        totals = {
            (row["suite"], row["mode"], row["stage"]): row
            for row in result["suite_mode_stage_totals"]
        }
        self.assertEqual(len(totals), 20)
        self.assertEqual(totals[("java", "direct", "ccache-text")]["repeat_totals_ms"],
                         [2210.0, 2410.0, 2610.0, 2810.0])

        contrasts = {
            (row["suite"], row["comparison"]): row
            for row in result["contrasts"]
        }
        protobuf = contrasts[("java", "protobuf-minus-text (direct)")]
        self.assertEqual(
            [row["delta_ms_candidate_minus_reference"] for row in protobuf["per_repeat"]],
            [200.0] * 4,
        )
        self.assertEqual(protobuf["summary"]["positive_rounds"], 4)
        self.assertEqual(protobuf["group_direction_counts_by_median_delta"]["positive"], 2)

        flat_proto = contrasts[("java", "flatbuffers-minus-protobuf (warm)")]
        self.assertEqual(
            [row["delta_ms_candidate_minus_reference"] for row in flat_proto["per_repeat"]],
            [-300.0] * 4,
        )
        self.assertEqual(
            [row["group_id"] for row in flat_proto["groups_ranked_by_absolute_median_delta"]],
            ["group-java-0", "group-java-1"],
        )

        warm_cache = contrasts[("java", "warm-minus-direct (ccache-text)")]
        self.assertEqual(warm_cache["summary"]["median_delta_ms_candidate_minus_reference"], -400.0)
        baseline = contrasts[("java", "before-cache-minus-text (direct)")]
        self.assertEqual(baseline["summary"]["median_delta_ms_candidate_minus_reference"], 500.0)
        self.assertIn("No significance tests", result["inference"])


if __name__ == "__main__":
    unittest.main()
