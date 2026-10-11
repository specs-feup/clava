# AST wire format

clang-dumper writes each translation unit as a stream of size-prefixed FlatBuffers `Block`s (file identifier
`CLV2`). The schema lives in clang-dumper (`wire/v2/*.fbs`). Clava reads the stream in
`pt.up.fe.specs.clang.wire` without generated code:

- `WireSchema` loads the schema's binary reflection (`ast-wire.bfbs`) and the release's schema hash
  (`ast-wire.sha256`). The `wireSchema` Gradle task fetches both for the dumper selected in
  `clang-dumper-release.tag`. Streams whose header carries another hash are rejected.
- `NodeBindings` maps each node payload table onto Clava node data by name: table `FooData` fills class `Foo`, and
  field `bar_baz` fills the public `DataKey BAR_BAZ` named `barBaz`. A `base` field continues with the parent
  payload table. The key's declared type picks the conversion (node reference, `Optional`, `List`, enum by
  ordinal, scalar, or a compound value from `Compounds`). Annotate a node key with `@NullableNodeReference` when an
  absent reference must become a typed null node.
- `WireReader` reads blocks and records, builds nodes and resolves references.

Scalars are declared `= null`, so absence is visible; reading an absent field fails unless the field is marked
`(wire_optional)`.

## Changing the schema

1. In clang-dumper, edit `wire/v2/*.fbs` and the matching `src/Clava/Flat*.cpp` emitter, then publish a release
   candidate.
2. In Clava, add or rename the `DataKey`s the new fields need (and compound handling in `Compounds` for new
   compound tables).
3. Select the release in `clang-dumper-release.tag`.
4. Run `gradle -p ClangAstParser test`. `NodeBindingsTest` binds every payload table, so a missing or mistyped key
   fails there.

## Releases and caching

`clang-dumper-release.tag` selects the dumper release; an absolute path selects a local build directory with the
same manifest. Clava accepts a release whose manifest `wire_schema.sha256` equals `ast-wire.sha256`.

AST ccache entries are namespaced by schema hash; ccache's own compiler check covers the dumper executable. Every
present `CCACHE_DISABLE` value disables caching; `0`, `false` and `no` are rejected, so unset it to enable caching.
