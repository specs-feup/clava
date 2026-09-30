# AST wire audit: implementation and maintenance evidence

Audit date: 2026-09-30. This is a read-only source audit except for this report. No build or benchmark was run. It records implementation facts and counting boundaries; it does not recommend a protocol or assign a weighted maintainability score.

## Scope and checked-out sources

The text reader count uses the `Exploration/clava` checkout at `f787a5e66cc9264ddfbde36defc3d3cacf330b80`. The FlatBuffers reader/writer count uses `ast-flatbuffers` at `febe021fc9d27463c0f1d43c75ede77d08a8bc02`. The protobuf reader count uses the local `ast-protobuf/clava` checkout at `56250d8ab9f85f2800d1badb91b556e454b5fa89`. The benchmark artifact references below are snapshots, not a claim that all checkouts or artifacts are interchangeable.

The audit follows each format from the native writer to the Java read path, then through existing Clava model construction and reference resolution. In particular, adapter-only line counts are not whole-branch maintenance totals: FlatBuffers keeps the text mode available, and both binary readers reuse common Clava model-building code.

## Source footprint counts

Counts are physical `wc -l` lines in the named production files. Blank lines and comments count. Generated source in build directories and schema text (`.proto`, `.fbs`) are excluded. Tests, general Clang/Clava AST code, build scripts other than the named generators, and vendored SDK sources are excluded. These boundaries make the counts reproducible, not directly comparable as effort estimates.

| Checkout / source set | Handwritten adapter LOC | Generated source counted separately | Boundary / caveat |
|---|---:|---:|---|
| Text reader package | 19 Java files / 4,038 LOC | none | Includes shared `ClavaNodeParser` and `ClavaNodes`, as well as line workers and field parsers. The native dumper's existing text-emission code is not included. |
| FlatBuffers branch's retained text reader package | 19 Java files / 4,044 LOC | none | Same named file set as text row, with branch-local versions. Text remains a selectable mode; it is not the Flat eager field decoder. |
| Protobuf native adapter | 12 files / 3,424 LOC | 2 checked-in generated headers / 2,734 LOC | The 12 include `ProtoDispatch.inc` and `ProtoHandlerDeclarations.inc`; no generator/verification task for those two was found in the current CMake/codegen path. Excludes generated `protoc` bindings in the build tree. |
| Protobuf Java reader | 3 files / 1,473 LOC | generated bindings excluded | Counts `FramedProtobufReader`, `ProtoAstReader`, `ProtoNodeDataReader`; excludes shared `ClavaNodeParser`/`ClavaNodes` and their branch-local 489 LOC. |
| Protobuf generator scripts | 2 scripts / 755 LOC | — | Native descriptor adapter generator plus Java descriptor visitor generator; not runtime reader LOC. |
| FlatBuffers v2 native adapter | 10 files / 1,793 LOC | generated `flatc` code excluded | Counts the `Flat*.cpp`/`.h` writer and field adapters only. |
| FlatBuffers Java reader | 5 files / 786 LOC | generated `flatc` accessors and `GeneratedNodes` excluded | Counts `CompleteReader`, `CompoundReader`, `MappedRecords`, `SchemaRuntime`, `WireMode`; excludes shared branch-local `ClavaNodeParser`/`ClavaNodes` (613 LOC). |
| FlatBuffers generator script | 1 script / 139 LOC | — | Physical LOC understate density: the generator uses many multi-statement single lines. |
| FlatBuffers legacy v1 native path | 2 files / 556 LOC | generated header excluded | `WireStream.cpp/.h` is separate from v2, remains compiled behind `AST_WIRE_FLAT`, and is not used by the `flatbuffers-v2` Java mode. |

