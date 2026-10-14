# October 8, 2026 RC8 Protobuf-only measurements

This capture records the final Protobuf-only benchmark cohort against published
`v18.1.8_6-rc8`. The exact source revisions, snapshot-local instrumentation
overlays, runtime jars, release manifest and asset hashes are in
[`source-identity.json`](source-identity.json). The `updated-protobuf-results.json`
file contains 48 valid rows: four repetitions for each of App construction and
separate uninstrumented wall timing, across the Java and Clava-JS suites and
direct, cold and warm cache states. The workload contract is 116 pinned Java
test IDs, 164 JavaScript tests (158 passed, 6 skipped), 216 Java App calls, 170
JavaScript App calls and 130 JavaScript syntax-only calls. The only measured
stage was Protobuf; Text and FlatBuffers controls were not rerun.

The selected RC8 manifest, Linux x64 executable, schema and descriptor hashes
are recorded both in the results and in `release-manifest.json`. The verified
consumer cache supplied the staged resource data. Raw measured sidecars are in
`runtime-raw-sidecars.tar.gz`; the six memory repeat logs, commands and GNU time
records are in `memory-raw-repeats.tar.gz`. `memory.json` preserves the combined
120-cycle record, while `memory-nas-lu.json` and `memory-templates.json` are the
two renderer-ready workload payloads (three fresh JVMs per workload, 20 cycles
per JVM). `pretime-build-logs.tar.gz` records the build preparation. The large
copied resource trees and compiler caches are intentionally omitted; their
release and tree identities remain recorded in the JSON evidence.

The first final-cohort attempt is preserved under [`failed-attempt/`](failed-attempt/).
It stopped at the Java preflight because discovery included three additional
framework-search tests (119 tests instead of the frozen 116-ID contract), and
has zero timed observations. It is excluded from the rendered results. The
failed result manifest records attempted-driver SHA-256
`4f5a6f8367892451a3d4e9d1ec3ee7bd1fd53338416be746d7345a2333dcf84e`; that
earlier script body was unavailable when the archive was prepared, so the
failed attempt is represented by its manifest and stdout log. The
successful run uses a run-local Gradle init script to exclude only that added
test class, preserving the original 116 pinned IDs; the script and its hashes
are included here. No source checkout was edited to make this selection.

`runtime-driver.py` and `memory-driver.py` are the exact accepted drivers for
the successful captures. `SHA256SUMS` covers every file in this bundle except
itself. The frozen October 7 controls used for comparison are copied here and
retained at their original evidence path as well.
