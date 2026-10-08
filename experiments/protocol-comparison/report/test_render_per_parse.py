"""Tests for the per-parse timing report."""

import csv
import tempfile
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

import render_per_parse


FIXTURE = Path(__file__).resolve().parent / "testdata/per_parse_fixture.csv"
RUN_FIXTURE = Path(__file__).resolve().parent / "testdata/per_parse_runs_fixture.csv"


def write_parse_fixture(path: Path, include_event_flags: bool = False) -> Path:
    with FIXTURE.open(newline="", encoding="utf-8") as source:
        reader = csv.DictReader(source)
        fieldnames = list(reader.fieldnames or []) + [
            "parse_pair_key", "pair_available", "test_id", "resource_key", "parse_pass",
        ]
        if include_event_flags:
            fieldnames += ["cache_enabled", "explicit_gc_disabled"]
        rows = list(reader)
    for row in rows:
        row["parse_pair_key"] = row.get("source_identity") or ""
        row["pair_available"] = str(bool(row.get("source_identity")) and row.get("event_valid") == "true").lower()
        row["test_id"] = f"{row.get('suite', 'unknown')}-test"
        row["resource_key"] = row.get("source_path", "")
        row["parse_pass"] = row.get("invocation_index", "")
        if include_event_flags:
            row["cache_enabled"] = "true"
            row["explicit_gc_disabled"] = "true" if row["gc_policy"] == "disabled" else "false"
    with path.open("w", newline="", encoding="utf-8") as destination:
        writer = csv.DictWriter(destination, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    return path


def write_runs_fixture(path: Path) -> Path:
    with RUN_FIXTURE.open(newline="", encoding="utf-8") as source:
        reader = csv.DictReader(source)
        fieldnames = list(reader.fieldnames or []) + [
            "ccache_adapter_events", "ccache_event_counter_match", "source_content_match",
            "parse_args_match", "source_args_match", "cache_distribution_match",
            "test_total", "test_passed", "test_failed", "test_skipped",
        ]
        rows = list(reader)
    for row in rows:
        cacheable = int(row.get("ccache_cacheable_calls") or 0)
        uncacheable = int(row.get("ccache_uncacheable_calls") or 0)
        row["ccache_adapter_events"] = str(cacheable + uncacheable)
        if row.get("suite") == "clava-js":
            row.update(test_total="164", test_passed="158", test_failed="0", test_skipped="6")
            row["expected_event_count"] = "191"
        else:
            row.update(test_total="116", test_passed="116", test_failed="0", test_skipped="0")
            row["expected_event_count"] = "247"
        for column in fieldnames:
            if column in {"ccache_event_counter_match", "source_content_match", "parse_args_match",
                          "source_args_match", "cache_distribution_match"}:
                row[column] = "true"
    with path.open("w", newline="", encoding="utf-8") as destination:
        writer = csv.DictWriter(destination, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    return path


def load_runs_fixture():
    with tempfile.TemporaryDirectory() as directory:
        fixture = write_runs_fixture(Path(directory) / "runs.csv")
        return render_per_parse.read_run_csv(fixture)


def load_parse_fixture():
    runs, _ = load_runs_fixture()
    measured_run_ids = {row["run_id"] for row in runs}
    with tempfile.TemporaryDirectory() as directory:
        fixture = write_parse_fixture(Path(directory) / "parses.csv")
        return render_per_parse.read_csv(fixture, measured_run_ids)


class PerParseReportTest(unittest.TestCase):
    def test_small_fixture_keeps_gc_policies_and_suite_pairs_separate(self):
        rows, invalid = load_parse_fixture()
        grouped = render_per_parse.group_rows(rows)
        pairs, unmatched, unavailable = render_per_parse.pair_rows(rows)

        self.assertEqual(len(rows), 18)
        self.assertEqual(len(invalid), 2)
        self.assertIn("parse row has no valid measured run", invalid[1]["reason"])
        self.assertEqual(set(grouped), {("normal", "warm"), ("disabled", "warm")})
        self.assertEqual(len(pairs[("normal", "warm")]["clava-js"]), 3)
        self.assertEqual(len(pairs[("normal", "warm")]["java"]), 2)
        self.assertEqual(len(pairs[("disabled", "warm")]["clava-js"]), 2)
        self.assertEqual(len(pairs[("disabled", "warm")]["java"]), 2)
        self.assertEqual(unmatched, {})
        self.assertEqual(unavailable, {})

        normal_clava = pairs[("normal", "warm")]["clava-js"]
        self.assertEqual([pair["delta_ms"] for pair in normal_clava], [2, -2, 10])

    def test_tukey_whiskers_retain_outliers_as_explicit_points(self):
        summary = render_per_parse.distribution([10.0, 10.0, 10.0, 10.0, 100.0])
        self.assertEqual(summary["whisker_minimum"], 10.0)
        self.assertEqual(summary["whisker_maximum"], 10.0)
        self.assertEqual(summary["outliers"], [100.0])
        self.assertEqual(summary["n"], 5)

    def test_central_delta_view_is_additional_and_counts_off_scale_points(self):
        points = [
            {"delta_ms": value, "identity": str(value), "pair_id": "r1", "repeat": "1",
             "text_ms": 100 + value, "protobuf_ms": 100 + value + value}
            for value in (-100, -1, 0, 1, 100)
        ]
        groups = [("Java parser", points)]
        self.assertTrue(render_per_parse.delta_chart_needs_central_scale(groups))
        full = ET.fromstring(render_per_parse.svg_delta_chart("Full", groups))
        central = ET.fromstring(render_per_parse.svg_delta_chart("Central", groups, central_scale=True))
        self.assertEqual(len(full.findall(".//circle")), 5)
        self.assertEqual(len(central.findall(".//circle")), 3)
        central_stats = render_per_parse.delta_chart_stats(
            groups, central_scale=True, show_points=False)
        self.assertIn("2 off scale", central_stats)
        self.assertIn("central linear scale", "".join(central.itertext()))
        self.assertFalse(render_per_parse.delta_chart_needs_central_scale([
            ("compact", [{**point, "delta_ms": index} for index, point in enumerate(points)])
        ]))

    def test_large_chart_samples_dots_but_uses_all_rows_for_candles(self):
        points = [
            {"delta_ms": float(index), "elapsed_ms": float(index + 1),
             "identity": f"source-{index}", "source_identity": f"source-{index}",
             "pair_id": "pair", "repeat": "1"}
            for index in range(1000)
        ]
        sample = render_per_parse.display_sample(points, "delta_ms", 80)
        self.assertEqual(len(sample), 80)
        self.assertEqual(sample[0]["delta_ms"], 0)
        self.assertEqual(sample[-1]["delta_ms"], 999)
        full = ET.fromstring(render_per_parse.svg_delta_chart(
            "Full range", [("Suite", points), ("Pooled", points)],
            max_points_per_group=80, point_groups={"Suite"}))
        self.assertEqual(len(full.findall(".//circle")), 80)
        delta_stats = render_per_parse.delta_chart_stats(
            [("Suite", points), ("Pooled", points)], max_points=80,
            point_groups={"Suite"})
        self.assertIn("n=1000", delta_stats)
        self.assertIn("80/1000 points", delta_stats)
        self.assertIn("points omitted", delta_stats)
        self.assertNotIn("n=1000", "".join(full.itertext()))
        central = ET.fromstring(render_per_parse.svg_delta_chart(
            "Central", [("Suite", points)], central_scale=True, show_points=False))
        self.assertEqual(len(central.findall(".//circle")), 0)
        central_stats = render_per_parse.delta_chart_stats(
            [("Suite", points)], central_scale=True, show_points=False)
        self.assertIn("0 off scale", central_stats)
        self.assertIn("n=1000", central_stats)

        runtime = ET.fromstring(render_per_parse.svg_distribution_chart(
            "Runtime", {"text": [
                {"elapsed_ms": float(index + 1), "source_identity": f"source-{index}"}
                for index in range(1000)
            ]}, max_points_per_protocol=80))
        self.assertEqual(len(runtime.findall(".//circle")), 80)
        runtime_stats = render_per_parse.distribution_chart_stats(
            {"text": [
                {"elapsed_ms": float(index + 1), "source_identity": f"source-{index}"}
                for index in range(1000)
            ]}, ("text",), max_points=80)
        self.assertIn("n=1000", runtime_stats)
        self.assertIn("80/1000 points", runtime_stats)
        self.assertNotIn("n=1000", "".join(runtime.itertext()))
        self.assertEqual(len(runtime.findall(".//circle/title")), 0)

        responsive = render_per_parse.distribution_chart_html(
            "Runtime", {"text": [{"elapsed_ms": value} for value in (1.0, 2.0)]}, ("text",))
        self.assertIn("chart-svg-wide", responsive)
        self.assertIn("chart-svg-mobile", responsive)
        self.assertIn("Distribution details", responsive)

    def test_distribution_and_paired_charts_are_well_formed_svg_with_each_sample(self):
        rows, _ = load_parse_fixture()
        normal = [row for row in rows if row["gc_policy"] == "normal"]
        distribution_svg = ET.fromstring(render_per_parse.svg_distribution_chart(
            "Normal policy", {
                protocol: [row for row in normal if row["protocol"] == protocol]
                for protocol in render_per_parse.PROTOCOL_ORDER
            }))
        self.assertEqual(len(distribution_svg.findall(".//circle")), len(normal))
        self.assertEqual(len(distribution_svg.findall(".//rect")), 2)

        pairs, _, _ = render_per_parse.pair_rows(normal)
        all_pairs = [pair for rows_by_suite in pairs[("normal", "warm")].values()
                     for pair in rows_by_suite]
        paired_svg = ET.fromstring(render_per_parse.svg_delta_chart(
            "Normal policy", [("Pooled per-parse", all_pairs)]))
        self.assertEqual(len(paired_svg.findall(".//circle")), len(all_pairs))
        self.assertTrue(any("outlier" in circle.get("class", "")
                            for circle in paired_svg.findall(".//circle")))

    def test_per_parse_csv_without_phase_requires_runs_csv_to_drop_seed_rows(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = write_parse_fixture(Path(directory) / "parses.csv")
            with self.assertRaisesRegex(ValueError, "Supply --runs-csv"):
                render_per_parse.read_csv(fixture)

    def test_ineligible_cache_events_are_retained_and_effective_gc_is_checked(self):
        run_rows, _ = load_runs_fixture()
        measured_run_ids = {row["run_id"] for row in run_rows}
        with tempfile.TemporaryDirectory() as directory:
            fixture_path = write_parse_fixture(Path(directory) / "parses.csv", include_event_flags=True)
            with fixture_path.open(newline="", encoding="utf-8") as source:
                reader = csv.DictReader(source)
                fieldnames = list(reader.fieldnames or [])
                fixture_rows = list(reader)
            fixture_rows[0]["cache_enabled"] = "false"
            fixture_rows[1]["explicit_gc_disabled"] = "true"
            with fixture_path.open("w", newline="", encoding="utf-8") as destination:
                writer = csv.DictWriter(destination, fieldnames=fieldnames)
                writer.writeheader()
                writer.writerows(fixture_rows)
            rows, invalid = render_per_parse.read_csv(fixture_path, measured_run_ids)

        reasons = {row["reason"] for row in invalid}
        self.assertIn("effective explicit-GC setting disagrees with gc_policy", reasons)
        self.assertNotIn("AST ccache adapter was not enabled for parse event", reasons)
        self.assertEqual(len(rows), 17)
        self.assertFalse(rows[0]["cache_enabled"])

    def test_html_shows_per_policy_suite_and_pooled_parse_views(self):
        rows, invalid = load_parse_fixture()
        html = render_per_parse.report_html(FIXTURE, rows, invalid)

        self.assertIn('class="parser-results"', html)
        self.assertEqual(html.count("Pooled per-parse distribution</h3>"), 2)
        self.assertIn("Clava-JS · warm cache", html)
        self.assertIn("Java parser · warm cache", html)
        self.assertIn("Pooled per-parse sample", html)
        self.assertIn("Excluded parse or command rows, including invalid, incomplete, and seed rows: 2", html)
        self.assertIn('class="evidence-details"', html)
        self.assertIn('class="pooled-details"', html)
        self.assertIn("duplicate source identity", html)
        self.assertIn("parsePrivate", html)
        self.assertIn("Positive values mean Protobuf took longer", html)
        self.assertIn("Largest per-source median changes across repeats", html)
        self.assertIn("Paired invocations: faster / tied / slower", html)
        self.assertIn("Candles use every valid event", html)
        self.assertIn("dots are a deterministic display sample, at most 250 per protocol",
                      html.lower())
        self.assertNotIn("http://", html)
        self.assertNotIn("https://", html)

    def test_duplicate_valid_condition_for_one_pair_is_rejected(self):
        rows, _ = load_parse_fixture()
        duplicate = dict(rows[0])
        rows.append(duplicate)
        with self.assertRaisesRegex(ValueError, "Duplicate valid parse event"):
            render_per_parse.pair_rows(rows)

    def test_unpairable_events_remain_in_raw_data_but_not_pair_deltas(self):
        rows, _ = load_parse_fixture()
        target = "clava-js|normal|r1"
        for row in rows:
            if row["pair_id"] == target and row["source_path"] == "src/c.c":
                row["pair_available"] = False

        pairs, unmatched, unavailable = render_per_parse.pair_rows(rows)

        self.assertEqual(len(rows), 18)
        self.assertEqual(len(pairs[("normal", "warm")]["clava-js"]), 2)
        self.assertEqual(unmatched, {})
        self.assertEqual(unavailable[("normal", "warm", "clava-js")], 2)
        self.assertEqual(len(render_per_parse.group_rows(rows)[("normal", "warm")]["clava-js"]), 6)
        pair_note = render_per_parse.pair_eligibility_summary(
            [row for row in rows if row["gc_policy"] == "normal" and row["suite"] == "clava-js"]
        )
        self.assertIn("1/3 per run × 1 runs", pair_note)
        self.assertIn("stay in the runtime candles", pair_note)

    def test_direction_fractions_and_top_source_medians_across_repeats(self):
        pairs = [
            {"parse_pair_key": "slow", "delta_ms": 8, "test_id": "slow-test", "resource_key": "res/slow", "parse_pass": "1", "source_path": "slow.c", "parse_id": "ast", "identity": "slow"},
            {"parse_pair_key": "slow", "delta_ms": 4, "test_id": "slow-test", "resource_key": "res/slow", "parse_pass": "1", "source_path": "slow.c", "parse_id": "ast", "identity": "slow"},
            {"parse_pair_key": "fast", "delta_ms": -10, "test_id": "fast-test", "resource_key": "res/fast", "parse_pass": "2", "source_path": "fast.c", "parse_id": "ast", "identity": "fast"},
            {"parse_pair_key": "tie", "delta_ms": 0, "test_id": "tie-test", "resource_key": "res/tie", "parse_pass": "3", "source_path": "tie.c", "parse_id": "ast", "identity": "tie"},
        ]

        self.assertIn("faster 1/4 (25%)", render_per_parse.direction_summary(pairs))
        self.assertIn("slower 2/4 (50%)", render_per_parse.direction_summary(pairs))
        changes = render_per_parse.top_source_changes(pairs)
        self.assertEqual(changes["slower"][0]["median_delta_ms"], 6)
        self.assertEqual(changes["slower"][0]["repeat_count"], 2)
        self.assertEqual(changes["faster"][0]["median_delta_ms"], -10)
        table = render_per_parse.source_change_table(pairs)
        self.assertIn("slow-test", table)
        self.assertIn("resource res/slow", table)
        self.assertIn("pass 1", table)

    def test_full_command_wall_delta_uses_runs_and_shared_sequential_group(self):
        run_rows, invalid = load_runs_fixture()
        suite_pairs, global_pairs, unmatched = render_per_parse.pair_run_rows(run_rows)

        self.assertEqual(len(run_rows), 8)
        self.assertEqual(len(invalid), 1)
        self.assertIn("seed", invalid[0]["reason"])
        self.assertEqual(len(suite_pairs[("normal", "warm")]["clava-js"]), 1)
        self.assertEqual(len(suite_pairs[("normal", "warm")]["java"]), 1)
        self.assertEqual(len(global_pairs[("normal", "warm")]), 1)
        self.assertEqual(global_pairs[("normal", "warm")][0]["text_s"], 40.0)
        self.assertEqual(global_pairs[("normal", "warm")][0]["protobuf_s"], 46.0)
        self.assertEqual(global_pairs[("normal", "warm")][0]["delta_s"], 6.0)
        self.assertEqual(global_pairs[("disabled", "warm")][0]["delta_s"], 3.0)
        self.assertEqual(unmatched, {})

        normal_rows = [row for row in run_rows if row["gc_policy"] == "normal"]
        counters = render_per_parse.ccache_counter_table(normal_rows)
        self.assertIn("Cacheable calls", counters)
        self.assertIn("<td>5</td><td>3</td><td>2</td><td>0</td>", counters)

    def test_html_keeps_full_command_wall_time_separate_from_parse_latency(self):
        rows, invalid = load_parse_fixture()
        run_rows, invalid_runs = load_runs_fixture()
        html = render_per_parse.report_html(FIXTURE, rows, invalid, run_rows, invalid_runs)

        self.assertIn("Whole-suite runtime", html)
        self.assertIn("Sequential Clava-JS + Java block", html)
        self.assertIn("includes caller-side heap logging and explicit GC", html)
        self.assertIn("Paired Clava-JS full-command differences", html)
        self.assertIn('class="command-details"', html)
        self.assertIn("Excluded parse or command rows, including invalid, incomplete, and seed rows: 3", html)

    def test_headline_workload_and_command_results_precede_parse_candles(self):
        rows, invalid = load_parse_fixture()
        run_rows, invalid_runs = load_runs_fixture()
        for row in run_rows:
            if row["gc_policy"] == "disabled":
                row["repeat"] = "1"
        plan = {
            "experiment": "same-revision A/B",
            "repeat_count": 6,
            "cache_mode": "warm",
            "expected_suite_counts": {
                "clava-js": {"total_tests": 164, "passed_tests": 158, "skipped_tests": 6},
                "java": {"total_tests": 116, "passed_tests": 116, "skipped_tests": 0},
            },
            "expected_parse_events": {"clava-js": 191, "java": 247},
            "parse_timing_boundary": "parsePrivate entry through TranslationUnit construction; excludes caller-side heap logging/System.gc",
            "full_command_timing_boundary": "monotonic wall time around the full command",
            "runtime_parser_jar_sha256": "jar-digest",
            "sources": {
                "clava": {"revision": "clava-rev", "dirty": {"status": [], "diff_sha256": "diff"}},
                "native": {"revision": "native-rev", "dirty": {"status": []}, "tool_sha256": "native-digest"},
            },
        }
        html = render_per_parse.report_html(FIXTURE, rows, invalid, run_rows, invalid_runs, plan)

        self.assertLess(html.index("Whole-suite runtime"), html.index("Study at a glance"))
        self.assertLess(html.index("Study at a glance"), html.index("Per-parse results"))
        self.assertIn("164 total; 158 passed; 6 skipped", html)
        self.assertIn("116 total; 116 passed; 0 skipped", html)
        self.assertIn("191 parse events per run", html)
        self.assertIn("247 parse events per run", html)
        self.assertIn("Java GC-policy contrast", html)
        self.assertIn("Java timing-boundary check", html)
        self.assertIn("Revision and artifact fingerprints", html)
        self.assertIn("jar-digest", html)
        self.assertIn("native-digest", html)
        self.assertNotIn("/home/", html)

    def test_theme_toggle_uses_html_dark_class_without_system_preference(self):
        rows, invalid = load_parse_fixture()
        html = render_per_parse.report_html(FIXTURE, rows, invalid)
        self.assertIn("html.dark", html)
        self.assertNotIn("prefers-color-scheme", html)

    def test_display_paths_strip_machine_roots_and_randomized_woven_directory_names(self):
        self.assertEqual(render_per_parse.safe_path_label("/home/person/source.c"), "source.c")
        label = render_per_parse.safe_path_label(
            "generated/__clava_woven_abcd1234_person/switch_to_if.c")
        self.assertEqual(label, "generated/__clava_woven_<id>/switch_to_if.c")
        self.assertNotIn("person", label)
        self.assertEqual(render_per_parse.safe_path_label("generated/tmp_person/dummy.cpp"),
                         "generated/tmp_<temp>/dummy.cpp")


if __name__ == "__main__":
    unittest.main()
