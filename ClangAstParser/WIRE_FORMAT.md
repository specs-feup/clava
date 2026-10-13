# AST wire format

clang-dumper writes each translation unit as size-prefixed FlatBuffers `Block`s with file identifier `CLV2`.
Its schema is `wire/v2/*.fbs`. Clava uses the official `flatbuffers-java` reflection API to read the binary
schema. Neither `flatc` nor a custom binary-schema parser is needed in the Java build.

`ClavaAst:wireSchema` downloads and verifies the binary schema selected by
`ClangAstParser/clang-dumper-release.tag`. Both code generation and the parser use that same resource.
`WireSchema` loads the schema and its hash; `WireReader` rejects a stream with a different schema hash.

## Data and behavior

`ClavaAst:generateNodeData` runs `ClavaAst/generator/GenerateNodeData.java`. Each payload with data has a
generated abstract base in `ClavaAst/build/generated/sources/node-data`, interleaved with the behavior classes:

```text
DeclaratorDecl -> FunctionDeclBase (generated, abstract) -> FunctionDecl (hand-written)
```

The generated base owns typed storage, public keys for the generic API, `getWireX`/`setWireX` accessors and a
`Values` record. Behavior classes have both their existing constructor and a constructor accepting that record.
Only the behavior subclasses are instantiated by the parser and factory. A schema parent with data must be a
Java ancestor, checked by `javac`. Empty structural tables need no base. For example, Clava represents
expressions outside the statement hierarchy, so the empty wire `StmtData` adds no Java inheritance requirement.

`ClavaAst/node-data.json` records Java field policies: reference types, defaults, enum and compound classes,
nullable references, and Clava-only properties on these classes. The wire stores node references as integers,
so it cannot supply those Java types. Update this contract rather than editing generated files or adding
fields to behavior classes. Field documentation is carried into generated code.

`NodeDataStore` is the compatibility view for generic operations. It routes existing key access and parser
fixups to the same typed storage used by generated accessors. Copy and deep-copy retain the existing per-key
rules; explicitly shared node views share their field state. Root identity, context, source-location metadata,
and Clava-only nodes remain hand-written.

`NodeBindings` uses generated field descriptors, with no reflective key lookup. The declared Java type selects
reference, optional, list, enum, scalar or compound conversion. `Compounds` handles compound values. Source
information and aligned attributes have explicit conversions because their wire representation differs from
Clava's data. Scalars declared `= null` must be present unless marked `(wire_optional)`.

## Reviewing schema changes

`ClangAstParser/wire-fields.lock` is the committed review record. It covers all tables, field slots and types,
presence/default rules, attributes, enum ordinals and union members.

- Ordinary compilation runs `warnWireFields`, including when generated sources are already up to date. A
  mismatch warns on every build until explicitly accepted.
- `check` runs `verifyWireFields` and fails on a mismatch. The parser's `check` also runs the AST generator tests.
- `gradle -p ClangAstParser updateWireFields` is the explicit acceptance step. It rewrites the lock and is never
  invoked automatically. Review and commit its diff with the consumer changes.

To change the protocol:

1. Edit clang-dumper's schema and emitter, validate them, and prepare the selected release or local bundle.
2. Select that bundle in `clang-dumper-release.tag`.
3. Update `node-data.json` and any behavior or compound conversions needed for the changed fields.
4. Review the warning, run `updateWireFields`, and inspect the lock diff.
5. Run `gradle -p ClangAstParser check`. `NodeBindingsTest` checks every node payload's conversions; the parser
   suite checks code generation against its goldens. Build the weaver and run the JS suite for behavior changes.

## Releases and caching

An absolute selector path uses a local build directory with the same release manifest. The manifest's schema
hash must match the parser resource. Schema downloads are verified against the manifest's SHA-256.

AST ccache entries are namespaced by schema hash; ccache's compiler check covers the dumper executable. Every
present `CCACHE_DISABLE` value disables caching; `0`, `false` and `no` are rejected, so unset it to enable caching.
