# Protocol comparison benchmark

This harness compares the current heads of four local Clava worktrees with the
same two workloads:

- the optimized parent before AST dump caching;
- the text protocol after ccache integration;
- the protobuf experiment branch;
- the eager FlatBuffers experiment branch.

FlatBuffers and protobuf are sibling branches from the same cache head. They
are not cumulative steps. Each run records the Clava and dumper branch names,
HEADs, worktree status, runtime jar manifest, and built dumper hash. It checks
that the HEADs do not move during a run. Each Clava-JS observation also records
the checksum of its staged parser jar after the dumper release tag is patched.

Each stage gets one uncharted resource warm-up followed by six measured runs. The
Clava-JS workload runs 164 tests with the four host-dependent OpenMP/CUDA
cases skipped, producing 158 passes and 6 skips. The Java workload contains
the same 116 parser tests used by the corrected cache report, with resource,
CUDA, cache-adapter, generated-root integration tests, and branch-only protocol
unit tests excluded.

Run each cache state with the same suite and repeat count:

```sh
python3 experiments/protocol-comparison/run_comparison.py --mode direct
python3 experiments/protocol-comparison/run_comparison.py --mode cold
python3 experiments/protocol-comparison/run_comparison.py --mode warm
```

The pre-cache reference has no AST dump cache. It runs in direct mode only and
is automatically omitted from cold and warm runs. Direct mode sets
`CCACHE_DISABLE=true` and puts a small probe wrapper first on `PATH`; any ccache
invocation makes the observation invalid. Cold mode clears only the owned cache
before every invocation and requires cache misses (same-run duplicate inputs
may still hit). Warm mode seeds the cache with an unmeasured run, then requires
at least one cache hit in each measured run. Per-run hit, miss, and cacheable
call counts are saved in JSON and CSV.

`--stages` accepts a comma-separated subset. Clava and native dumper revisions
are read from the checked-out branch heads at launch. The FlatBuffers Java run
sets `FLAT_NATIVE` to that stage's dumper source root so Gradle uses its built
generator.

Every invocation gets a unique timestamped output directory under the ignored
`results/` directory. The runner refuses to reuse an existing output directory
and writes raw logs, timing files, per-run summaries, a CSV, and a JSON manifest;
existing results are left in place.
