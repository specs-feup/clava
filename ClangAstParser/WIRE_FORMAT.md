# Eager FlatBuffers AST transport

Clava consumes completed, size-prefixed FlatBuffers v2 files. It creates the complete AST and resolves references before returning it. There is no lazy mode or Text fallback. The preserved `lazy-flatbuffers-experiment` branch contains the earlier experiments.

## Release selection and toolchain

`clang-dumper-release.tag` selects the producer release. The build downloads that release's `clang-dumper-release-manifest.json` and `clang-dumper-wire-schema-v2.zip`; it checks the archive SHA-256 and the canonical schema SHA-256 before generating anything. The runtime checks the native binary hash and rejects streams whose schema hash differs from the compiled bindings.

An absolute local build directory is supported for development. It must contain the same manifest and schema bundle produced by the dumper build. A missing or incompatible manifest is an error. There is no implicit sibling schema checkout.

The compiler and Java runtime are pinned to FlatBuffers 25.12.19, upstream commit `7e163021e59cca4f8e1e35a7c828b5c6b7915953`. The compiler archive and exact-commit runtime source archive have checked SHA-256 values in `scripts/resolve_wire_release.py`; the compiler version and runtime version marker are also checked. The Java runtime is compiled from that archive because this version has no Maven Central runtime artifact. Downloads are cached under Gradle's user cache, with hashes checked before reuse. Linux and Windows ARM64 build the compiler from the same verified source archive and require CMake and a C++ compiler.

The producer owns the schema and native code generator. Clava owns the Java binding generator and verifier generator. Neither repository's generator imports the other repository's scripts.

## Adding a field or node

1. Add the producer schema field and native emitter in `clang-dumper`. Give node payload tables the exact name `JavaClassNameData`. Fields use snake case; the corresponding public Java `DataKey` name uses its camel case spelling. Rename existing mismatches rather than adding aliases.
2. Add or update the Java node's `DataKey` in `ClavaAst`. `BindingInventory.java` inspects the compiled classes, including inherited keys and generic types. Generation fails when a class, key, enum or field type does not match. Node references use the actual key type to select required, optional or list reference resolution. Annotate a Clava node key with `@NullableNodeReference` when absence must become a typed Clava null node rather than an empty Optional.
3. Keep protocol structure in the schema. Scalar presence, required offsets, discriminated unions, source locations and reference IDs are validated before applying records. Extend the consumer's compound-value decoder when introducing a new compound field type; do not attach Java class or key annotations to the producer schema.
4. Build the dumper, select its local build directory for development, and run `gradle -p ClangAstParser generateCompleteWire`. This resolves the schema, runs the pinned compiler, inspects Java keys, and regenerates the bindings and structural verifier.
5. Run the wire tests and both parser suites, then the cross-TU, source round-trip, malformed-input and memory gates described in `experiments/flatbuffers/README.md`.
6. Run `gradle -p ClangAstParser updateGeneratedWireInventory`, review `generated-wire.sha256`, and commit it with the schema/binding changes. `verifyGeneratedWire` fails if regenerated Java sources differ from this inventory. `check` and CI invoke that verification.

## Publishing a compatible release

Publish the dumper binaries, generated enum bundle, schema bundle and manifest from one producer revision. The producer release workflow creates the schema bundle and manifest with its shared manifest script. The manifest records the FlatBuffers version and commit, schema version and canonical hash, schema archive hash, and each binary's hash.

Select that published tag in Clava, regenerate the inventory, and validate the release through the ordinary build and parser runtime. A schema change requires regenerated consumer bindings; mismatches fail explicitly. Existing releases that lack the v2 schema manifest cannot serve this consumer.

## Caching and cleanup

AST ccache entries are isolated by FlatBuffers version, upstream commit, schema hash and executable hash. Every present `CCACHE_DISABLE` value disables Clava's ccache wrapper; `0`, `false` and `no` are rejected, so unset the variable to enable caching. Clava's explicit disable takes precedence over a conflicting `CCACHE_NODISABLE`. Bypass measurements must use the same policy for each control. Executable digest metadata is limited to 128 file identities; temporary resource paths cannot grow that cache indefinitely.

Mapped windows are released when replaced and when the reader closes, including rejection paths. Parsing releases temporary native files after importing the eager graph. The repeated-parse probe checks collected ASTs, retained heap, mapped files and temporary folders; these checks are required in addition to a short-process peak memory measurement.