The text package counted above contains `ClangStreamParserV2`, `ClavaNodeParser`, `ClavaNodes`, `IdToFilenameParser`, `IncludesParser`, `LanguageParser`, `NodeDataParser`, `PragmasLocationsParser`, `SkippedNodesParser`, `TopLevelNodesParser`, `VisitedChildrenParser`, `VisitingChildrenCheck`, `data/{AttrDataParser,ClavaDataParsers,DeclDataParser,ExprDataParser,StmtDataParser,TypeDataParser}.java`, and `util/PragmasLocations.java`.

The FlatBuffers branch retains this selectable text implementation: `WireMode.mode()` defaults to `text`, and `ClangAstDumper` still has the text reader branch. In an active `flat-eager` parse, however, `CompleteReader` maps records through `GeneratedNodes`/`SchemaRuntime`; it does not call the text `NodeDataParser` to decode fields. It calls `ClangStreamParserV2.newInstance(context).getData()` to obtain initialized `ClangAstData`, then uses `ClavaNodeParser`/`ClavaNodes` for the established AST construction and deferred-reference path. Protobuf similarly maps wire fields with `ProtoNodeDataReader`, then uses `ClavaNodeParser` and `ClavaNodes`; it does not use the text field parser. Shared `ClavaNodeParser`/`ClavaNodes` physical counts vary by checkout (608 LOC in the Text tree, 613 in the FlatBuffers tree, 489 in the Protobuf tree); they are included in each full text-package row but excluded from binary adapter-only rows. This is why the 786/1,473 line adapter counts must not be presented as total branch LOC or proof of simpler maintenance.

### Reproduction commands

Run from the `ast-protobuf` workspace root. The shell lists below are the exact counted file sets; the `.proto`, `.fbs`, generated bindings, and build outputs are deliberately not in these commands.

```bash
text=/home/lmsousa/Documents/Projects/SPeCS/Exploration/clava/ClangAstParser/src/pt/up/fe/specs/clang/parsers
wc -l "$text"/{ClangStreamParserV2,ClavaNodeParser,ClavaNodes,IdToFilenameParser,IncludesParser,LanguageParser,NodeDataParser,PragmasLocationsParser,SkippedNodesParser,TopLevelNodesParser,VisitedChildrenParser,VisitingChildrenCheck}.java "$text"/data/{AttrDataParser,ClavaDataParsers,DeclDataParser,ExprDataParser,StmtDataParser,TypeDataParser}.java "$text"/util/PragmasLocations.java

wc -l clang-dumper/src/Clava/Proto{Attributes.cpp,Common.cpp,Decls.cpp,Emit.h,Expressions.cpp,Statements.cpp,Stream.cpp,Stream.h,Support.h,Types.cpp,Dispatch.inc,HandlerDeclarations.inc}
wc -l clang-dumper/src/Clava/Proto{Objects.h,Encode.h}
wc -l clava/ClangAstParser/src/pt/up/fe/specs/clang/wire/{FramedProtobufReader,ProtoAstReader,ProtoNodeDataReader}.java
wc -l clang-dumper/cmake/generate_proto_adapters.py clava/ClangAstParser/tools/generate_proto_java_bindings.py

flat=/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers
flat_text="$flat/clava/ClangAstParser/src/pt/up/fe/specs/clang/parsers"
wc -l "$flat_text"/{ClangStreamParserV2,ClavaNodeParser,ClavaNodes,IdToFilenameParser,IncludesParser,LanguageParser,NodeDataParser,PragmasLocationsParser,SkippedNodesParser,TopLevelNodesParser,VisitedChildrenParser,VisitingChildrenCheck}.java "$flat_text"/data/{AttrDataParser,ClavaDataParsers,DeclDataParser,ExprDataParser,StmtDataParser,TypeDataParser}.java "$flat_text"/util/PragmasLocations.java

wc -l "$flat"/clang-dumper/src/Clava/Flat{Attributes.cpp,Common.cpp,Decls.cpp,Emit.h,Expressions.cpp,Statements.cpp,Stream.cpp,Stream.h,Support.h,Types.cpp}
wc -l "$flat"/clava/ClangAstParser/src/pt/up/fe/specs/clang/wire/{CompleteReader,CompoundReader,MappedRecords,SchemaRuntime,WireMode}.java
wc -l "$flat"/clang-dumper/scripts/generate_complete_wire.py
wc -l "$flat"/clang-dumper/src/Clava/WireStream.{cpp,h}
```

