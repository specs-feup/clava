# FlatBuffers and lazy Clava properties

Measured research prototype, 10 September 2026. This directory contains the Java reader,
validation and benchmark drivers. The companion `clang-dumper` branch is `ast-flatbuffers`.

The proposal and measured conclusions are on [DraftLink](https://draftlink.lmsousa.workers.dev/d/dnyeY92urT89).

## Decision

The architecture works, including eager reference resolution, individual memoization,
copying, mutation before decoding, and collection after detaching a translation unit.
The measurements support continuing with a schema-based format. They do **not** justify
making almost every field lazy by default or merging this partial protocol.

On the two larger C fixtures, lazy FlatBuffers reduced the native-plus-Java workload by
7–11% on misses and 10–13% on hits against the existing compressed-text **file** path.
Small C++ was essentially unchanged. Laziness itself saved only 3.7–4.4% of the consumer
workload relative to eager FlatBuffers on those larger fixtures. It increased retained
heap after normal Clava postprocessing. Most of the total gain predates laziness: dense
IDs, file interning and typed import also change between the text and binary modes.

Prefer an eager schema-based reader as the next integration baseline. Keep the small
memoization adapter available for expensive properties that real scripts seldom read.
Source locations look like a poor blanket-lazy target because postprocessing reads many
of them before scripts can run. Do not replace the production format on this evidence alone.

## What is implemented

- Native incremental writer with independently size-prefixed FlatBuffers records,
  a file table, dense IDs and generated C++ builders. Raw fallback chunks are at most 64 KiB.
- Ten typed expression/statement kinds. Other node payloads and structure records retain
  the legacy text representation inside raw records. Qualified/template declaration
  references also fall back. This is a hybrid format, not a complete AST schema.
- Generated Java accessors, read-only mapping in 64 MiB windows with boundary overlap,
  and a bridge into Clava's real parser. No whole-dump Java byte array is required.
- All node references eager, including references in fallback compound payloads.
  Clava builds the TU/App, runs its normal postprocessing and text/comment reconstruction,
  and then discards construction indexes before query or code generation.
- `MemoizedDataStore` uses a shared unloaded marker in ordinary `ListDataStore` slots.
  There is one decoder per backed node, no `Lazy` wrapper or supplier per field, and no
  retained node lookup. Typed writes replace slots without decoding the old value.
  Raw writes decode when necessary to return their previous value, as the API requires.
- Loaded values use existing Clava storage and copy policies. Pending mutable values
  decode independently in a copy. Stores have the same single-threaded mutation
  assumption as `ListDataStore`.
- An immutable backing record and the file table remain reachable from each lazy decoder.
  The decoder does not retain a parser, construction map or owning Java node.

The existing `Lazy` implementation would add a wrapper and retained supplier per field.
The storage adapter implements the same memoization semantics with much less per-field
bookkeeping. Its small `ListDataStore` hooks are the only shared-library changes.

## Time

Milliseconds, median of 12 measured observations per mode after three warmups. Modes
were randomized within each round, run serially on the same machine. `cold` in the CSVs
means a ccache miss, not a cold filesystem page cache. All measured warm trials were direct
cache hits; each persistent cache has one setup miss in its counters.

| Fixture | Text miss | Lazy miss | Packed lazy miss | Text hit | Lazy hit | Packed lazy hit |
| --- | --- | --- | --- | --- | --- | --- |
| NAS BT, C | 769.4 | 688.3 | 704.3 | 660.6 | 577.1 | 579.7 |
| Small C++ | 18.8 | 19.1 | 18.9 | 7.0 | 6.8 | 7.1 |
| Templates, C++ | 348.2 | 345.5 | 344.0 | 33.3 | 31.3 | 31.1 |
| NAS LU, C | 748.6 | 694.8 | 699.6 | 660.3 | 593.4 | 592.4 |

Each end-to-end observation includes native execution or cache restoration, file import,
TU/App construction, production postprocessing, a Java traversal of nodes and reference
fields that reads names/operators/types, and generated-code emission. It excludes JVM
startup, JavaScript engine startup and user scripts. The traversal also computes a digest;
it is not a full Clava-JS Query API benchmark. Phase medians must not be added to derive
a total; the total is timed per observation.

`Text` is the existing native zstd-text output, with extra ccache compression disabled.
`Lazy` maps uncompressed FlatBuffers with ccache compression disabled. `Packed lazy` uses
the same reader and uncompressed materialized file but lets ccache compress persistent
storage at level 1. Eager and lazy warm modes share the same FlatBuffers cache artifact.
Native format selection is explicitly part of the experiment's ccache extra-file key.

Consumer-only time isolates eager versus lazy decoding of the same immutable artifact:

| Fixture | Text | Eager FlatBuffers | Lazy FlatBuffers |
| --- | --- | --- | --- |
| NAS BT, C | 651.0 | 591.4 | 569.5 |
| Small C++ | 4.2 | 4.3 | 4.0 |
| Templates, C++ | 34.8 | 33.5 | 31.8 |
| NAS LU, C | 655.1 | 606.6 | 579.9 |

For NAS BT, initial import fell from 452.8 ms for text to 205.9 ms for lazy FlatBuffers,
but TU construction/postprocessing grew from 89.3 to 271.1 ms. Comparing only initial import
would exaggerate the benefit. Of 233,101 selected lazy properties, 1,817 were decoded after
import, 35,605 after TU construction, 41,199 after traversal and 47,380 after codegen.
Only 20.3% were read even at the end, yet the runtime saving over eager import was modest.
Avoided fields differ greatly in cost.

`SourceLocation` currently canonicalizes its path in every constructor, including paths
already resolved through the binary file table. Repeated path resolution is a candidate
for a separate, format-neutral optimization. This experiment did not profile or remove
that cost, so it is not an established explanation of the measured time.

## Memory and storage

Three separate JVMs per mode/fixture. Heap snapshots follow explicit GC outside timed
runs. RSS is the Java process high-water mark and includes mapped pages, JVM code and
other non-heap allocations. It is not the sum of phase heaps or a simultaneous native/Java
system peak. RSS varies between launches, so retain the raw observations.

| Fixture | Mode | Import heap MiB | Ready heap MiB | After codegen MiB | Peak RSS MiB |
| --- | --- | --- | --- | --- | --- |
| nas | text | 53.7 | 34.7 | 34.8 | 310.9 |
| nas | eager | 40.9 | 34.1 | 34.4 | 238.3 |
| nas | lazy | 35.4 | 36.1 | 36.8 | 324.2 |
| nas_lu | text | 48.3 | 31.9 | 32.1 | 291.7 |
| nas_lu | eager | 37.2 | 31.3 | 31.7 | 226.3 |
| nas_lu | lazy | 31.8 | 33.2 | 33.7 | 239.7 |

Lazy import used less heap initially but retained more once the AST was ready. On NAS BT,
lazy peak RSS exceeded text; on NAS LU it was lower. This does not establish a general
memory advantage. After detaching the TU, it was collected in all 36 probes. The larger
fixtures then retained about 8.2 MiB of Java heap. This proves the tested ownership path
can release nodes; it does not prove arbitrary scripts or multi-TU graphs release them.

| Fixture | Text MiB | Dense text MiB | FlatBuffers MiB | Zstd text MiB |
| --- | --- | --- | --- | --- |
| NAS BT, C | 16.488 | 13.455 | 12.866 | 1.594 |
| Small C++ | 0.032 | 0.026 | 0.025 | 0.005 |
| Templates, C++ | 0.947 | 0.775 | 0.770 | 0.127 |
| NAS LU, C | 15.027 | 12.430 | 11.221 | 1.403 |

The uncompressed binary dump is about 19–25% smaller than original text, but 5–8 times
larger than native zstd text in these fixtures. Much of the reduction against raw text
also appears in the dense-text control. Do not attribute it all to FlatBuffers.

Compression and mmap are compatible when ccache owns compression: NAS BT's persistent
FlatBuffers cache occupied about 760 KiB, and NAS LU about 700 KiB, while restoring ordinary
uncompressed files for mapping. These are ccache-reported storage sizes including its
container/manifest accounting, not exact payload-only byte counts. Per-execution disk
and resident mapped pages still need the larger restored artifact.

## Correctness and limits

All six fixtures passed native generated-buffer verification, normalized legacy-wire
reconstruction, Java field/graph equivalence, generated-code equality, and the mutation/copy
checks. IDs are normalized; the `CXXMethodDecl.RECORD_ID` string is checked against its eager
reference before normalization. Runtime context/origin bookkeeping is excluded from graph
hashes. Fixtures include C11, C++17, templates, macros, literal spelling, comments, pragmas,
line continuations and a header path with spaces. Every timed trial also produced the same
code digest within its fixture.

The shared-library suite passed 1,743 tests with two skipped, and its coverage gate passed.
The native Release tool and verifier built. The native plugin and cross-platform builds
were not tested. A sparse 2,348,810,800-byte input passed the mapping-boundary check, including
records beyond 2 GiB. That tests address handling, not GCC-scale AST construction or memory.

Typed coverage is workload-dependent: NAS BT has 35,845 typed records, while the template
fixture has only 333. Fallback behavior is part of every timing above. No full Clang test
corpus, Clava-JS suite, multi-TU application or all-language-version validation was run.
Exact original-versus-regenerated dump identity remains unproven; matching generated code
between import modes is a narrower check.

Dense IDs currently restart per TU and the pilot runs one TU per process. Production
integration must give Java IDs a TU namespace before assembling a multi-TU application.
The reader limits records to 16 MiB and IDs to 10 million for this pilot. Bounded record
construction is compatible with incremental reading, but the ccache path still waits for
output completion. There is no new simultaneous producer/consumer benchmark here, so these
results do not establish break-even against uncached streaming.

Both sides generate binary accessors at build time. The release manifest/schema hash
contract, comprehensive presence/reference validation and generated Clava field mapping
are future work. FlatBuffers supports required non-scalar fields and scalar presence;
application invariants such as matching a node kind to its payload/reference types still
need validation. This pilot's schema is not that final contract.

## Reproduce

Start with sibling SPeCS and native worktrees named `ast-flatbuffers` and
`clang-dumper-ast-flatbuffers`. The SPeCS workspace needs its normal Clava Java distribution
under `Clava-JS/java-binaries/lib`. Commands below run from the Clava repository.
The measured environment used OpenJDK 26.0.1, LLVM 18, ccache 4.12.3, Linux x86-64 and an
Intel i7-9700. All Java timing/memory processes use `-Xms128m -Xmx512m`.

```sh
python3 experiments/flatbuffers/bootstrap.py
```

Configure/build the native tool as documented in its `wire/README.md`. Then:

```sh
python3 experiments/flatbuffers/build.py
python3 experiments/flatbuffers/prepare.py
python3 experiments/flatbuffers/validate.py
python3 experiments/flatbuffers/run_bench.py
python3 experiments/flatbuffers/memory.py
```

`FLATBUFFERS_ROOT` overrides the SDK location and `FLAT_NATIVE` the native worktree.
The SDK is pinned to upstream commit `7e163021e59cca4f8e1e35a7c828b5c6b7915953`.
The experiment compiles generated Java sources, two shared-store classes and a
`ClavaNodes` overlay against the existing distribution; it does not install a production
reader or change normal Clava startup. The overlay chooses a temporary dense map for
FlatBuffers. The public scripts never remove an unrelated ccache directory.

Run shared-library checks from `specs-java-libs/jOptions`:

```sh
gradle test -x jacocoTestReport -x jacocoTestCoverageVerification
gradle jacocoTestCoverageVerification
```

`results/` and `build/` are ignored. Durable final CSVs, summaries, cache counters,
validation logs, memory observations and environment details are in `measurements/final/`.
`measurements/initial/` records the first BitSet-based memoization adapter, before replacing
it with a marker in existing slots. It is historical tuning evidence, not another workload.

The initial bases were Clava `603997af2fb1a7f9cdd1b418b15772f892494aca`,
specs-java-libs `44ab19744fd4902f02ed85e1079e6214ee00dfef`,
lara-framework `b780d0b8ba0aac2670ad5aa98998883a2a4f8030`, and
clang-dumper `bc498f5cedb88239062eef21a9669bc9bddb0ff7`.
