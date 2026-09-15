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
the same owned Java temporary root. The runner sets `XDG_CACHE_HOME` to that
root because Clava-JS uses the Linux cache convention for `DUMPER_FOLDER`.
The ccache is therefore below `@specs-feup/clava/clang-dumper-ccache` for the
baseline or `@specs-feup/clava/clang-dumper-protobuf-ccache-v1` for protobuf.
Bypass receives a new temporary root and
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

Supply a JSON list of known failures when desired. The checked-in
`suite/known-environment-failures.json` records the four failures observed in
both builds on the measurement host:

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

## Production heap and identity probe

`validation/ProtobufMemoryIdentityProbe.java` is an experiment-only entry
point. It calls the installed `CodeParser` and `App` classes directly. The
heap mode parses one source, reports the live heap after graph construction,
queries and detached-copy transformation, writes generated code, and then
reports the heap after the parsing method has returned. Each report follows
the `PROTOBUF_HEAP` contract used by `run_heap_probe.py`.

Compile it against a built distribution. The helper does not belong in a
production jar:

```sh
CLAVA=/path/to/ast-protobuf/clava
DIST="$CLAVA/ClavaWeaver/build/install/ClavaWeaver"
PROBE_CLASSES="$(mktemp -d)"
javac --release 17 -cp "$DIST/lib/*" \
  -d "$PROBE_CLASSES" \
  "$CLAVA/experiments/protobuf/validation/ProtobufMemoryIdentityProbe.java"
PROBE_CP="$PROBE_CLASSES:$DIST/lib/*"
```

Run the three-repeat NAS LU probe with GNU time supplied by the existing
wrapper:

```sh
python3 "$CLAVA/experiments/protobuf/validation/run_heap_probe.py" \
  --source "$CLAVA/ClangAstParser/test-resources/c/bench/nas_lu.c" \
  --command "java -Xmx4g -cp $PROBE_CP ProtobufMemoryIdentityProbe --source {source} --work {work}" \
  --output-root /tmp/protobuf-memory-$(date -u +%Y%m%dT%H%M%SZ) \
  --repeats 3
```

The probe also accepts `--identity --first DIR --second DIR`. That path keeps
the first `App` unchanged, exercises a query plus copy/mutation and a
`TreeTransformer` on a detached copy, writes the first source tree, reparses
it, and writes the second tree. Pass the same command template to
`run_identity.py` for a separate baseline runtime. The wrapper compares both
trees by relative path and exact bytes.

On the local protobuf distribution, NAS LU produced 28,218 tree nodes, 49
functions, 158 calls, and 42,280 nodes reachable through fields. The helper
reported these values over three runs:

| checkpoint | repeat 1 | repeat 2 | repeat 3 | median |
| --- | ---: | ---: | ---: | ---: |
| graph-ready heap (bytes) | 42,822,232 | 42,685,168 | 42,745,008 | 42,745,008 |
| post-query heap (bytes) | 42,826,776 | 42,687,728 | 42,749,168 | 42,749,168 |
| post-codegen heap (bytes) | 43,000,008 | 42,861,208 | 42,922,512 | 42,922,512 |
| detached heap (bytes) | 18,253,936 | 18,260,048 | 18,278,360 | 18,260,048 |
| GNU time peak RSS (KB) | 573,020 | 555,608 | 537,284 | 555,608 |

GNU time elapsed was 4.15, 4.26, and 4.12 seconds, with a 4.15 second
median. The generated file was 117,774 bytes. `copy_isolated=1` and
`transform_visited=1` in all three runs. Heap numbers are JVM live-heap
readings after an explicit GC request, not a retained-object graph; detached
heap still includes runtime statics and caches. Peak RSS includes the JVM,
native dumper, and loaded libraries. The three-repeat output is retained by
`run_heap_probe.py` under its selected results directory.

For the separate baseline comparison, the NAS LU identity run used the
baseline runtime from the independent text checkout and the protobuf runtime
from this checkout. Both produced one file and passed exact parse/generate/
reparse comparison with SHA-256
`8ebdb29ca3c907a31bb3b424d8b30873adc4e2ef12c7693ec0af0cb2891a79d8` for both
passes. Other default fixtures still expose existing non-idempotent or
unsupported generated-code cases; those remain failures in
`identity.json` instead of being hidden by this probe.

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
