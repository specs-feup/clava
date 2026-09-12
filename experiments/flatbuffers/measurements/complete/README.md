# Complete schema measurements

The [visual report](https://draftlink.lmsousa.workers.dev/d/dnyeY92urT89) uses this dataset.

Keep text as the default for now. The complete schema works, but this implementation does not improve full-suite time: eager FlatBuffers is 1.7% slower cold and 0.8% slower warm; lazy is 0.4% slower cold and 1.2% slower warm. Laziness shortens initial import, then pays much of that cost during TU construction. It also retains more heap in both single-TU probes. The schema is a maintenance improvement worth keeping as an experimental path; these measurements do not justify adopting blanket lazy fields for performance.

## Full Clava-JS suite

All 27 runs contain 164 tests: 158 passed, four known environment failures,
and two pending. Three repetitions per format/cache state, sequential execution,
rotating format order. All use the same native executable and 89-jar manifest.

| Format | Bypass median, s | Cold median, s | Warm median, s |
|---|---:|---:|---:|
| text | 54.81 | 54.69 | 43.70 |
| flat-eager | 55.28 | 55.63 | 44.07 |
| flat-lazy | 61.37 | 54.92 | 44.23 |

Cold calls had 16 direct hits / 167 cacheable calls; warm had 162 / 167 for all
formats. Five warm misses involve generated input paths under a unique temporary
directory. Header-only parses and final syntax validation are outside this cache
denominator. Bypass uses files, not the historical streaming transport.

The four failures are OmpThreadsExplore, Cuda, CudaMatrixMul, and CudaQuery.
They remain in the workload. Compiler/header environment failures are not new
FlatBuffers failures. The long wall-time outliers concentrate in the failing Cuda
test, which took approximately 15–17 seconds instead of 8 in text cold/bypass and
two lazy bypass cells. The 12% lazy bypass median difference is not established
as a format-caused regression. No final observations were removed.

## Memory and fidelity

Eleven fixtures passed graph, reachable-field, generated-code and mutation/copy
comparisons. This does not establish original/weave/reparse dump identity.
Synthetic mapping checks cover a sparse file above 2 GiB and a record above
64 MiB, not an AST of that size. Required scalar/reference presence and optional
scalar absence have focused checks.

Two single-TU memory fixtures, three JVMs per mode, showed higher retained heap
after postprocessing with lazy fields. All 18 manually detached TUs were collected.
Normal code generation does not yet detach each TU. These probes do not establish
GCC-scale or multi-TU memory bounds.

## Provenance

Measured production revisions:

- Clava `122a79244e335cdc2f3e36d55e4825ceb39609d4`
- specs-java-libs `26022a8f`
- clang-dumper `e69a77f2838581bcfdbe7d025f389d3ed9e85b0b`

Native executable SHA-256:
`1e8b3e7efc6c0392241ef73da4b66d73928a42541a71b8c47cb184cfec3a3b16`.
Runtime manifest SHA-256:
`1376413a13e370cb882dfb4e674f832aef323e4f8a46cbf473067659cc87f79c`.
The later measurement commit only adds runner, documentation and evidence changes.

`suite.json` indexes final results. `suite-matrix-20260912T143002441107Z/cell-NN/`
contains raw summaries, parse metrics, cache stats, per-test timings and GNU time
output. Vitest logs and JSON are gzip-compressed without changing their contents.
The separate ccache path diagnostic is also preserved there.

`pre-fix-race/` and `temp-quota/` preserve invalid earlier trials and are excluded
from every final timing comparison. The shared metadata race was fixed before
these final trials. Java, Node and Python temporary files use run-owned workspace
directories to avoid the system `/tmp` quota. Filesystem metadata is recorded.

`fidelity.json`, `memory.json`, `storage.json`, `mapping.log` and the fixture logs
record focused evidence. Compressed storage controls are zstd -5 recompressions
of the same text bytes. `concurrency/` includes the old failing focused test and
fixed focused result. The full 1,745-test jOptions run was reported by the test
agent; its XML was overwritten by a later focused run, as recorded in result.json.

Per-TU phase sums are aggregate occupancy, not wall-time slices. Peak RSS is a
process high-water mark, not aggregate concurrent memory. Three repetitions
show observed spread, not confidence intervals. The full Clang corpus and release
manifest schema distribution remain unimplemented validation/release work.
