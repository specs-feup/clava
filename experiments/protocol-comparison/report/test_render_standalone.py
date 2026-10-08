import unittest

import render_standalone as report


class StandaloneReportTest(unittest.TestCase):
    def test_report_labels_the_isolated_source_workload_before_results(self):
        rows = [
            dict(suite='clava-js', cache_mode='warm', input_id='js-0001', repeat='1',
                 protocol=protocol, elapsed_ms=elapsed, source_sha256='source-sha',
                 source_content_sha256='source-sha', args_sha256='args-sha',
                 parse_args_sha256='args-sha', identity='unit.c')
            for protocol, elapsed in [('text', 10.0), ('protobuf', 12.0)]
        ]
        metadata = {
            'plan': {
                'source_fingerprint': {'clava_head': 'clava-rev'},
                'native_tool_sha256': 'native-sha',
                'runtime_manifest': {'sha256': 'runtime-sha'},
                'suite_event_counts': {'clava-js': 1, 'java': 0},
                'repeats': [1], 'cache_modes': ['warmcache'],
                'compression': False, 'show_exec_info': False,
            },
            'results': {
                'valid': True, 'complete': True,
                'source_fingerprint_unchanged': True,
                'materialized_inputs_unchanged': True,
                'measured_rows': 2, 'diagnostic_rows': 0,
                'fidelity': {'valid': True, 'compared_input_pairs': 1},
            },
            'historical_context': [{
                'suite': 'clava-js', 'comparison': 'Across branches',
                'text_s': 39.93, 'protobuf_s': 38.55, 'change_pct': -3.5,
            }],
        }

        html = report.render(rows, metadata)

        self.assertLess(html.index('Isolated-source replay'),
                        html.index('Cumulative time for isolated sources'))
        self.assertIn('Each captured source ran in its own CodeParser call', html)
        self.assertIn('cross-translation-unit reconciliation are absent', html)
        self.assertIn('each call still builds an App', html)
        self.assertIn('Clava-JS and Java identify input origin', html)
        self.assertIn('All replayed inputs', html)
        self.assertNotIn('Both suites', html)
        self.assertIn('516 original CodeParser calls', html)
        self.assertIn('73 carried multiple sources', html)
        self.assertIn('Separate historical suite measurements', html)
        self.assertIn('across-branch rows also change code and runtime artifacts', html)
        self.assertIn('cannot reproduce or explain an earlier suite speedup or regression', html)

    def test_total_change_is_median_of_matched_differences(self):
        pairs = [dict(repeat=index, text_ms=text, protobuf_ms=proto)
                 for index, (text, proto) in enumerate([(100, 101), (1, 11), (5, 25)])]
        text, proto, delta, percent, repetitions = report.totals(pairs)
        self.assertEqual((text, proto), (5, 25))
        self.assertEqual(delta, 10)
        self.assertAlmostEqual(percent, 400)
        self.assertEqual(repetitions, 3)

    def test_pairing_rejects_changed_inputs_and_incomplete_cohorts(self):
        def row(protocol, repeat, identity='a', digest='sha'):
            return dict(suite='java', cache_mode='warm', input_id=identity, repeat=repeat,
                        protocol=protocol, elapsed_ms=10, source_sha256=digest, args_sha256='args')
        with self.assertRaisesRegex(ValueError, 'source_sha256 differs'):
            report.paired([row('text', 1), row('protobuf', 1, digest='different')])
        with self.assertRaisesRegex(ValueError, 'Unmatched input'):
            report.paired([row('text', 1)])
        with self.assertRaisesRegex(ValueError, 'population changed'):
            report.paired([row(protocol, repeat, identity)
                           for repeat, identities in [(1, ['a', 'b']), (2, ['a'])]
                           for identity in identities for protocol in ['text', 'protobuf']])

    def test_individual_input_change_is_paired_before_summarizing(self):
        pairs = [dict(input_id='source', text_ms=text, protobuf_ms=proto, delta_ms=proto-text)
                 for text, proto in [(100, 101), (1, 11), (5, 25)]]
        self.assertEqual(report.input_medians(pairs)[0]['delta_ms'], 10)

    def test_missing_entire_measurement_cell_is_rejected(self):
        rows = [dict(suite='java', cache_mode=mode, input_id='a', repeat=repeat,
                     protocol=protocol, elapsed_ms=10, source_sha256='sha', args_sha256='args', identity='a')
                for mode, repeat in [('warm', 1), ('warm', 2), ('direct', 1)]
                for protocol in ['text', 'protobuf']]
        with self.assertRaisesRegex(ValueError, 'Missing measurement cell'):
            report.paired(rows)

    def test_profile_must_cover_every_measured_input_in_both_formats(self):
        pairs = [dict(suite='java', cache_mode='warm', input_id='a')]
        profiles = [dict(suite='java', cache_mode='warm', input_id='a', protocol='text', valid='True')]
        with self.assertRaisesRegex(ValueError, 'Diagnostic coverage'):
            report.validate_diagnostics(profiles, pairs)
        profiles.append(dict(profiles[0], protocol='protobuf'))
        report.validate_diagnostics(profiles, pairs)
        profiles.append(dict(profiles[0]))
        with self.assertRaisesRegex(ValueError, 'Duplicate diagnostic'):
            report.validate_diagnostics(profiles, pairs)

    def test_render_rejects_unverified_build_provenance(self):
        rows = [dict(suite='java', cache_mode='warm', input_id='a', repeat=1,
                     protocol=protocol, elapsed_ms=10, source_sha256='sha', args_sha256='args', identity='a')
                for protocol in ['text', 'protobuf']]
        with self.assertRaisesRegex(ValueError, 'metadata'):
            report.render(rows, {})

    def test_mobile_ticks_have_space_even_on_a_log_axis(self):
        positions = {10: 52, 50: 120, 200: 200, 500: 240, 2000: 330}
        labels = {10: '0.01 s', 50: '0.05 s', 200: '0.20 s', 500: '0.50 s', 2000: '2 s'}
        selected = report.charts.spaced_ticks(list(positions), positions.get, labels.get)
        self.assertEqual(selected, [10, 50, 200, 2000])


if __name__ == '__main__':
    unittest.main()
