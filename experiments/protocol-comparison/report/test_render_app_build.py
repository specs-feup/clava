from __future__ import annotations

import csv
import io
import json
from pathlib import Path
import tempfile
import unittest

from render_app_build import (
    TIMING_BOUNDARY,
    aggregate,
    csv_text,
    main,
    median_or_none,
    paired_percentages,
    render_fragment,
    validate_input,
)


def fixture() -> dict:
    groups = [
        {"suite": suite, "group_id": f"PRIVATE_{suite}_GROUP_{index}", "source_count": count}
        for suite in ("clava-js", "java")
        for index, count in ((1, 3), (2, 0))
    ]
    rows = []
    for group in groups:
        suite = group["suite"]
        group_id = group["group_id"]
        for mode in ("direct", "cold", "warm"):
            stages = ("before-cache", "ccache-text", "protobuf", "flatbuffers") if mode == "direct" else ("ccache-text", "protobuf", "flatbuffers")
            for stage in stages:
                for repeat in range(1, 5):
                    rows.append({
                        "suite": suite,
                        "stage": stage,
                        "mode": mode,
                        "repeat": repeat,
                        "group_id": group_id,
                        "elapsed_ms": 1000 + 100 * repeat + (100 if stage == "protobuf" else 0),
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
        "evidence_path": "/home/secret/private-run.json",
    }


class RenderAppBuildTests(unittest.TestCase):
    def test_aggregates_all_app_calls_and_accepts_zero_source_group(self):
        data = validate_input(fixture())
        sums = aggregate(data)
        # Both Apps count, including the valid group with no source files.
        self.assertEqual(len(data["ids_by_suite"]["java"]), 2)
        self.assertEqual(sums[("java", "direct", "ccache-text")], [2200, 2400, 2600, 2800])
        self.assertEqual(sums[("java", "cold", "protobuf")], [2400, 2600, 2800, 3000])

    def test_paired_median_uses_repeatwise_percentages(self):
        payload = fixture()
        suite = "clava-js"
        ids = [group["group_id"] for group in payload["groups"] if group["suite"] == suite]
        for row in payload["rows"]:
            if row["suite"] != suite:
                continue
            if row["group_id"] != ids[0]:
                row["elapsed_ms"] = 0
            if row["mode"] == "direct" and row["stage"] == "ccache-text" and row["group_id"] == ids[0]:
                row["elapsed_ms"] = [10, 100, 100, 100][row["repeat"] - 1]
            if row["mode"] == "direct" and row["stage"] == "protobuf" and row["group_id"] == ids[0]:
                row["elapsed_ms"] = [20, 10, 50, 100][row["repeat"] - 1]
        totals = aggregate(validate_input(payload))
        effects = paired_percentages(totals, suite, "direct", "protobuf")
        self.assertEqual(effects, [100.0, -90.0, -50.0, 0.0])
        self.assertEqual(median_or_none(effects), -25.0)
        # Difference of the two repeat medians would be -65%, not this paired result.
        self.assertNotEqual(median_or_none(effects), -65.0)

    def test_zero_text_total_has_undefined_paired_percentage(self):
        payload = fixture()
        for row in payload["rows"]:
            if row["mode"] == "cold" and row["stage"] == "ccache-text":
                row["elapsed_ms"] = 0
        totals = aggregate(validate_input(payload))
        self.assertEqual(paired_percentages(totals, "java", "cold", "protobuf"), [None] * 4)

    def test_fragment_has_two_responsive_shared_scale_candles_and_no_private_ids(self):
        payload = fixture()
        fragment = render_fragment(payload)
        visible = fragment.split('<details class="app-method"', 1)[0]
        details = fragment.split('<details class="app-method"', 1)[1].split("</details>", 1)[0]
        self.assertEqual(fragment.count("<svg class=\"app-chart\" viewBox=\"0 0 360 490\""), 2)
        self.assertEqual(fragment.count('class="divider"'), 4)
        self.assertIn("grid-template-columns:repeat(2,minmax(0,1fr))", fragment)
        self.assertIn("@media (max-width:700px)", fragment)
        self.assertIn("svg.app-chart { display:block", fragment)
        self.assertIn(".app-chart .label,.app-build-timing .app-chart .mode { fill:var(--muted,#56636e); font:15px", fragment)
        self.assertIn("Non-zero axis", fragment)
        self.assertIn("Build the Clava App", fragment)
        self.assertIn("Only the work from the original C/C++ inputs to a ready-to-use App is timed", fragment)
        self.assertIn("No Gradle/JVM startup, assertions, syntax-only checks, code display or forced GC.", fragment)
        self.assertIn("<details class=\"app-method\"><summary>Timing and calculation details</summary>", fragment)
        self.assertIn("not replay or per-file timing", details)
        self.assertIn("Natural GC during the call is included", details)
        self.assertIn("TextParser/text passes and cross-file linking are inside the interval", details)
        self.assertIn("100 × (format total / same-mode Text total − 1)", details)
        self.assertNotIn("ParallelCodeParser.parse", visible)
        self.assertNotIn("Natural GC", visible)
        self.assertNotIn("100 ×", visible)
        self.assertIn("Line median; box middle half; whiskers range; dots four rounds.", fragment)
        self.assertNotIn("<details class=\"app-method\" open", fragment)
        self.assertNotIn("when the completed <code>App</code> returns", fragment)
        self.assertNotIn("Timing starts after TextParser", fragment)
        self.assertNotIn("PRIVATE_", fragment)
        self.assertNotIn("/home/secret", fragment)

    def test_csv_is_sanitized_and_contains_per_repeat_sum_not_group_ids(self):
        payload = fixture()
        output = csv_text(payload)
        self.assertNotIn("PRIVATE_", output)
        self.assertNotIn("/home/secret", output)
        rows = list(csv.DictReader(io.StringIO(output)))
        total = next(row for row in rows if row["row_type"] == "repeat_total" and row["suite"] == "Java parser" and row["stage"] == "Text"
                     and row["mode"] == "direct" and row["repeat"] == "1")
        self.assertEqual(total["app_call_count"], "2")
        self.assertEqual(total["source_count_total"], "3")
        self.assertEqual(total["elapsed_ms"], "2200.000000")
        self.assertTrue(any(row["row_type"] == "paired_median" for row in rows))
        app_call = next(row for row in rows if row["row_type"] == "app_call" and row["suite"] == "Java parser"
                        and row["stage"] == "Text" and row["mode"] == "direct"
                        and row["repeat"] == "1" and row["source_count"] == "0")
        self.assertEqual(len(app_call["group_sha256"]), 64)
        self.assertEqual(app_call["app_call_count"], "1")

    def test_cli_writes_html_fragment_and_csv(self):
        payload = fixture()
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            input_path = root / "input.json"
            output_path = root / "nested" / "fragment.html"
            csv_path = root / "nested" / "summary.csv"
            input_path.write_text(json.dumps(payload), encoding="utf-8")
            self.assertEqual(main(["--input", str(input_path), "--output", str(output_path), "--csv", str(csv_path)]), 0)
            self.assertIn("Build the Clava App", output_path.read_text(encoding="utf-8"))
            self.assertIn("paired_median", csv_path.read_text(encoding="utf-8"))

    def test_rejects_invalid_provenance_and_incomplete_or_malformed_cells(self):
        mutations = (
            lambda p: p.update(forced_gc=True),
            lambda p: p.update(startup_included=True),
            lambda p: p.update(timing_boundary="whole command"),
            lambda p: p["rows"].pop(),
            lambda p: p["rows"].append(p["rows"][0].copy()),
            lambda p: p["rows"][0].update(valid=False),
            lambda p: p["rows"][0].update(app_returned_null=True),
            lambda p: p["rows"][0].update(elapsed_ms=-1),
            lambda p: p["rows"][0].update(elapsed_ms=float("nan")),
            lambda p: p["rows"][0].update(stage="before-cache", mode="warm"),
            lambda p: p["groups"][0].update(source_count=-1),
        )
        for mutate in mutations:
            with self.subTest(mutate=mutate):
                payload = fixture()
                mutate(payload)
                with self.assertRaises(ValueError):
                    validate_input(payload)


if __name__ == "__main__":
    unittest.main()
