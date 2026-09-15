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
`a5b3b1a6d2f50a2f739caca2378842e34b1de520`, protobuf clang-dumper
`da00ef63495dcc0c2f51c7cc49d8fce7d8a99d0f`, baseline Clava
`603997af2fb1a7f9cdd1b418b15772f892494aca`, and baseline clang-dumper
`bc498f5cedb88239062eef21a9669bc9bddb0ff7`. Results are retained at
`/tmp/protobuf-final-combined-identity.dlRrEn/results`:

| fixture | first graph SHA (baseline = protobuf) | second graph SHA (baseline = protobuf) | generated source baseline = protobuf (first / second) |
| --- | --- | --- | --- |
| `nas_lu` | `cf289dee6e9ba0388ca652ce9fefba25ad10330cdb6ff05786bd7d8c1d46e758` | `d0b3488e001e27f4b547802866aa4c26de63b1a53b7be9d017619f356c1eab35` | yes / yes |
| `comment` | `17259f34a56a333ca8d16cc85e1132c78075703da161cd14781b56c310d24961` | `70a4554714dcaf1b19fcb8fd50e9510eb4ff7075ca5d842e2390ee27db201e51` | yes / yes |
| `pragmas` | `2b671e341d4bfdae759bd8d2accd96857e24a877a20c7498a067688ea529da43` | `18607a2fe63a8747e212cc0fd15c0ddb044d90dc56a4b71a393b664a68686668` | yes / yes |
| `macro` | `6f6506286845e77ee1704ce479a258488c226cabd26950d0d58492c6755bd11e` | `8b0743acc44b278b9a1a05b3aa4ea2c9f98a6d31053324f7aad8c850b7466f4c` | yes / yes |
| `literals` | `dd420c28ab00fcddd55963ff39f1f8ac9ee3fdbb269c80c3fa0422296b70a6cd` | `20715fae9a533462b7e2172dfab4027330ea017e52ca6d1f1488f69246c50942` | yes / yes |
| `source_locations` | `3d3e9aa20a7f7df032f22fe52650ec21b2cc56e00724dd175476db336caec87c` | `619f6942ac7af0763b915accbfdc6f2d3d1281a5674cbd3f46c82520dca650a7` | yes / yes |

The full values are in `identity.json`; all 12 baseline/protobuf helper
commands returned zero. The runner returns one only because it retains the two
known within-runtime non-idempotent source fixtures, `comment` and `literals`.
The exact cross-runtime generated-source hashes were, by fixture and pass: NAS LU
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
hash was identical between runtimes. The final all-field comparison reports
`graph_mismatches=[]` and `graph_reparse_mismatches=[]`: every fixture matches
at both graph checkpoints. `comment` and `literals` remain byte-identical
between runtimes but are not idempotent within either runtime (`first` and
`second` source hashes differ); this is retained separately in `failed` and
does not affect the baseline/protobuf equality result.

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
