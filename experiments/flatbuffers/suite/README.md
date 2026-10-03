# Clava-JS eager FlatBuffers workload

`run_suite.py` stages a built ClavaWeaver distribution and runs the fixed
Vitest selection from `Clava-JS`. There is no wire-format switch. Each run
records the packaged release tag, local schema/tool identities when applicable,
runtime JAR hashes, repository state, cache counters, host load, and GNU-time
metrics.

The test-name filter keeps the historical workload at 164 tests: 158 passed,
zero failed, two originally pending, and four host-dependent OpenMP/CUDA
exclusions. The exclusions remain visible as pending tests in the Vitest JSON.
A run fails when this population or the eager protocol contract changes
unexpectedly.

Run the three cache states sequentially, with independent cold caches paired
to their warm observations and cache bypass still exercising the production
file path:

```sh
python3 experiments/flatbuffers/suite/run_matrix.py \
  --runtime-root ClavaWeaver/build/install/ClavaWeaver --repeat-count 3
```

The matrix sets `CCACHE_DISABLE=true` for bypass and requires zero ccache calls
there; cold and warm runs require misses and restored hits, respectively. Do
not run build or benchmark jobs at the same time. Reports are written beneath
the ignored `experiments/flatbuffers/suite/results/` directory.

## Compare eager, Text, and Protobuf runtimes

`run_runtime_comparison.py` runs one bypass observation for each runtime, in
sequence. It uses the eager runner in this checkout and the historical Text
and Protobuf runners in
`~/.cache/ast-flatbuffers-release-validation/{text-build,protobuf-build}`.
The historical runners are loaded as controls, then pointed at the current
`Clava-JS` checkout and this suite's Vitest config. They stage the supplied
runtime into each run directory and patch only the staged parser JAR's dumper
tag. The checked out `clang-dumper-release.tag` is not changed.

The default inputs use these distributions and matching native tools:

```text
~/.cache/ast-flatbuffers-release-validation/eager-runtime
~/.cache/ast-flatbuffers-release-validation/text-build/clava/ClavaWeaver/build/install/ClavaWeaver
~/.cache/ast-flatbuffers-release-validation/protobuf-build/clava/ClavaWeaver/build/install/ClavaWeaver
~/.cache/ast-flatbuffers-release-validation/text-build/clang-dumper/build/tool
~/.cache/ast-flatbuffers-release-validation/protobuf-build/clang-dumper/build/tool
```

Run the comparison after the three distributions are ready:

```sh
python3 experiments/flatbuffers/suite/run_runtime_comparison.py
```

Use `--text-checkout` and `--protobuf-checkout` to point at other frozen
control checkouts. Their runtime and dumper defaults follow those paths. Each
runtime, dumper, and checkout can also be supplied directly. Extra Vitest
arguments are limited to `--maxWorkers=N`, `--minWorkers=N`, `--pool=NAME`,
`--fileParallelism`, and `--no-fileParallelism`; the driver owns the test
selection and config.

The fixed filter is
`^(?!(?:CxxTest OmpThreadsExplore|CudaTest Cuda|CudaTest CudaMatrixMul|CudaTest CudaQuery)$).*$`.
The driver requires 164 total tests, 158 passed, zero failed, and six pending.
It also checks that the four named exclusions are pending and that exactly two
other tests remain pending. The ordered suite and test names must match across
all three observations.

Every observation sets `CCACHE_DISABLE=true`. The driver builds a result-local
PATH link farm without a `ccache` executable and verifies that `ccache` does
not resolve from it. The historical runners' ccache-stat helpers are replaced
with a no-op because ccache is intentionally unavailable for this bypass
measurement. The summaries still record zero counters.

Compare `wall_s` from each runner summary. It measures monotonic wall time from
immediately before the GNU-time-wrapped `npm`/Vitest command starts until it
exits. It excludes runtime staging, setup, and report parsing. The result
manifest records each source revision, runner SHA-256, runtime JAR hashes,
dumper hash where applicable, test counts, cache state, and timing boundary.
Reports and the PATH link farm stay beneath the ignored `results/` directory.
