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
The historical runners are loaded as controls and run the same current
`Clava-JS` test sources and this suite's Vitest config. Each historical run
gets an isolated npm workspace whose `Clava-JS` source files point to the
current checkout; Vitest's root is set to that overlay. Its ClavaWeaver test
resources start from the current tree, with the
historical Text/Protobuf `GlobalAttributes.js` script and golden staged from
that control checkout. This keeps the old `builtinKind` access paired with its
matching parser and golden while preserving the same test identity and C++
input. The plan and each observation record the fixture hashes and resource
tree hashes before and after the run. The runners stage the supplied runtime
into each run directory. The historical Text runner patches only its staged
parser JAR's dumper tag to its matching control; the checked out
`clang-dumper-release.tag` is not changed.

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
other tests remain pending. The suite and test name pairs must match across all
three observations.

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

## Compare Java parser runtimes

`run_java_runtime_comparison.py` runs the same 116 direct parser tests against
historical Text and Protobuf controls and an isolated eager release checkout.
The exact JUnit identities are checked in at `java-runtime-test-ids.json`; the
Gradle init script fixes the exclusions and accepts no workload arguments.
Pass alternate checkouts explicitly when reproducing a release candidate:

```sh
python3 experiments/flatbuffers/suite/run_java_runtime_comparison.py \
  --eager-checkout ~/.cache/ast-flatbuffers-release-validation/eager-release-build/clava \
  --text-checkout ~/.cache/ast-flatbuffers-release-validation/text-build/clava \
  --protobuf-checkout ~/.cache/ast-flatbuffers-release-validation/protobuf-build/clava
```

The runner preflights `testClasses` and hashes the actual Gradle test classpath
before timing. It hashes the classpath content again immediately before each
timed observation and after the test process exits; any mismatch against the
preflight or between the pre-run and post-run fingerprints invalidates that
observation. It then makes three sequential observations per runtime in
rotated order, with one Gradle worker and one test JVM fork. Every process has
`CCACHE_DISABLE=true` and a PATH link farm that omits ccache and aliases to it.
JaCoCo is disabled for these comparison invocations only.
The historical Text checkout's Gradle build needs its sibling dumper schema
generator, so preflight and observations pin `FLAT_NATIVE` to that same
source-identified control checkout and record the generator, schema, and
FlatBuffers `flatc` hashes. Older controls do not emit the current
`selected-release.json`; their report records the embedded parser release tag,
local tool hash, and complete source checkout snapshots. Eager still requires
the resolved selected-release record and pinned RC tool hash.
The reports capture checkout revisions
and patches, parser source and fixture hashes, release selection, test IDs and
counts, classpath hashes, worker settings, and task states. The Issue 15 golden
remains historical in Text and Protobuf (`$$$$10`/`$$$$20`); eager preserves
the source operands as `$10`/`$20`.

`testcase_duration_s` is the sum of JUnit testcase durations and is the
test-only measure. `gradle_wall_s` is reported separately and includes Gradle
configuration and test-worker startup. The runner makes no wall-time
performance claim. All nine observations run serially; do not run this
comparison beside another benchmark or parser test build. Results go in a new
timestamped directory under `results/`.
