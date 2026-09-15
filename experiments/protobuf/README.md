# Protobuf protocol validation and measurements

This directory contains experiment tooling only. It does not implement a
second AST, parse protobuf records, or edit `ClangAstParser/clang-dumper-release.tag`.
Production tests and helpers own protobuf decoding and Clava AST construction;
these scripts run those helpers, compare their exported inspection data, and
measure the real Clava-JS suite.

The comparison has two explicit inputs: a frozen text-protocol baseline build
and a frozen protobuf build. Never point both options at the same runtime or
native executable. Builds, Gradle installation, schema generation, resource
downloads, and heap probes must finish before timing a suite cell.

## Inputs and revision capture

Build each checkout into an owned staging directory, then record the native
executable SHA-256 and all nested repository revisions. The current matched
text native snapshot is:

```text
/home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache/build/tool
```

The protobuf command must use the executable built from the protobuf branch.
The runner stages each supplied `java-binaries` directory and changes only the
`clang-dumper-release.tag` entry inside the staged `ClangAstParser.jar`. The
checked-out tag file is never changed. Each `summary.json` records the native
SHA, runtime-jar manifest, dumper/runtime Git status, temporary filesystem,
command, environment, and all raw output paths.

The normal build must not depend on an absolute worktree path. A staged runtime
is used here only to make a measurement reproducible while the production
release-manifest work is reviewed separately.

## Real Clava-JS matrix

The full suite is `npm exec --workspace @specs-feup/clava -- vitest run`. The
matrix executes both builds in three states, with three repetitions and rotated
implementation order:

```text
baseline cold, baseline warm, baseline bypass,
protobuf cold, protobuf warm, protobuf bypass
```

On the next repetition the implementation order rotates. Cold and warm share
the same owned Java temporary root and therefore the same ccache below
`clang_ast_exe_<user>/clang-dumper-ccache` for the baseline or
`clang_ast_exe_<user>/clang-dumper-protobuf-ccache-v1` for protobuf. Bypass receives a new temporary root and
sets `CCACHE_DISABLE=true`. A cold run requires an empty root. A warm run must
find the populated paired root. The matrix writes a plan, progress manifest,
per-cell driver log, Vitest JSON/log, flattened per-test timings, GNU `time`
output, ccache counters, parsed protocol metrics, and a final results manifest.
It continues after test failures so diagnostics and outliers are retained.

Example, using two already-built distributions:

```sh
cd /home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clava
python3 experiments/protobuf/suite/run_matrix.py \
  --baseline-dumper /home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache/build/tool \
  --protobuf-dumper /path/to/protobuf/clang-dumper/build/tool \
  --baseline-runtime ./Clava-JS/java-binaries \
  --protobuf-runtime /path/to/protobuf/clava/Clava-JS/java-binaries \
  --baseline-dumper-repo /home/lmsousa/Documents/Projects/SPeCS/clang-dumper-ccache \
  --protobuf-dumper-repo /path/to/protobuf/clang-dumper \
  --output-root "$PWD/experiments/protobuf/suite/results/matrix-$(date -u +%Y%m%dT%H%M%SZ)"
```

Run one cell while developing the production implementation:

```sh
python3 experiments/protobuf/suite/run_suite.py \
  --implementation protobuf --mode cold \
  --dumper /path/to/protobuf/clang-dumper/build/tool \
  --runtime /path/to/protobuf/clava/Clava-JS/java-binaries \
  --output-root "$PWD/experiments/protobuf/suite/results/dev"
```

The default JVM selector is `-Dclava.astWire=protobuf` for the protobuf build
and `-Dclava.astWire=text` for baseline. Use `--wire-property` and
`--*-wire-value` when the production selector has a different name. The
current file-based ClangAstDumper waits for the native process and only then
opens the completed dump. Consequently every suite cell is labeled
`completed-file`, with `producer_consumer_overlap=false`; it must not be
described as streaming overlap.

The runner parses either `CLAVA_AST_METRIC {json}` or
`PROTOBUF_METRIC {json}` lines. Numeric fields named `native_ms`, `read_ms`,
`decode_ms`, `ast_ms`, `tu_ms`, `construction_ms`, `dump_bytes`,
`cache_bytes`, `nodes`, `references`, `deferred`, and `records` are retained
and summed as aggregate occupancy. These sums can span parallel parser jobs and
are not wall-time components. Wall time is the outer `/usr/bin/time` process
span around Vitest; peak RSS is GNU time's process high-water mark. Dump/cache
bytes are measured independently from the run-owned temporary tree and ccache.

Supply a JSON list of known failures when desired:

```json
["CxxTest OmpThreadsExplore", "CudaTest Cuda"]
```

