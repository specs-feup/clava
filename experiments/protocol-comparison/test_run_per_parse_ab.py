from __future__ import annotations

import tempfile
from pathlib import Path
from unittest.mock import patch
import unittest

import run_per_parse_ab as harness


class PerParseHarnessTests(unittest.TestCase):
    def test_java_suite_filter_excludes_all_harness_only_tests(self) -> None:
        init_script = (harness.SCRIPT_ROOT / "java-suite.init.gradle").read_text(encoding="utf-8")

        self.assertIn(
            "excludeTestsMatching 'pt.up.fe.specs.clang.dumper.ClangAstDumperArgumentsTest'",
            init_script,
        )
        self.assertIn(
            "excludeTestsMatching 'pt.up.fe.specs.clang.wire.AstWireBenchmarkIdentityTest'",
            init_script,
        )

    def test_parse_metric_events_accepts_both_wire_prefixes(self) -> None:
        text = '\n'.join((
            'PROTOBUF_METRIC {"format":"protobuf","parse_elapsed_ms":12.5}',
            'CLAVA_AST_METRIC {"format":"text","parse_elapsed_ms":10.0}',
            'PROTOBUF_METRIC {broken json}',
        ))

        events = harness.parse_metric_events(text)

        self.assertEqual([event["format"] for event in events], ["protobuf", "text"])

    def test_java_events_uses_suite_output_fallback_per_xml_file(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            junit = root / "junit"
            junit.mkdir()
            log = root / "run.log"
            (junit / "first.xml").write_text(
                '<testsuite name="first"><testcase classname="A" name="one">'
                '<system-out>PROTOBUF_METRIC {"parse_id":"1"}</system-out>'
                '</testcase></testsuite>', encoding="utf-8",
            )
            (junit / "second.xml").write_text(
                '<testsuite name="second"><system-out>'
                'PROTOBUF_METRIC {"parse_id":"2"}</system-out></testsuite>', encoding="utf-8",
            )

            events = harness.java_events(log, junit)

        self.assertEqual([event["parse_id"] for event in events], ["1", "2"])
        self.assertEqual(events[0]["test_case"], "A#one")

    def test_registered_identity_excludes_digests_and_pair_key_disambiguates_distinct_parses(self) -> None:
        base = {
            "format": "text", "source_path": "/tmp/random/source.c", "parse_id": "0",
            "test_id": "sample.ParserTest#testIt", "tester_invocation": 1,
            "parse_pass": "original", "resource_key": "c/sample.c",
            "parse_args_sha256": "args", "source_content_sha256": "content-a",
        }
        events = harness.identify_events(
            [base, {**base, "source_content_sha256": "content-b"}], "java",
            Path("/clava"), Path("/clava/Clava-JS"),
        )

        self.assertEqual(events[0]["source_identity"], events[1]["source_identity"])
        self.assertNotEqual(events[0]["parse_pair_key"], events[1]["parse_pair_key"])
        self.assertTrue(all(event["pair_available"] for event in events))
        self.assertEqual([event["identity_quality"] for event in events], ["registered", "registered"])
        self.assertEqual(events[0]["source_identity"],
                         harness.identify_events([base], "java", Path("/clava"), Path("/js"))[0]["source_identity"])

    def test_identical_duplicate_parse_rows_are_retained_but_not_pairable(self) -> None:
        event = {
            "format": "text", "source_path": "/tmp/random/source.c", "parse_id": "0",
            "test_id": "sample.ParserTest#testIt", "tester_invocation": 1,
            "parse_pass": "original", "resource_key": "c/sample.c",
            "parse_args_sha256": "args", "source_content_sha256": "content",
        }

        rows = harness.identify_events(
            [event, event], "java", Path("/clava"), Path("/clava/Clava-JS"),
        )

        self.assertEqual(len(rows), 2)
        self.assertEqual(rows[0]["parse_pair_key"], rows[1]["parse_pair_key"])
        self.assertEqual([row["pair_available"] for row in rows], [False, False])
        self.assertEqual([row["identity_quality"] for row in rows],
                         ["ambiguous_duplicate", "ambiguous_duplicate"])

    def test_fallback_source_label_strips_random_tester_root_and_marks_roundtrip(self) -> None:
        event = {"source_path": "/tmp/temp-clang-ast-random/outputFirst/c/bench/2mm.c"}

        label, parse_pass = harness.source_label(event, "java", Path("/repo"), Path("/repo/Clava-JS"))

        self.assertEqual(label, "c/bench/2mm.c")
        self.assertEqual(parse_pass, "roundtrip")

    def test_source_label_preserves_checkout_and_run_relative_paths(self) -> None:
        checkout_label = harness.source_label(
            {"source_path": "/repo/clava/ClavaWeaver/resources/c/issue.c"},
            "clava-js", Path("/scratch/clava"), Path("/repo/clava/Clava-JS"), Path("/run/temp"),
        )
        generated_label = harness.source_label(
            {"source_path": "/run/temp/tmp_user/dummyFile.cpp"},
            "clava-js", Path("/scratch/clava"), Path("/repo/clava/Clava-JS"), Path("/run/temp"),
        )
        unresolved_label = harness.source_label(
            {"source_path": "/opt/unknown/include/issue.c"},
            "clava-js", Path("/scratch/clava"), Path("/repo/clava/Clava-JS"), Path("/run/temp"),
        )

        self.assertEqual(checkout_label, ("ClavaWeaver/resources/c/issue.c", "source"))
        self.assertEqual(generated_label, ("generated/tmp_user/dummyFile.cpp", "source"))
        self.assertEqual(unresolved_label, ("unresolved/opt/unknown/include/issue.c", "fallback"))

    def test_source_label_canonicalizes_only_known_woven_uuid_component(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            clava_root = root / "clava"
            text_root = clava_root / "experiments/results/temp/clava-js/normal/text"
            proto_root = clava_root / "experiments/results/temp/clava-js/normal/protobuf"
            text_root.mkdir(parents=True)
            proto_root.mkdir(parents=True)

            text_label = harness.source_label(
                {"source_path": str(text_root / "__clava_woven_"
                                     "c5f11cb8-c4cf-4b85-8144-7e19064e4920_lmsousa/src/parse.c")},
                "clava-js", clava_root, root / "Clava-JS", text_root,
            )
            proto_label = harness.source_label(
                {"source_path": str(proto_root / "__clava_woven_"
                                     "68f1484c-7bfa-437c-a48d-f0dfd4dd2c9d_lmsousa/src/parse.c")},
                "clava-js", clava_root, root / "Clava-JS", proto_root,
            )
            different_suffix = harness.source_label(
                {"source_path": str(text_root / "__clava_woven_"
                                     "c5f11cb8-c4cf-4b85-8144-7e19064e4920_other/src/parse.c")},
                "clava-js", clava_root, root / "Clava-JS", text_root,
            )
            different_relative_file = harness.source_label(
                {"source_path": str(proto_root / "__clava_woven_"
                                     "68f1484c-7bfa-437c-a48d-f0dfd4dd2c9d_lmsousa/src/other.c")},
                "clava-js", clava_root, root / "Clava-JS", proto_root,
            )
            nonmatching_directory = harness.source_label(
                {"source_path": str(text_root / "__clava_woven_not-a-uuid_lmsousa/src/parse.c")},
                "clava-js", clava_root, root / "Clava-JS", text_root,
            )

        self.assertEqual(text_label, proto_label)
        self.assertEqual(text_label, ("generated/__clava_woven_<uuid>_lmsousa/src/parse.c", "source"))
        self.assertNotEqual(text_label, different_suffix)
        self.assertNotEqual(text_label, different_relative_file)
        self.assertEqual(nonmatching_directory,
                         ("generated/__clava_woven_not-a-uuid_lmsousa/src/parse.c", "source"))

    def test_source_label_canonicalizes_only_numeric_junit_temp_component(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            clava_root = root / "clava"
            text_root = clava_root / "temp/java/text"
            proto_root = clava_root / "temp/java/protobuf"
            text_root.mkdir(parents=True)
            proto_root.mkdir(parents=True)

            text_label = harness.source_label(
                {"source_path": str(text_root / "junit17030483088972485408"
                                     "/paren_list_initialization.cpp")},
                "java", clava_root, root / "Clava-JS", text_root,
            )
            proto_label = harness.source_label(
                {"source_path": str(proto_root / "junit1114943792790136064"
                                     "/paren_list_initialization.cpp")},
                "java", clava_root, root / "Clava-JS", proto_root,
            )
            different_relative_file = harness.source_label(
                {"source_path": str(proto_root / "junit1114943792790136064"
                                     "/source_locations.cpp")},
                "java", clava_root, root / "Clava-JS", proto_root,
            )
            nonmatching_directory = harness.source_label(
                {"source_path": str(text_root / "junit17030483088972485408x"
                                     "/paren_list_initialization.cpp")},
                "java", clava_root, root / "Clava-JS", text_root,
            )

        self.assertEqual(text_label, proto_label)
        self.assertEqual(text_label,
                         ("generated/junit<temp>/paren_list_initialization.cpp", "source"))
        self.assertNotEqual(text_label, different_relative_file)
        self.assertEqual(nonmatching_directory,
                         ("generated/junit17030483088972485408x/paren_list_initialization.cpp", "source"))

    def test_seed_pair_validation_checks_every_policy_and_suite_after_mismatch(self) -> None:
        policies = ("normal", "disabled")
        suites = ("clava-js", "java")
        runs = {}
        parse_rows = {}
        for policy in policies:
            for suite in suites:
                for protocol in ("text", "protobuf"):
                    rid = harness.run_id(suite, policy, protocol, "seed", 0)
                    runs[rid] = {"run_id": rid}
                    parse_rows[rid] = []

        with patch.object(harness, "validate_wire_pair", side_effect=[False, True, False, True]) as validate:
            result = harness.validate_seed_pairs(policies, suites, runs, parse_rows)

        self.assertFalse(result)
        self.assertEqual(validate.call_count, len(policies) * len(suites))

    def test_wire_pair_rejects_source_or_configuration_digest_mismatch(self) -> None:
        event = {
            "source_identity": "identity", "source_content_sha256": "source-a",
            "parse_args_sha256": "args", "format": "text",
        }
        proto_event = {**event, "source_content_sha256": "source-b", "format": "protobuf"}
        text_run = {"metrics": [event], "valid": True, "validity_reason": "valid"}
        proto_run = {"metrics": [proto_event], "valid": True, "validity_reason": "valid"}
        text_rows = [{"run_valid": True, "validity_reason": "valid"}]
        proto_rows = [{"run_valid": True, "validity_reason": "valid"}]

        matched = harness.validate_wire_pair(text_run, text_rows, proto_run, proto_rows)

        self.assertFalse(matched)
        self.assertFalse(text_run["valid"])
        self.assertFalse(proto_run["valid"])
        self.assertFalse(text_rows[0]["run_valid"])
        self.assertTrue(text_run["identity_multiset_match"])
        self.assertFalse(text_run["source_content_match"])

    def test_wire_pair_reports_argument_mismatch_separately_from_identity(self) -> None:
        event = {
            "source_identity": "identity", "identity_base": "base",
            "source_content_sha256": "source", "parse_args_sha256": "args-a",
        }
        proto_event = {**event, "parse_args_sha256": "args-b"}
        text_run = {"metrics": [event], "valid": True, "validity_reason": "valid"}
        proto_run = {"metrics": [proto_event], "valid": True, "validity_reason": "valid"}
        text_rows = [{"run_valid": True, "validity_reason": "valid"}]
        proto_rows = [{"run_valid": True, "validity_reason": "valid"}]

        matched = harness.validate_wire_pair(text_run, text_rows, proto_run, proto_rows)

        self.assertFalse(matched)
        self.assertTrue(text_run["identity_multiset_match"])
        self.assertTrue(text_run["source_content_match"])
        self.assertFalse(text_run["parse_args_match"])
        self.assertIn("paired_compile_arguments_digest_mismatch", text_run["validity_reason"])

    def test_wire_pair_rejects_swapped_source_argument_associations(self) -> None:
        text_events = [
            {"source_identity": "identity", "identity_base": "base", "source_content_sha256": "s1",
             "parse_args_sha256": "a1"},
            {"source_identity": "identity", "identity_base": "base", "source_content_sha256": "s2",
             "parse_args_sha256": "a2"},
        ]
        proto_events = [
            {"source_identity": "identity", "identity_base": "base", "source_content_sha256": "s1",
             "parse_args_sha256": "a2"},
            {"source_identity": "identity", "identity_base": "base", "source_content_sha256": "s2",
             "parse_args_sha256": "a1"},
        ]
        text_run = {"metrics": text_events, "valid": True, "validity_reason": "valid"}
        proto_run = {"metrics": proto_events, "valid": True, "validity_reason": "valid"}
        text_rows = [{"run_valid": True, "validity_reason": "valid"}]
        proto_rows = [{"run_valid": True, "validity_reason": "valid"}]

        self.assertFalse(harness.validate_wire_pair(text_run, text_rows, proto_run, proto_rows))
        self.assertTrue(text_run["source_content_match"])
        self.assertTrue(text_run["parse_args_match"])
        self.assertFalse(text_run["source_args_match"])

    def test_duplicate_digest_multiset_pairing_does_not_depend_on_event_order(self) -> None:
        text_events = [
            {"source_identity": f"id-{i}", "identity_base": "duplicate", "source_content_sha256": source,
             "parse_args_sha256": "args"}
            for i, source in enumerate(("source-a", "source-b"))
        ]
        proto_events = [
            {"source_identity": f"id-{i}", "identity_base": "duplicate", "source_content_sha256": source,
             "parse_args_sha256": "args"}
            for i, source in enumerate(("source-b", "source-a"))
        ]
        text_run = {"metrics": text_events, "valid": True, "validity_reason": "valid",
                    "cache_dir": "/tmp/text/cache"}
        proto_run = {"metrics": proto_events, "valid": True, "validity_reason": "valid",
                     "cache_dir": "/tmp/protobuf/cache"}
        for run in (text_run, proto_run):
            run.update({"ccache_cacheable_calls": 1, "ccache_hits": 1, "ccache_misses": 0,
                        "ccache_uncacheable_calls": 0, "ccache_adapter_events": 1,
                        "ccache_event_counter_match": True})
        text_rows = [{"run_valid": True, "validity_reason": "valid"}]
        proto_rows = [{"run_valid": True, "validity_reason": "valid"}]

        self.assertTrue(harness.validate_wire_pair(text_run, text_rows, proto_run, proto_rows))

    def test_wire_pair_requires_isolated_equal_ccache_distributions(self) -> None:
        event = {
            "source_identity": "identity", "source_content_sha256": "source",
            "parse_args_sha256": "args",
        }
        text_run = {
            "metrics": [event], "valid": True, "validity_reason": "valid", "cache_dir": "/tmp/text/cache",
            "ccache_cacheable_calls": 10, "ccache_hits": 9, "ccache_misses": 1,
            "ccache_uncacheable_calls": 0,
        }
        proto_run = {
            "metrics": [event], "valid": True, "validity_reason": "valid", "cache_dir": "/tmp/protobuf/cache",
            "ccache_cacheable_calls": 10, "ccache_hits": 8, "ccache_misses": 2,
            "ccache_uncacheable_calls": 0,
        }
        text_rows = [{"run_valid": True, "validity_reason": "valid"}]
        proto_rows = [{"run_valid": True, "validity_reason": "valid"}]

        matched = harness.validate_wire_pair(text_run, text_rows, proto_run, proto_rows)

        self.assertFalse(matched)
        self.assertTrue(text_run["identity_multiset_match"])
        self.assertFalse(text_run["cache_distribution_match"])
        self.assertFalse(text_run["valid"])

    def test_protocol_cache_directories_are_isolated_by_temp_root(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            text_root = root / "text"
            protobuf_root = root / "protobuf"

            text_cache = text_root / "actual-dumper-folder" / harness.CCACHE_NAMESPACE
            protobuf_cache = protobuf_root / "actual-dumper-folder" / harness.CCACHE_NAMESPACE
            text_cache.mkdir(parents=True)
            protobuf_cache.mkdir(parents=True)

            self.assertNotEqual(text_cache, protobuf_cache)
            self.assertEqual(text_cache.name, harness.CCACHE_NAMESPACE)
            self.assertEqual(protobuf_cache.name, harness.CCACHE_NAMESPACE)
            self.assertEqual(harness.cache_directories(text_root), [text_cache.resolve()])
            self.assertEqual(
                harness.reported_cache_directory(
                    [
                        {"ccache_cache_dir": str(text_cache.resolve()), "cache_enabled": True},
                        {"ccache_cache_dir": None, "cache_enabled": False},
                    ], text_root,
                ),
                text_cache.resolve(),
            )
            self.assertFalse(harness.event_cache_path_matches(
                {"ccache_cache_dir": None, "cache_enabled": True}, text_cache.resolve(),
            ))

    def test_six_repeat_order_alternates_protocol_first(self) -> None:
        first_repeat = [stage["wire"] for stage, _suite in harness.measured_order(1)]
        second_repeat = [stage["wire"] for stage, _suite in harness.measured_order(2)]

        self.assertEqual(first_repeat, ["text", "text", "protobuf", "protobuf"])
        self.assertEqual(second_repeat, ["protobuf", "protobuf", "text", "text"])


if __name__ == "__main__":
    unittest.main()
