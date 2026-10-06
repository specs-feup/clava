# Protobuf optimizations missing from FlatBuffers

Audited on 6 October 2026 by GPT-6-Luna with xhigh reasoning; key findings checked against source by the parent agent.

## Scope and revisions

Read-only source and history audit. I compared the actual branches and worktrees; their HEADs match the supplied frozen controls:

| Repo | FlatBuffers | Protobuf |
|---|---|---|
| clava | cfe5caaf8f56cad93ffd7462327e4a3cdbe8006c | ff5e58afa96a07b18a3d2bd2e40a00a59bb28442 |
| specs-java-libs | fd41aceb8336d2b5e026abb2deee1a514a778be5 | 19c8e3e4c81dbc77a89d77cd7ab2bc8bfc3844fa |
| lara-framework | b780d0b8ba0aac2670ad5aa98998883a2a4f8030 | b780d0b8ba0aac2670ad5aa98998883a2a4f8030 |
| clang-dumper | 158080298c6043fd6bc3ee7037a74874fd76567c | ab9d0238bd9c27eb9f156fd19967d16025137a37 |

I left both dirty Clava release selectors, the Protobuf Lara patch, and untracked Python caches untouched. I did not fetch, build, run tests, or change runtime sources. This inventory changes no runtime source. No new performance measurements were taken for this audit.

## Findings that are absent or narrower in FlatBuffers

### 1. Avoid the second forced GC during memory reporting

