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
  --shared-generated \
  --output-root /owned/results/identity
```

Both generated trees are hashed by relative path and exact bytes. This catches
identity drift after a no-op parse/generate/reparse cycle and does not pretend
compiler equivalence is source fidelity.

`--shared-generated` is recommended when comparing graph digests: it gives both
runtimes the same absolute generated-source paths while retaining copied first
and second trees under each implementation's result directory.

The Java probe additionally emits `first_graph_sha256` and
`second_graph_sha256`. These are deterministic all-field graph digests based on
`ast-flatbuffers/experiments/flatbuffers/complete-check/CompleteBenchmark.java`:
the walk includes runtime node classes, child and node-field order, every
populated `DataKey`, and recursively nested `DataClass`, optional, collection,
map, array, and node-reference values. `CONTEXT`, `ORIGIN`, and `PREVIOUS_ID`
are runtime wiring and are excluded. Only `ClavaNode.ID` and node/pointer
identity (including the legacy `CXXMethodDecl.RECORD_ID` value) are replaced by
encounter-order ordinals; source locations, paths, names, literals, comments,
pragmas, and all other values remain exact. Set
`-Dprotobuf.graphTrace=/absolute/path/trace` to retain a per-node diagnostic
trace alongside the digest.

For the direct comparison below, baseline and protobuf were run sequentially
with the same absolute generated `first` and `second` directories. This keeps
the `sourceFile` value comparable while retaining separate parser work/cache
directories. The pinned runtimes were protobuf Clava
`3fb2b9350cfbf4eaa79773f7d801d94bc23d7cf2`, protobuf clang-dumper
`da00ef63495dcc0c2f51c7cc49d8fce7d8a99d0f`, baseline Clava
`603997af2fb1a7f9cdd1b418b15772f892494aca`, and baseline clang-dumper
`bc498f5cedb88239062eef21a9669bc9bddb0ff7`. Results are retained at
`/tmp/protobuf-final-identity-graph-wrapper.BSQhF0/results`:

| fixture | baseline first graph | protobuf first graph | baseline second graph | protobuf second graph | generated source (first / second) |
| --- | --- | --- | --- | --- | --- |
| `nas_lu` | `cf289dee...` | `cf289dee...` | `ffbb3dde...` | `ffbb3dde...` | equal / equal |
| `comment` | `17259f34...` | `f586929b...` | `d11ef561...` | `667378d3...` | equal / equal |
| `pragmas` | `2b671e34...` | `2b671e34...` | `6c402b25...` | `6c402b25...` | equal / equal |
| `macro` | `6f650628...` | `6f650628...` | `f1b24cf1...` | `f1b24cf1...` | equal / equal |
| `literals` | `dd420c28...` | `6da75473...` | `7f0da538...` | `a796d978...` | equal / equal |
| `source_locations` | `3d3e9aa2...` | `3d3e9aa2...` | `0079e245...` | `0079e245...` | equal / equal |

The full values are in `identity.json`; every baseline/protobuf helper
command returned zero (the runner returns one because it reports the two
non-idempotent source fixtures, `comment` and `literals`).
The exact generated-source hashes were, by fixture and pass: NAS LU
`8ebdb29ca3c907a31bb3b424d8b30873adc4e2ef12c7693ec0af0cb2891a79d8` /
`8ebdb29ca3c907a31bb3b424d8b30873adc4e2ef12c7693ec0af0cb2891a79d8`, comment
`393bc958fecbc1f1c5397bfe9b05a9443f5cb05839295866c59cde1ff9d82289` /
`e1c395e05df695af67042f7c75f44b9c6eead6809a75784b51ecd9881c7e60a3`, pragmas
`dfbcebfca00af4039c1d62ba2e0a4bf2d81f65e5a0001d8a8987b852e40583e7` /
`dfbcebfca00af4039c1d62ba2e0a4bf2d81f65e5a0001d8a8987b852e40583e7`, macro
`44bef1949108be8efee60e1d9e09a15a71342d90107697079ca553b0b2e6eea8` /
`44bef1949108be8efee60e1d9e09a15a71342d90107697079ca553b0b2e6eea8`, literals
`a94eba78d920929dd56a4c01b33508460c5af414da7a6de3d2b7f591ad47f6df` /
`b1d225aec80d3eb524a364d0a0ba120b0c476445777cbef694eef12ec27af709`, and
source locations `e31972995af6dc12e8678dfb1f0109e699cfdd678b5265d7a0d89385ed2bfd0f` /
`e31972995af6dc12e8678dfb1f0109e699cfdd678b5265d7a0d89385ed2bfd0f`; each
hash was identical between runtimes.

The demonstrated graph mismatches are limited to populated data that the
all-field check intentionally does not normalize away. On `comment.cpp`, the
baseline has `recordId=node#102` on 201 `CXXMethodDecl` records and
`recordId=node#97` on 10 records, while protobuf omits `recordId`; on the
reparse the corresponding baseline ordinals are `node#101` (201) and
`node#96` (10). The same trace also shows the system-header
`CXXConversionDecl` at node 558 (`operator __sv_type` baseline versus
`operator basic_string_view` protobuf; node 557 after reparse). On
`literals.cpp`, baseline has `recordId` on 38 records (`node#49` 36 times,
`node#131` once, and `node#192` once) while protobuf omits it. NAS LU,
pragmas, macro, and source-locations match at both graph checkpoints. The
comment and literals source trees are byte-identical between runtimes but are
not idempotent within either runtime (`first` and `second` source hashes
differ), which is reported separately from the graph comparison.

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