Known failures remain in `failed_tests` and `expected_failures`; only names not
in that list become `unexpected_failures`. Missing summaries are recorded as
excluded infrastructure trials in `matrix-results.json`, rather than silently
discarded. Do not exclude a CUDA or header failure without retaining its log
and stating the reason in the report.

## Graph, source, and identity validation

Production validation helpers should export this small inspection JSON shape:

```json
{
  "nodes": [
    {"wire_id": "...", "class": "...", "fields": {},
     "children": ["..."], "references": {"decl": "..."}}
  ],
  "auxiliary": []
}
```

Compare the baseline and protobuf exports with:

```sh
python3 experiments/protobuf/validation/compare_graph.py \
  --baseline /owned/results/baseline.graph.json \
  --protobuf /owned/results/protobuf.graph.json \
  --output /owned/results/graph-comparison.json
```

The comparator rejects duplicate or dangling references. It normalizes only
`wire_id` and pointer values below `children` and `references`, assigning IDs
in node order. It does not normalize source locations, names, paths, literal
spelling, comments, pragmas, macro data, field values, or auxiliary records.
This is the only permitted implementation-ID normalization.

The source-fidelity runner reuses files from `ClangAstParser/test-resources`,
`ClangWeaver/resources`, and `clang-dumper/test/inputs`; it never copies or
rewrites fixtures. Commands are shell-free templates with `{source}`, `{std}`,
`{output}`, `{work}`, and `{fixture}` substitutions and must write the generated
source artifact to `{output}`:

```sh
python3 experiments/protobuf/validation/run_fidelity.py \
  --baseline-command 'java -cp /path/to/checks BaselineFidelity {source} {output} {std}' \
  --protobuf-command 'java -cp /path/to/checks ProtobufFidelity {source} {output} {std}' \
  --output-root /owned/results/fidelity
```

Generated source is compared exactly. The identity runner invokes a production
parse/generate/reparse helper whose template receives `{source}`, `{first}`,
`{second}`, `{work}`, and `{fixture}`:

```sh
python3 experiments/protobuf/validation/run_identity.py \
  --baseline-command 'java -cp /path/to/checks BaselineIdentity {source} {first} {second}' \
  --protobuf-command 'java -cp /path/to/checks ProtobufIdentity {source} {first} {second}' \
  --output-root /owned/results/identity
```

Both generated trees are hashed by relative path and exact bytes. This catches
identity drift after a no-op parse/generate/reparse cycle and does not pretend
compiler equivalence is source fidelity.

## Required-field and malformed-record checks

Required-field presence, truncated framing, invalid references, and incompatible
protocol/schema versions are production parser tests. This wrapper makes those
tests callable and preserves their output without embedding a duplicate parser:

```sh
python3 experiments/protobuf/validation/run_protocol_checks.py \
  --checks required,truncated,reference,version \
  --command 'gradle -p /path/to/clava :ClangAstParser:test --tests pt.up.fe.specs.clang.protobuf.ProtocolTest' \
  --output-root /owned/results/protocol-checks
```

The command exits non-zero when production checks fail. The report identifies
the categories requested and links the complete production log.

## Incremental and memory probes

For a large existing input such as NAS LU, provide a producer and consumer
helper. The normal mode runs producer-to-completed-file followed by consumer.
The optional FIFO mode starts both processes before the producer writes and is
the only mode marked as actual overlap:

```sh
python3 experiments/protobuf/validation/run_incremental_probe.py \
  --source /path/to/clava/ClangAstParser/test-resources/c/bench/nas_lu.c \
  --producer 'native-producer --source {source} --output {output}' \
  --consumer 'java -cp /path/to/checks Consumer {input}' \
  --stream-producer 'native-producer --source {source} --output {output}' \
  --stream-consumer 'java -cp /path/to/checks StreamingConsumer {input}' \
  --repeats 3 --output-root /owned/results/incremental
```

The probe retains per-process logs, return codes, output sizes, and elapsed
time. It does not claim a heap bound. Use separate JVM memory probes with
explicit GC outside timed trials to record graph, post-construction, query,
code-generation, and detached/retained heap, and wrap each probe with
`/usr/bin/time -f %M` for peak RSS.

## Clang corpus and report boundary

Use `clang-dumper` branch
`t3code/dumper-coverage-test-strategy` by `git show` or in a separate corpus
checkout. Its runner is:

```sh
python3 test/corpus_runner.py --tool build/tool \
  --corpus /owned/llvm-project/clang/test --out /owned/results/corpus
```

It distinguishes harness skips, CUDA/device environment skips, Clang parse
errors, expected diagnostics, dumper crashes, and clean dumps while collecting
handler coverage. Existing dumper limitations must be listed separately from
protobuf regressions.

The FlatBuffers report remains separate and unchanged:

https://draftlink.lmsousa.workers.dev/d/dnyeY92urT89
