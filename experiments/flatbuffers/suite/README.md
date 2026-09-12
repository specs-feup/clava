# Real Clava-JS suite experiments

`run_suite.py` runs the actual Vitest suite from `Clava-JS`, with an isolated
XDG cache and an isolated copy of the existing `java-binaries` distribution.
The staged `ClangAstParser.jar` has only its `clang-dumper-release.tag`
resource rewritten, so the Java classes remain the existing distribution while
the process uses the local `clang-dumper-ast-flatbuffers/build/tool`.

The default native format is the existing compressed text file path. The
runner removes `AST_WIRE_FLAT` and `AST_WIRE_DENSE_TEXT` explicitly. It records
the native executable hash and both nested repository revisions in each
`summary.json`.

Run one state at a time. A cold run owns a new cache root; a warm run reuses
that same root after the cold run. Bypass keeps the cache directory isolated
but sets `CCACHE_DISABLE=true`. For a reproducible baseline, use the preserved
native snapshot below so a later native rebuild cannot change the executable
under measurement.

```sh
cd /home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers/clava
DUMPER="$PWD/experiments/flatbuffers/suite/results/dumper-snapshot/tool"
python3 experiments/flatbuffers/suite/run_suite.py --mode cold \
  --dumper "$DUMPER" \
  --cache-root experiments/flatbuffers/suite/results/cache-cold
python3 experiments/flatbuffers/suite/run_suite.py --mode warm \
  --dumper "$DUMPER" \
  --cache-root experiments/flatbuffers/suite/results/cache-cold
python3 experiments/flatbuffers/suite/run_suite.py --mode bypass \
  --dumper "$DUMPER"
```

The snapshot above belongs to the earlier pilot. The complete v2 matrix uses the
companion build explicitly, with SHA-256
`1e8b3e7efc6c0392241ef73da4b66d73928a42541a71b8c47cb184cfec3a3b16`.
Do not mix the pilot and complete-schema timings.

Each run writes its Vitest JSON/log, flattened `per_test_timings.csv`, GNU
`time` metrics, ccache counters and `summary.json` below ignored `results/`.
`suite-observations.svg` is a dependency-free grouped bar chart generated from
the summaries. Additional Vitest filters can be passed with repeated
`--vitest-arg`, for example `--vitest-arg 'api/Issues.test.ts'`.

Every run creates `results/<run>/tmp` before staging the Java runtime. The
runner sets `TMPDIR`, `TMP` and `TEMP` for Python, Node and native children, and
adds `-Djava.io.tmpdir=<run>/tmp` for Java. The summary records the path and
`df -P` filesystem metadata so a run can be checked without relying on the
machine's `/tmp` quota. This is required for the full matrix because `/tmp`
may be a small separate tmpfs while the experiment checkout is on `/home`.

`bypass` intentionally reports zero ccache calls because it sets
`CCACHE_DISABLE=true`; use the cold and warm summaries for hit and miss
counters. The current full text baseline has 164 tests (158 passed, 4 failed,
2 pending) in all three states. The four failures are the existing OpenMP
header and CUDA integration failures, so a non-zero Vitest exit is expected for
these observations.

For the complete format/cache experiment, `run_matrix.py` executes 27 cells
(text, flat-eager and flat-lazy; cold, warm and bypass; three repeats) one at a
time. It rotates the format order by repeat (`text/eager/lazy`,
`eager/lazy/text`, `lazy/text/eager`) and keeps each format's cold and warm
cells adjacent before its bypass cell. Each warm cell reuses its paired cold
cache, and each bypass cell gets a fresh isolated cache. Use the frozen native
build selected for the experiment:

```sh
cd /home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers/clava
python3 experiments/flatbuffers/suite/run_matrix.py \
  --dumper "/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ast-flatbuffers/build/tool"
```

The driver creates a timestamped `results/matrix-*` directory, records the
matrix plan and progress, and preserves every normal runner result directory.
Each run records a canonical SHA-256 manifest of the source runtime jars and
the porcelain status of both the Clava and clang-dumper checkouts. The driver
stops on the first unexpected cell: every completed cell must contain 164 tests
with 158 passed, 4 failed and 2 pending, and its four failures must be
`OmpThreadsExplore`, `Cuda`, `CudaMatrixMul` and `CudaQuery`.

The final 27-cell matrix is archived under `../measurements/complete/`.
All formats had 16/167 cold direct hits and 162/167 warm direct hits. The five
warm misses come from generated sources whose run-owned temporary paths change.
This keeps the formats comparable to each other, but not to older 167/167-hit
runs that used a shared temporary path.
