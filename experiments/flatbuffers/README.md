# Complete FlatBuffers and lazy Clava properties

This experiment integrates the full v2 protocol into Clava's normal parsing pipeline.
The companion native branch is `ast-flatbuffers` in `clang-dumper-ast-flatbuffers`.
The [report](https://draftlink.lmsousa.workers.dev/d/dnyeY92urT89) contains the measured
comparison. The earlier ten-payload hybrid experiment is documented in [PILOT.md](PILOT.md).
Do not mix its timings with the complete-schema measurements.

## Architecture

The native schema covers all 112 current dumper payload handlers, including generic
family handlers. This describes current dumper support, not every Clang AST class. Its 173 tables describe both node fields and compound values. Both
sides use generated FlatBuffers accessors; official schema reflection also generates
Clava DataKey bindings and presence checks. Native Clang getters and compound-value
adapters remain handwritten. There is no raw-text fallback.

The native writer emits size-prefixed blocks at a 64 KiB target. Java maps windows,
builds the graph and resolves every node reference eagerly. This includes references
inside template arguments, constructor targets and exception specifications. Construction
maps are released after multi-TU normalization and structural postprocessing. The wire
IDs are dense integers; the current importer converts them to scoped strings for the
existing Java queues. It does not yet use an array-indexed construction map.

The lazy mode places unloaded markers in existing DataStore slots. One decoder per node
retains its mapped record and file table. Reading a field memoizes the value; writing
replaces it. Copies preserve Clava's normal copy policies. This avoids a Lazy object and
supplier per field. New nodes use ordinary in-memory storage. Detached nodes can be
collected if no other AST or context references retain them.

Immutable mapped files remain until JVM exit. Binary files are uncompressed locally;
ccache compresses their persistent entries. Text keeps its native zstd compression.
The file/cache path waits for the native process before import, so incremental mapping
does not restore native/Java overlap.

## Build and select

Use the pinned SDK and build the native companion as described in its `wire/README.md`.
From this Clava repository:

```sh
python3 experiments/flatbuffers/bootstrap.py
gradle -p ClavaWeaver --no-daemon installDist
```

Set `FLAT_NATIVE` and `FLATBUFFERS_ROOT` to override the sibling native worktree and SDK
paths. Gradle generates Java bindings from the companion schema during the build and
compiles the Java FlatBuffers runtime from the same pinned SDK.

Normal Clava defaults to text. Select the integrated experiment with a JVM property:

```text
-Dclava.astWire=text
-Dclava.astWire=flat-eager
-Dclava.astWire=flat-lazy
```

`-Dclava.astWireMetrics=true` emits per-TU native/read/construction timings and sizes.
The dumper format argument is part of ccache invocation identity. The native executable
must be the matching experimental build; the suite runner stages it into an isolated
release cache. Schema hash mismatch fails explicitly. Downloading the schema through a
release manifest remains release engineering work, not part of this prototype.

## Validate and measure

```sh
python3 experiments/flatbuffers/complete-check/run.py
java -Xmx512m -cp 'experiments/flatbuffers/complete-check/build:Clava-JS/java-binaries/lib/*' \
  pt.up.fe.specs.clang.wire.MappingChecks
python3 experiments/flatbuffers/complete-check/memory.py --repeats 3
python3 experiments/flatbuffers/suite/run_matrix.py \
  --dumper ../../clang-dumper-ast-flatbuffers/build/tool --repeat-count 3
```

The eleven fixtures compare normalized graph references, all reachable field values,
generated code and mutation/copy behavior across text, eager and lazy readers. Mapping
checks include a sparse file above 2 GiB, a record above 64 MiB, malformed framing and
required scalar/table/union presence. Memory probes run in separate JVMs with explicit
GC, outside timing trials. These probes are single-TU tests and are not a GCC-scale
memory guarantee or an original/weave/reparse dump-identity proof.

The matrix runs all Clava-JS tests, with three repeats of each format and cache state.
Cold/warm pairs share a fresh isolated ccache. Cold means an empty AST cache, not dropped
OS pages; repeated inputs within that cold suite can already hit. Bypass disables ccache
but still uses the file path. Runtime resources are prepared before timing. Do not run
builds or other experiments concurrently with the matrix.

Per-TU timers sum to aggregate occupancy across potentially parallel work. They cannot
be stacked into suite wall time. GNU time peak RSS is a process high-water mark, not the
sum of simultaneous processes. Three repetitions show median and range, not statistical
confidence. The four known baseline suite failures remain included and visible.

Raw generated files and local caches are ignored under `results/`. Durable summaries,
field-check logs and environment manifests belong under `measurements/complete/`.
