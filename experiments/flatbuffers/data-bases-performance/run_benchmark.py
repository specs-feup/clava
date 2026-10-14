#!/usr/bin/env python3
"""Compare committed Clava revisions with the original thread's fixed App benchmark cohorts."""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys

sys.dont_write_bytecode = True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--workspace', type=Path, default=Path(__file__).resolve().parents[4])
    parser.add_argument('--harness-workspace', type=Path)
    parser.add_argument('--baseline', default='aa0946736')
    parser.add_argument('--current', default='HEAD')
    parser.add_argument('--release', default='v18.1.8_7-rc4')
    parser.add_argument('--resource-seed', type=Path, required=True, help='Verified clang-dumper cache with releases/ and includes/.')
    parser.add_argument('--repeats', type=int, default=4)
    parser.add_argument('--suites', nargs='+', choices=['clava-js', 'java'], default=['clava-js', 'java'])
    parser.add_argument('--modes', nargs='+', choices=['direct', 'cold', 'warm'], default=['direct', 'warm'])
    parser.add_argument('--wall', action='store_true', help='Also measure separate commands without the App overlay.')
    parser.add_argument('--profile', action='store_true', help='Record Java-worker JFR profiles; timings are diagnostic only.')
    parser.add_argument('--keep', action='store_true', help='Keep isolated snapshot worktrees and cache directories.')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    if args.repeats < 1 or len(set(args.modes)) != len(args.modes) or len(set(args.suites)) != len(args.suites):
        parser.error('Use a positive repeat count and unique suites/cache modes.')
    workspace = args.workspace.resolve()
    harness_workspace = (args.harness_workspace or workspace.parent / 'ast-protobuf').resolve()
    harness = harness_workspace / 'clava/experiments/protobuf/suite/refresh'
    sys.path.insert(0, str(harness))
    import three_way as t
    t.WORKSPACE = workspace
    t.SUITES = tuple(args.suites)
    # Nested node_modules directories are optional in npm's hoisted workspace layout.
    original_link = t._link_node_modules
    def link(source, destination, old_root, new_root):
        if source.is_dir():
            original_link(source, destination, old_root, new_root)
        elif source == old_root / 'node_modules':
            raise RuntimeError(f'Missing workspace dependencies: {source}')
        else:
            destination.mkdir(parents=True)
    t._link_node_modules = link
    def namespace(clava):
        paths = [clava / 'ClavaAst/build/generated/wire-schema/pt/up/fe/specs/clang/wire/ast-wire.sha256',
                 clava / 'ClangAstParser/build/generated/wire-schema/pt/up/fe/specs/clang/wire/ast-wire.sha256']
        schema_hash = next(path.read_text().strip() for path in paths if path.is_file())
        return 'clang-dumper-ccache/flatbuffers-v2-' + schema_hash
    repository = workspace / 'clava'
    baseline = t.git('rev-parse', args.baseline, cwd=repository)
    current = t.git('rev-parse', args.current, cwd=repository)
    t.STAGES = tuple(t.StageSpec(key, repository, ref, 'flat-eager', args.release,
                                args.resource_seed.resolve(), namespace)
                     for key, ref in [('baseline', baseline), ('current', current)])
    output = args.output or Path.home() / '.cache/clava-data-bases-performance' / dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    output = output.resolve()
    print(json.dumps({'output': str(output), 'baseline': baseline, 'current': current}), flush=True)
    with t.refresh._exclusive_lock(t.refresh.DEFAULT_LOCK):
        t.prepare(output)
        try:
            collector = t.Collector(output, quick=not args.wall)
            with collector.java_init.open('a') as init:
                init.write("""\ngradle.afterProject { project ->
    if (project.name == 'ClangAstParser') project.tasks.named('test') {
        filter { excludeTestsMatching 'pt.up.fe.specs.clang.codeparser.ParallelCodeParserDefaultsTest' }
    }
}
""")
            # Apply the same parser-default initialization to older controls. Concurrent default reads
            # mutate SimpleDataStore's HashMap and can lose explicit settings such as SHOW_CODE.
            instrument_source = collector.overlay.instrument_source
            def stable_parser_defaults(source):
                if 'void initializeParallelDefaults()' not in source:
                    anchor = '        ExecutorService executor = Executors.newFixedThreadPool(numThreads);'
                    if source.count(anchor) != 1:
                        raise RuntimeError('Parallel parser initialization anchor changed')
                    source = source.replace(anchor, '''        for (var key : getStoreDefinitionTry().orElseThrow().getKeys()) {
            if (key != GENERATED_PARSE_ROOT && key.hasDefaultValue() && !hasValue(key)) get(key);
        }
''' + anchor, 1)
                return instrument_source(source)
            collector.overlay.instrument_source = stable_parser_defaults
            collector.record['parser_defaults_control'] = 'Defaults initialized before parallel workers in both revisions; prevents shared HashMap writes and lost settings.'
            if args.profile:
                with collector.java_init.open('a') as init:
                    init.write("""\ngradle.afterProject { project ->
    if (project.name == 'ClangAstParser') {
        project.tasks.named('test') {
            def path = System.getenv('CLAVA_PROFILE_PATH')
            if (path) jvmArgs '-XX:StartFlightRecording=settings=profile,dumponexit=true,filename=' + path
        }
    }
}
""")
                base_environment = collector.app.base_environment
                def profile_environment(stage, suite, mode, repeat, run_dir, *remaining, **kwargs):
                    env = base_environment(stage, suite, mode, repeat, run_dir, *remaining, **kwargs)
                    if suite == 'java':
                        env['CLAVA_PROFILE_PATH'] = str(run_dir / 'worker.jfr')
                    return env
                collector.app.base_environment = profile_environment
                collector.record['profiling'] = 'Java worker JFR profile enabled; timings are diagnostic, not final performance evidence.'
            collector.record['quick'] = args.repeats == 1 and not args.wall
            collector.record['methodology'] = ('Matched committed revisions, pinned 116-test Java and 164-test JS cohorts; '
                    'serial rounds rotate revision, cache mode and suite. App time includes AST parsing and processing; '
                    'builds, resource download and unmeasured direct/warm preflights occur before measurement.')
            collector.record['settings'] = {'repeats': args.repeats, 'suites': args.suites, 'modes': args.modes, 'wall': args.wall,
                    'release': args.release, 'profile': args.profile, 'host': platform.node(), 'platform': platform.platform(),
                    'cpu': Path('/proc/cpuinfo').read_text().split('model name')[1].split('\n')[0].strip(': \t'),
                    'java': subprocess.run(['java', '-version'], capture_output=True, text=True).stderr,
                    'node': subprocess.check_output(['node', '--version'], text=True).strip(),
                    'harness_sha256': hashlib.sha256((harness / 'three_way.py').read_bytes()).hexdigest()}
            t.build(output, collector.java_init)
            collector.setup_stages()
            phases = [('app', collector.app_run)] + ([('wall', collector.wall_run)] if args.wall else [])
            seeds = {}
            keys = list(collector.stages)
            ordinal = 0
            for phase, run in phases:
                collector._set_order(phase)
                for key in keys:
                    stage = collector.stages[key]
                    for suite in t.SUITES:
                        ordinal += 1
                        collector.record['preflight'].append(run(stage, suite, 'direct', 0, ordinal, False))
                        ordinal += 1
                        collector.record['seeds'].append(run(stage, suite, 'warm', 0, ordinal, False))
                        cache = collector.app.cache_dir_for(stage, suite, output / phase)
                        seed = output / phase / 'seeds' / key / suite
                        shutil.copytree(cache, seed)
                        seeds[phase, key, suite] = seed
                        collector.save()
                        print(json.dumps({'preflight': phase, 'stage': key, 'suite': suite, 'valid': True}), flush=True)
            collector.record['status'] = 'running'
            for phase, run in phases:
                collector._set_order(phase)
                for repeat in range(1, args.repeats + 1):
                    offset = (repeat - 1) % len(keys)
                    for position, key in enumerate(keys[offset:] + keys[:offset]):
                        stage = collector.stages[key]
                        shift = (repeat - 1 + position) % len(args.modes)
                        for index, mode in enumerate(args.modes[shift:] + args.modes[:shift]):
                            suites = t.SUITES if (repeat + position + index) % 2 == 0 else t.SUITES[::-1]
                            for suite in suites:
                                if mode == 'warm':
                                    cache = collector.app.cache_dir_for(stage, suite, output / phase)
                                    shutil.rmtree(cache, ignore_errors=True)
                                    shutil.copytree(seeds[phase, key, suite], cache)
                                ordinal += 1
                                row = run(stage, suite, mode, repeat, ordinal, True)
                                collector.record['observations'].append(row)
                                collector.save()
                                print(json.dumps({k: row.get(k) for k in ('phase', 'stage', 'suite', 'mode', 'repeat', 'app_elapsed_ms', 'wall_s')}), flush=True)
            collector.record['status'] = 'complete'
            collector.record['completed_utc'] = dt.datetime.now(dt.timezone.utc).isoformat()
            collector.save()
            (output / 'summary.txt').write_text(t.summary(collector.record) + '\n')
            print(t.summary(collector.record), flush=True)
        except Exception as error:
            if 'collector' in locals():
                collector.record['status'] = 'failed'
                collector.record['error'] = repr(error)
                collector.save()
            raise
        finally:
            if not args.keep:
                for phase in ('app', 'wall'):
                    for name in ('cache', 'temp', 'seeds'):
                        shutil.rmtree(output / phase / name, ignore_errors=True)
                t.remove_worktrees(output)
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
