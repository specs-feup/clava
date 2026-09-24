# Text/Protobuf AST fidelity gate

Before timing either wire mode, run
`pt.up.fe.specs.clang.dumper.AstWireFidelitySnapshotTest` once with each value
of `CLAVA_AST_AB_WIRE` (`text`, then `protobuf`). Set
`CLAVA_AST_AB_FIXTURE_C`, `CLAVA_AST_AB_FIXTURE_CXX`,
`CLAVA_AST_AB_SNAPSHOT_DIR`, and `CLANG_DUMPER_TOOL` for both invocations.
The test writes `text/c.json`, `text/cxx.json`, `protobuf/c.json`, and
`protobuf/cxx.json` beneath the snapshot directory.

Run `python3 experiments/protocol-comparison/analysis/ab_fidelity.py
--snapshots-dir <directory> --output <directory>/comparison.json`. It exits
successfully only when both C and C++ normalized Clava graphs compare exactly.
The result records node kinds and counts, ordered tree edges, node-valued data
references, populated fields, and any differing JSON paths. The runner must
stop before timing if this gate fails.

The serializer retains concrete Clava node classes, source ranges, all
populated semantic data keys, child order, and reference topology. It converts
Clava IDs to graph-local references and replaces the tested source path with
`$FIXTURE`. It excludes `ClavaNode.ID`, `PREVIOUS_ID`, `CONTEXT`, and `ORIGIN`,
JVM object identity, and timing metrics. The `DataStore` definition name is
also excluded: it labels the wire dispatch/store implementation, and can vary
for the split `AlignedAttr` representation. The concrete node class and its
populated fields remain in the comparison, including the aligned attribute
kind, expression/type data, location, and child edges.

This is a structural gate on the selected representative inputs. Passing it
does not replace running the full Clava-JS and Java smoke workloads in both
modes; those establish that each complete suite can consume its own output.
