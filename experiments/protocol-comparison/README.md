# Protocol comparison benchmark

This harness compares four pinned Clava states with the same two workloads:

- the optimized parent before AST dump caching;
- the text protocol after ccache integration;
- the protobuf experiment branch;
- the eager FlatBuffers experiment branch.

FlatBuffers and protobuf are sibling branches from the same cache head. They
are not cumulative steps.

Each stage gets one uncharted resource warm-up followed by six measured runs. The
Clava-JS workload runs 164 tests with the four host-dependent OpenMP/CUDA
cases skipped, producing 158 passes and 6 skips. The Java workload contains
the same 116 parser tests used by the corrected cache report, with resource,
CUDA, cache-adapter, generated-root integration tests, and branch-only protocol
unit tests excluded.

Run both suites with:

```sh
python3 experiments/protocol-comparison/run_comparison.py
```

Select `--mode bypass`, `--mode cold`, or `--mode warm`. Warm mode preserves
the stage-owned ccache between runs, cold mode clears only that ccache before
each run, and bypass mode sets `CCACHE_DISABLE=true`. `--stages` accepts a
comma-separated subset when an uncached reference does not need to be repeated.

The runner refuses revision drift and writes raw logs, timing files, per-run
summaries, a CSV, and a final JSON manifest below its ignored `results/`
directory.