The CMake generation/verification boundary for the two checked-in protobuf headers is explicit in `clang-dumper/CMakeLists.txt:215-319`: `protoc` creates build-tree C++ bindings and descriptor; the Python descriptor generator emits `ProtoObjects.h`/`ProtoEncode.h` into the build tree; CMake compares those outputs with checked-in copies. The Java build similarly pins Protobuf `4.28.3` and protoc `4.28.3`, consumes the schema from the native checkout, and generates Java bindings, a descriptor hash, schema hash, and descriptor-driven Java adapter (`clava/ClangAstParser/build.gradle:8-99,114-140`).

FlatBuffers uses nine modular v2 `.fbs` files. Its Clava Gradle build invokes a Python script with `flatc`, the FlatBuffers SDK, and the native source checkout, then compiles the FlatBuffers Java runtime source tree from that SDK (`ast-flatbuffers/clava/ClangAstParser/build.gradle:91-108`). The bootstrap script checks the SDK commit pinned in `ast-flatbuffers/clang-dumper/wire/README.md:17-19`; a caller that overrides `FLATBUFFERS_ROOT` bypasses Gradle's own version checking because the Gradle task does not validate that checkout. The v2 reader's schema hash is checked in the stream header, but the same README states that publishing the schema/hash through the native release manifest is not implemented (`wire/README.md:44-57`).

## Read/write paths, compatibility, and cache behavior

* Text: `ClangStreamParserV2` registers line workers for node payload, children, files, includes, pragmas, language, top-level IDs, and node data. Field conversion is handwritten in `NodeDataParser` and the family data parsers. This retains readable raw dumps but has no schema compiler or generated per-field adapter.
* Protobuf: `ProtoAstReader` validates the protocol header/version and schema fingerprint and reads bounded length-delimited envelopes; `ProtoNodeDataReader` walks one generated message into the normal jOptions `DataStore` and queues references through `ClavaNodes`, rather than retaining a second AST. The reader is eager and completed-file based. `ClangAstWebResource` also validates manifest protocol ID/version, framing, max record size, schema hash, descriptor hash, producer version, and LLVM major (`clava/ClangAstParser/src/pt/up/fe/specs/clang/ClangAstWebResource.java:186-210`).
* FlatBuffers v2: the native `FlatStream` writes typed records in size-prefixed blocks and rejects legacy text on the v2 writer path. Java's `MappedRecords` scans size prefixes in 64 MiB mapped windows; `CompleteReader` checks the header schema hash and end counts before using generated descriptors to fill existing `DataStore`s. `flat-eager` materializes fields during decode; `flat-lazy` uses `MemoizedDataStore`. Embedded references in compounds are handled eagerly. V2 rejects the separate legacy `AST_WIRE_FLAT` mode (`ast-flatbuffers/clang-dumper/src/tool.cpp:208-263`; `wire/README.md:23-40,59-63`).
* FlatBuffers cleanup: `ParallelCodeParser` releases parser lookup maps and registers the dump folder for JVM-exit deletion whenever `WireMode.enabled()` is true, not only when `WireMode.lazy()` is true (`ast-flatbuffers/clava/ClangAstParser/src/pt/up/fe/specs/clang/codeparser/ParallelCodeParser.java:265-267,417-424`). Eager values are decoded immediately, but this report does not assert that the broader AST never retains any mapped data.
* No format is producer/consumer-overlapped in these Java paths. Each calls `SpecsSystem.runProcess` to completion before opening/reading the completed dump (`ast-protobuf/clava/ClangAstParser/src/pt/up/fe/specs/clang/dumper/ClangAstDumper.java:361-393`; `ast-flatbuffers/clava/ClangAstParser/src/pt/up/fe/specs/clang/dumper/ClangAstDumper.java:368-400`).