Commit cea5be9123be8508805c03be381a26af796ae7ff changes SpecsSystem.getUsedMemory(callGc) to call System.gc() only when callGc is true. [Protobuf change](https://github.com/specs-feup/specs-java-libs/commit/cea5be9123be8508805c03be381a26af796ae7ff). In FlatBuffers, SpecsSystem.java:510-517 in specs-java-libs still makes the conditional call at line 512 and an unconditional second call at line 515. Protobuf's current SpecsSystem.java:511-517 has only the conditional call.

This reaches the Clava path: CodeParser.java:63-66 defaults SHOW_EXEC_INFO to true, and ParallelCodeParser.java:265-269 calls getUsedMemory(true) after AST processing. With explicit GC enabled, this is one fewer full-GC request per parse. With callGc=false, Protobuf now avoids a collection entirely.

Priority: high for runs that keep execution diagnostics enabled; portable and low risk. The reported GC-enabled suite difference is confounded by this asymmetry, but its exact time cost was not isolated.

### 2. Bypass the synchronized shared cache on every node

Commit 166b6607b152ba4fb3e48cfd997f28fb8cb0d89b adds a per-reader fast path in ProtoNodeDataReader.java:94-100 and 122-135: a static ConcurrentHashMap caches each Clava class's StoreDefinition.

[Protobuf fast path](https://github.com/specs-feup/clava/blob/ff5e58afa96a07b18a3d2bd2e40a00a59bb28442/ClangAstParser/src/pt/up/fe/specs/clang/wire/ProtoNodeDataReader.java#L94). Flat CompleteReader.java:208-214 calls GeneratedNodes.descriptor(...).read(...) for every node. SchemaRuntime.java:104-129 then calls StoreDefinitions.fromInterface(...) for every node. That shared method already caches definitions (jOptions StoreDefinitions.java:29-53), so the missing piece is not reflection caching itself. Its CachedItems.get(...) constructs an AtomicBoolean, takes a synchronized path, and increments an AtomicLong hit counter on every hit (SpecsUtils CachedItems.java:54-80). ParallelCodeParser defaults PARALLEL_PARSING to true and submits file parses to a fixed thread pool (ParallelCodeParser.java:61-63, 132-148). The Protobuf map avoids that shared lock and hit-counter work after each class's first node.

Priority: medium to high for multi-file parallel parses, source-supported but not isolated in a benchmark. The extra map is keyed by the finite set of Clava node classes. Protobuf also caches enum conversions in this commit. FlatBuffers uses direct ordinal mapping with Enum.values()[value] (scripts/generate_clava_wire.py:122-150), avoiding Protobuf's name-normalization search. These are different strategies. The values() calls may create enum-array copies; this audit did not measure whether the JIT removes those allocations or whether a cached array improves performance. Copying Protobuf's enum map is therefore not an established optimization for FlatBuffers.

### 3. Generated per-payload visitor instead of a generic binding loop

Commit 1229e6c9c89167a5a252f0ff5795eec4c94fd609 generates protobuf typed visitors. The generator emits per-message visit methods that call typed getters and reader helpers directly (tools/generate_proto_java_bindings.py:161-197, 264-292), and rejects getAllFields()/descriptor-based protobuf field lookup in generated output.

The commit's named reflection-to-getter win does not transfer literally: FlatBuffers already generates direct typed accessor lambdas, not reflective field traversal (scripts/generate_clava_wire.py:39-48, 69-89). The remaining difference is that FlatBuffers stores those lambdas in static Descriptor/Binding lists, then loops through and dispatches them through generic BiFunctions per node (SchemaRuntime.java:91-129). Protobuf generates a method body for each payload instead.

[FlatBuffers binding loop](https://github.com/specs-feup/clava/blob/cfe5caaf8f56cad93ffd7462327e4a3cdbe8006c/ClangAstParser/src/pt/up/fe/specs/clang/wire/SchemaRuntime.java#L104).

Priority: exploratory. Direct accessors are already present in FlatBuffers; replacing the remaining generic per-field loop may save dispatch and branch work, but no isolated measurement shows that it does. Keep the schema validations and presence checks.

## Differences that should not be counted as missing general optimizations

- **Bounded producer batches:** Protobuf eeced3dd595568d72e8fc8562038635912cdb3da changed per-record envelopes into roughly 64 KiB chunks. FlatBuffers already packs records into size-prefixed blocks and flushes at 64 KiB (FlatStream.h:14-42, FlatStream.cpp:54-60; implementation began in e69a77f2838581bcfdbe7d025f389d3ed9e85b0b). The Protobuf reader loops over chunk records (ProtoAstReader.java:213-225); FlatBuffers loops over block records (CompleteReader.java:71-90). This is an equivalent batching strategy.

- **Incremental chunk-size accounting:** Protobuf ab9d0238bd9c27eb9f156fd19967d16025137a37 tracks each encoded Record's contribution in pending_chunk_bytes (ProtoStream.cpp:86-115), avoiding repeated ByteSizeLong() scans while a chunk is assembled. FlatBuffers checks FlatBufferBuilder.GetSize() after packing a record (FlatStream.h:38-42); its writer has no equivalent repeated ByteSizeLong() walk to eliminate. Treat this as an encoding-specific implementation, not a missing FlatBuffers feature.

- **Syntax-only validation:** Protobuf commits da00ef63495dcc0c2f51c7cc49d8fce7d8a99d0f and 3fb2b9350cfbf4eaa79773f7d801d94bc23d7cf2 add a protocol-free SyntaxOnlyAction path. FlatBuffers already has the corresponding native option and parser path (clang-dumper src/tool.cpp:26-28, 282-288; ClangAstDumper.java:301-303, 421-424), from 07f28158e2b9403378a46a07d4348cd5981fc8d3 and 01c6b739e5606af12734a3a21c63a45ce5dd5e67.

- **Release compiler settings:** Both native build workflows set CMAKE_BUILD_TYPE=Release (.github/workflows/build.yml:13, 104-110). I found no branch-specific LTO or extra -O flags in the CMake configuration. Protobuf adapter generation changes build inputs, not the selected optimization level.

## Conditional cache/compression policy

This is a real runtime-path difference, but it is not a portable win to copy into FlatBuffers. Protobuf uses an externally Zstd-compressed .pb.zst dump when AST dump caching is active (ClangAstDumper.java:329-338) and sets CCACHE_NOCOMPRESS=true (ClangCcacheAdapter.java:111-113; introduced in 15dec4608b8242662e2e53787805597d7ea10815). FlatBuffers writes a raw .clv2 file and asks ccache to compress it (ClangAstDumper.java:322-330; ClangCcacheAdapter.java:117-118). These are alternative places to compress different bytes. The Protobuf adapter's comment claims its choice reduces miss latency, but this audit found no experiment that isolates compression policy while holding the protocol and other implementation choices fixed. Existing cold/warm suite measurements do not establish the benefit of this policy alone.

AST_DUMP_CACHE defaults to true (CodeParser.java:51-53), but the 2026-10-06 explicit-GC diagnostic had ccache disabled and absent from PATH. Therefore cache/compression settings do not explain that diagnostic's remaining FlatBuffers-Protobuf gap. The branches also use separate cache namespaces, so warm-cache state cannot be assumed comparable. Protobuf commit 6714ac0bca193ac1d763c5e43d8d91d7fe65ed24 changes CCACHE_DISABLE parsing: Protobuf treats 0/false/no as enabled-cache values; FlatBuffers throws for those values. This is a harness-control difference, not a speed optimization.

## Benchmark and source limits

The existing RUNTIME_REGRESSION_INVESTIGATION.md:3-15 records the relevant test evidence. With explicit GC enabled, FlatBuffers was 38.016 s and Protobuf 31.986 s; each observation made 216 heap samples, so the FlatBuffers/Text source requested 432 GCs while Protobuf requested 216. Those are requests, not proof of completed collections. The newer nine-run diagnostic used -XX:+DisableExplicitGC equally: FlatBuffers 27.590 s versus Protobuf 25.839 s. It left heap diagnostics and FlatBuffers verification on and disabled ccache. That residual 1.751 s is not explained by completed explicit collections, but the test is not the earlier App-construction timer and does not isolate any individual source change.

The base SHOW_EXEC_INFO default is true in both branches. Earlier frozen benchmark overlays set it false, suppressing the call path above; those numbers do not describe the default production setting. The Protobuf reader also times each decoded frame and each envelope's construction (ProtoAstReader.java:198-207, 240-242); printing the JSON is opt-in (ClangAstDumper.java:435-440). FlatBuffers adds structural WireVerifier.verify and GeneratedNodes.validateRecord checks before consuming each block/record (CompleteReader.java:71-90). These are additional cost differences, not evidence for removing malformed-input verification.

One countervailing memory difference: FlatBuffers clears parser-only lookup maps after postprocessing (CompleteReader.java:150-159; ParallelCodeParser.java:257). The Protobuf branch removed that cleanup call when replacing the reader. The maps become collectible when the parse data is unreachable, but Protobuf can retain them through more of the parse. No peak-memory A/B in this audit measures that effect.

## Porting priorities

The clearest FlatBuffers port candidates are the single-GC memory-reporting behavior and the Protobuf reader's concurrent StoreDefinition fast path. A generated per-payload reader body is a plausible further experiment, but FlatBuffers already uses typed accessors. Chunking and syntax-only mode are already covered by FlatBuffers equivalents. Compression policy is conditional and needs matched ccache measurements before treating it as faster.

