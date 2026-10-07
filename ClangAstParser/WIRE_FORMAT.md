# Protobuf AST wire contract

Clang-dumper owns the canonical Protobuf schema, descriptor set, native
generator and emitted-node contract. Clava selects a dumper release through
`clang-dumper-release.tag`; release builds resolve the schema and descriptor
from that release's manifest instead of reading a sibling checkout. Local
development builds use the same manifest and artifact checks from the selected
CMake build directory.

The manifest's schema and descriptor SHA-256 values identify the producer and
consumer schema. The downloadable schema artifact is named
`clang-dumper-ast-wire.proto`, while its canonical descriptor records the
producer source name `clava_ast_wire.proto`; the resolver checks that identity
and rejects Java options in the producer descriptor. Clava injects its Java
package options into a separate schema copy for protoc; those consumer-only
options do not change the canonical descriptor identity or the hash written
to and checked in wire headers. The native Protobuf runtime and native
`protoc` must match. The Java generator and
runtime may use a different repository version, but the manifest must declare
minimum supported versions and the selected Clava pins must satisfy them. The
Java runtime must be at least the generator version, with runtime major V or
V+1, following the [Protobuf cross-version runtime guarantee](https://protobuf.dev/support/cross-version-runtime-guarantee/).
An incompatible release fails before generation or compilation; bump the
consumer dependency explicitly before selecting it.

The resolver and binding generator use the Python Protobuf descriptor parser.
Install its pinned build dependency before running Gradle locally with
`python3 -m pip install -r tools/requirements.txt`; CI installs the same file.

Clava's adapter is generated from the verified canonical descriptor and the
compiled Clava DataKey inventory. `gradle generateProtoJavaBindings` refreshes
the adapter. `gradle verifyProtoBindings`, `gradle check`, and CI verify that
the checked generator output has not drifted. Generation checks payload
inheritance, exact DataKey spellings and generic element types, enum coverage,
optional references, and typed-null reference annotations. It emits direct
protobuf getters; there is no reflective field lookup.

Every emitted `Node` must name a supported Clava node and carry the checked
payload for that node's nearest wire-payload ancestor. The only generic payload
contracts are the closed `AttributeKind`-derived `*Attr` set and the concrete
OpenMP statement classes listed in the selected release manifest. An unknown
emitted class or an incompatible payload fails parsing with its class name,
node ID, and source location when available, even if the node is unreachable.
Intentional producer exclusions and generic families belong in the release
contract; a generic or dummy-node fallback does not make a missing mapping
compatible.

JaCoCo's 70% source-coverage gate measures hand-written code. Its report and
verification task exclude only class files derived from generated Protobuf
Java, the generated binding adapter, and generated hash/toolchain constants;
the build derives those exact class patterns from the generated source roots.
Hand-written framing, cache, parser, resolver, and binding-inventory classes
remain covered by the unchanged threshold. Generated binding drift and
compiled-inventory checks guard generated-code correctness separately.

The stream retains its `CLAVAPB1` framing and bounded Protobuf `Envelope`
chunks. Clava reads and validates one bounded frame at a time, and the producer
accounts for chunk size incrementally. Do not buffer a whole dump or retain all
generated messages. Release and local-build verification runs before generated
bindings or cached schema artifacts are reused.

The ccache wrapper treats trimmed, case-insensitive `1`, `true`, `yes`, and
`on` values of `CCACHE_DISABLE` as a disable request; this takes precedence over
conflicting cache-enable flags. Null, empty, `false`, `0`, `no`, `off`, and
unrecognized values follow the wrapper's enabled policy. Non-disabling values
are removed from the ccache subprocess environment because ccache versions do
not consistently accept the wrapper's broader false-value policy. Zstandard
compression remains external to ccache, and `CCACHE_NOCOMPRESS` continues to
control that compression layer.

## Publishing a compatible dumper release

The dumper release must publish the canonical `.proto`, descriptor set and
native executable artifacts named and hashed by `clang-dumper-release-manifest.json`.
The manifest must include matched native Protobuf and `protoc` versions,
minimum Java generator/runtime versions, protocol and semantic-contract
versions, schema and descriptor hashes, LLVM metadata, and the checked generic
payload contracts. A local CMake build emits the same manifest and protocol
artifacts beside its actual executable names. Validate the release with the
consumer resolver and generated-binding drift check before updating the Clava
release selector.