Cache/compression semantics are important for interpretation of new measurements:

| Mode | Text / Protobuf | FlatBuffers v2 |
|---|---|---|
| Direct (`CCACHE_DISABLE=true`) | `ClangCcacheAdapter.isAvailable()` returns false, so no ccache wrapper and no native `-ast-dump-compression=zstd`; `.txt`/`.pb` files are uncompressed. The comparison runner verifies zero ccache calls (`run_comparison.py:103-123,160-193`). | Same disabled-cache decision; raw FlatBuffers file, no ccache internal compression. |
| Cold / warm ccache | Native AST output is Zstd compressed to `.txt.zst`/`.pb.zst`; Java wraps the file with `ZstdInputStream`. Ccache is configured `CCACHE_NOCOMPRESS=true` to avoid recompressing that frame (`ClangAstDumper.java:323-348,390-393`; `ClangCcacheAdapter.java:103-114`). | The native v2 file is uncompressed; ccache is configured `CCACHE_COMPRESS=true` for Flat mode and restores an uncompressed file for Java (`ast-flatbuffers/clava/.../ClangCcacheAdapter.java:98-114`; `clang-dumper/wire/README.md:51-54`). |

Do not read the Java metric boolean named `cached`/`cacheRestored` as evidence of a cache hit. In Text/Protobuf it is derived from the cache path being enabled and `CCACHE_DISABLE` being false, not from ccache's per-invocation hit result (`ClangAstDumper.java:402-405,444-462`). A cold ccache invocation can be a miss and include native Clang work; report ccache's aggregate calls/hits/misses separately. The phase timings are per-parser-job occupancy and overlap when translation units run in parallel; do not sum them into suite wall time.

The broad snapshot's manifest at `results/post-gc-20260927T175510Z/published/direct.json` records different runtime manifests: Text 88 jars, Protobuf 89, FlatBuffers 88, with distinct manifest hashes. Treat those as branch artifact identities; do not describe the matrix as a same-binary protocol toggle.

## Upstream ecosystem evidence

Official pages checked on 2026-09-30:

* Protobuf's official support policy documents quarterly releases, Java and C++ support windows, and compatibility policy: [version support](https://protobuf.dev/support/version-support/). The official gRPC introduction says gRPC uses Protobuf by default: [gRPC introduction](https://grpc.io/docs/what-is-grpc/introduction/).
* FlatBuffers' official release page lists v25.12.19 and a signed v25.12.19-2026-02-06 follow-up: [FlatBuffers releases](https://github.com/google/flatbuffers/releases). Its official `flatc` guide lists Java and C++ generators: [`flatc` docs](https://github.com/google/flatbuffers/blob/master/docs/source/flatc.md).
* At page retrieval, the official GitHub repositories showed 72.1k stars / 16.3k forks for `protocolbuffers/protobuf` and 26.5k stars / 3.7k forks for `google/flatbuffers`: [Protobuf repository](https://github.com/protocolbuffers/protobuf), [FlatBuffers repository](https://github.com/google/flatbuffers). This is a dated repository-interest proxy only—not deployment counts, adoption, operational safety, or a protocol ranking.

## Diagnostics after fresh valid measurements

The dual-reader A/B code already has opt-in per-parse `clava.astWireMetrics`: native-or-ccache process boundary, `read_ms`, `decode_ms`, `record_ms`, `reference_ms`, AST construction, bytes, and compression/cache-path flags. The full-suite runner records ccache aggregate hit/miss counters. After a fresh valid group, compare paired per-parse distributions only for identical source/argument identities and the same cache mode; treat cold process time as ccache invocation time, not pure cache restore. Keep direct, cold, and warm results separate. Do not sum overlapping per-TU phases into suite elapsed time.
