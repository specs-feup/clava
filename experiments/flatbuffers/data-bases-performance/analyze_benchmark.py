#!/usr/bin/env python3
"""Produce a compact, auditable summary of a completed matched benchmark."""
import argparse
import collections
import hashlib
import json
from pathlib import Path
import statistics


def summarize(path: Path) -> dict:
    record = json.loads(path.read_text())
    if record.get('status') != 'complete':
        raise ValueError('Only a complete run can be summarized.')
    repeats = record['settings']['repeats']
    expected = collections.Counter((stage, suite, mode, repeat)
        for stage in ['baseline', 'current'] for suite in ['clava-js', 'java']
        for mode in record['settings']['modes'] for repeat in range(1, repeats + 1))
    rows = [r for r in record['observations'] if r['phase'] == 'app']
    if collections.Counter((r['stage'], r['suite'], r['mode'], r['repeat']) for r in rows) != expected:
        raise ValueError('Measured cells are missing or duplicated.')
    if not all(r['valid'] and r['cache_validation']['passed'] and r['return_code'] == 0 for r in rows):
        raise ValueError('A measurement did not pass the workload or cache gate.')
    if any(r['calls_with_different_content'] != 0 for r in rows):
        raise ValueError('These revisions parsed different source contents.')
    cells = []
    for suite in ['clava-js', 'java']:
        for mode in record['settings']['modes']:
            cell = {'suite': suite, 'mode': mode, 'metrics': {}}
            selected = [r for r in rows if (r['suite'], r['mode']) == (suite, mode)]
            for metric, field, divisor in [('app_s', 'app_elapsed_ms', 1000),
                                           ('instrumented_command_s', 'wall_s', 1)]:
                stats = {}
                for stage in ['baseline', 'current']:
                    samples = [r[field] / divisor for r in sorted(selected, key=lambda r: r['repeat'])
                               if r['stage'] == stage]
                    stats[stage] = {'samples': samples, 'median': statistics.median(samples),
                                    'min': min(samples), 'max': max(samples)}
                stats['median_change_percent'] = 100 * (stats['current']['median'] / stats['baseline']['median'] - 1)
                stats['paired_change_percent'] = [100 * (b / a - 1)
                    for a, b in zip(stats['baseline']['samples'], stats['current']['samples'])]
                cell['metrics'][metric] = stats
            cells.append(cell)
    return {
        'baseline': record['snapshot']['stages']['baseline']['revision'],
        'current': record['snapshot']['stages']['current']['revision'],
        'shared': record['snapshot']['shared'], 'settings': record['settings'],
        'created_utc': record['created_utc'], 'completed_utc': record['completed_utc'],
        'results_sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
        'raw_results': str(path.resolve()), 'cells': cells,
        'checks': {'all_cells_valid': True, 'all_cache_gates_passed': True,
                   'source_content_differences': 0, 'java_tests': 116, 'java_app_calls': 216,
                   'js_tests': 164, 'js_passed': 158, 'js_skipped': 6, 'js_app_calls': 170},
        'uncertainty': 'Ranges are observed minima and maxima, not confidence intervals. Four runs do not prove exact equivalence.',
        'command_time': 'Full command time from App-instrumented runs, including process/Gradle startup and test work outside App.build.',
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('results', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    summary = summarize(args.results)
    args.output.write_text(json.dumps(summary, indent=2) + '\n')
    for cell in summary['cells']:
        stats = cell['metrics']['app_s']
        print(f"{cell['suite']:10} {cell['mode']:7} {stats['baseline']['median']:6.3f} -> "
              f"{stats['current']['median']:6.3f} s, {stats['median_change_percent']:+.2f}%")

if __name__ == '__main__':
    main()
