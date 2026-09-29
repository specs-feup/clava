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

## Same-revision text/Protobuf A/B

`run_dual_ab.py` compares both readers in one scratch Clava checkout and uses
one scratch `clang-dumper/build/tool` binary and one staged Java runtime for both
modes. Its preflight parses representative C and C++ files with each reader,
compares normalized AST graphs, then runs targeted Java and Clava-JS smoke
tests. Timing needs the directory from a passing preflight.

Start with a no-test plan check:

```sh
python3 experiments/protocol-comparison/run_dual_ab.py --dry-run
python3 -B -m unittest discover -s experiments/protocol-comparison -p 'test_run_dual_ab.py'
```

Run the fidelity and smoke gate, then inspect `fidelity/gate.json`:

```sh
python3 experiments/protocol-comparison/run_dual_ab.py --preflight-only
```

For a one-repeat check of both original suites, use that preflight directory:

```sh
python3 experiments/protocol-comparison/run_dual_ab.py \
  --preflight-result experiments/protocol-comparison/results/<preflight-run> \
  --repeat-count 1
```

Run the six-repeat A/B after the one-repeat results look sound:

```sh
python3 experiments/protocol-comparison/run_dual_ab.py \
  --preflight-result experiments/protocol-comparison/results/<preflight-run> \
  --repeat-count 6
```

Use `--js-workspace`, `--fixture-c`, or `--fixture-cxx` to point at the exact
Clava-JS test checkout and supported fidelity fixtures. The plan records the
Clava and native revisions and hashes, composite build dependency revisions
and worktree state, the Clava-JS enclosing revision plus subtree identity, and
the staged parser JAR hash. Every timing row checks that the Java
worker selected the requested format, emitted metrics, reported
`ccache_disabled=true`, and made no ccache calls. It records the actual
`compressed` metric rather than treating ccache bypass as a compression mode.
Full-suite timing rows must also match their 191-event Clava-JS or 247-event
Java reference total. Smaller fidelity and smoke checks are exempt from those
full-suite totals. Text and Protobuf event counts must match within each paired
run; a mismatch marks both rows invalid. The fidelity snapshot's `data_class`
normalization is documented in [analysis/README.md](analysis/README.md).

## Per-parse warm-ccache A/B

`run_per_parse_ab.py` measures the complete Clava-JS and Java parser suites with
Text+ccache and Protobuf+ccache on the same scratch Clava checkout, native tool,
and staged runtime. It crosses both protocols with the normal and explicit-GC-
disabled Java policies. Each of the six measured repeats is paired by suite,
policy, test invocation, parse pass, stable resource key, and parser ID. Source
content and compiler-argument hashes are checked separately, so configuration
mismatches fail with a clear reason instead of disappearing from the identity
join. The argument hash normalizes only the exact run-specific Java temp-root
prefix and retains the relative path and argument order. Java fixture copies
get their original resource identity from the opt-in test registry. Exact
duplicate parse keys remain as raw rows with `pair_available=false`; reports
keep them in runtime-spread distributions and exclude them from row-wise paired
deltas. Clava-JS-generated `__clava_woven_<UUID>_<owner>` roots and Java JUnit
`junit<digits>` temporary roots are canonicalized only in the first path
component after stripping the protocol-specific run root. Owner and all
relative source path components remain part of identity. Compiler-argument
hashes are not rewritten beyond the exact Java temp-root normalization, and
are checked separately from source identity.

First run a fresh fidelity/smoke preflight, then run the full matrix:

```sh
python3 -B experiments/protocol-comparison/run_dual_ab.py \
  --preflight-only --output-root experiments/protocol-comparison/results/<preflight-run>

python3 -B experiments/protocol-comparison/run_per_parse_ab.py \
  --preflight-result experiments/protocol-comparison/results/<preflight-run> \
  --repeat-count 6 \
  --output-root experiments/protocol-comparison/results/<per-parse-run>
```

The primary `parses.csv` has one row per completed `parsePrivate` invocation.
`parse_elapsed_ms` is measured from entry through `TranslationUnit`
construction; it excludes caller-side heap-logging `System.gc()`. `runs.csv`
records each complete suite command's monotonic wall time, which does include
that caller-side work. It can be grouped by `pair_group_id` for the two-suite
aggregate. Both CSVs include seed rows marked `phase=seed,measured=false`;
reports should use only valid measured rows.

Text payloads are uncompressed because the native dumper only supports Zstandard
compression for Protobuf. Both protocols use ccache, each with an isolated
`java.io.tmpdir`/`DUMPER_FOLDER`; parse events report the actual adapter cache
directory. A seed must record misses. Each measured run must have a warm-cache
hit rate of at least 90% among cacheable ccache calls. Events with
`cache_enabled=false` are not ccache-eligible and have no cache directory; every
eligible event must name the run's actual isolated directory, and the ccache
counter total must equal the eligible-event count. A paired Text/Protobuf run must have equal
cacheable-call, hit, miss, and uncacheable-call counters while using distinct
cache paths. These are run-level ccache counters, not per-file hit attribution:
the legacy event `cache_enabled` flag only means the adapter is enabled. Counter
values and hit rates are repeated into parse rows with
`ccache_observation_scope=whole-suite-run` so their scope is explicit.

The runner rejects failed/missing suite tests, parse counts other than 191
Clava-JS or 247 Java events, format or effective GC-policy mismatches, incomplete
parse metrics, source/config digest mismatches, cache-state failures, and
protocol-pair identity mismatches. It preserves per-run logs and writes
`plan.json`, `parses.csv`, `runs.csv`, `results.json`, and `summary.json` under
the chosen output directory. It refuses to reuse an existing path.
