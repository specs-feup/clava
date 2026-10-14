# October 14, 2026 Protobuf-only benchmark refresh

This bundle records a complete Protobuf-only refresh against the current implementation. It contains 48 valid timed observations: four repeats for App time and separate uninstrumented command wall time, across Java and Clava-JS in direct, cold, and warm cache modes. Text and FlatBuffers were not rerun; their earlier rows remain frozen controls.

The Java workload retained 116 pinned test identities. Clava-JS retained 164 selected identities, with 158 passing and 6 skipped. The workload, cache, App-call, context, and source-count gates are recorded in the complete results and snapshot identity files.

| Suite | Direct | Cold | Warm |
|---|---:|---:|---:|
| Java App time median, seconds | 25.07 | 26.09 | 14.85 |
| Clava-JS App time median, seconds | 8.86 | 9.39 | 5.36 |

| Suite | Direct | Cold | Warm |
|---|---:|---:|---:|
| Java command wall time median, seconds | 32.29 | 33.31 | 21.74 |
| Clava-JS command wall time median, seconds | 33.09 | 33.82 | 29.77 |


The current report SHA-256 is `3526c438598b1791ae65642428e634797eb41fe45e1202ef04c8476b13f0779a`. The first render, created before the source-label correction, is included as `report-superseded-before-label-fix.html` and remains in the raw run archive. It is superseded because its source column mislabeled historical Text and FlatBuffers rows. The rerender changed only source labels and report metadata. Runtime, memory, and control observations did not change. The current report labels only Protobuf as a new session; historical Text and FlatBuffers rows are labeled as reused controls.

The benchmark used Clava `8bdb6532078d2c246a23522f925a1ec80d2c81ca`, specs-java-libs `76fe6b1586bdc6bdb7f6501f30ce238891757b1b`, and Lara `73c5b955ab5484a3fd3199f9e2da7580758c5683`. The source workspace recorded clang-dumper checkout `ec4d80e9c73766df32355cd5380f9d4414443102`. Runtime used published `v18.1.8_6-rc8` from native release repository revision `e874debc111ec105fa5d1efa17ecd97300613178`; the Linux x64 executable SHA-256 is `ce467a8355c389c45024490e1ee67930737db090d8e5b236db82ecf1f69c9d27`. The current Clava implementation and generated packages were built in the isolated snapshot.

The nine selected JavaScript test files were staged from pinned fixtures whose hashes match the frozen cohort. The initial preflight rejection caused by the current CXX test source drift remains in the earlier `benchmark-refresh-20261008-js-cohort-drift` evidence. This capture kept the frozen baseline and records original, current, and staged hashes in the overlay provenance. Only the isolated benchmark test tree received the fixture overlay.

The separate memory diagnostic used six fresh JVMs, three each for NAS+ and C++ templates, with 20 cycles per JVM. It recorded 120 cleanup phases. Memory measurements are separate from timed runtime rows and do not establish runtime recovery.

`runtime-raw-sidecars.tar.gz` contains measured and preflight command records, per-run logs, test reports, and App call sidecars. `memory-raw-repeats.tar.gz` contains all six probe logs, command records, and GNU time output. Large parser resource trees, compiler caches, and temporary build trees are omitted; their release and source identities remain in the capture records. The full unpruned run remains in the local benchmark archive.

The exact driver sources named by the capture hashes are preserved as `runtime-driver.py` and `memory-driver.py`. `runtime/overlays/protobuf/overlay.jar`, its provenance file, and the transformed parser source in `runtime-raw-sidecars.tar.gz` preserve the App timing overlay used by this capture. Build and packaging logs are kept byte-for-byte in `pretime-build-logs.tar.gz`.

The report passed the workflow's local-path, private-URL, and secret-value checks. T3 HTML preview passed in light and dark themes with no console messages. Workflow verification passed 34 refresh tests and 18 report tests.


See `provenance-clarification.md` for the source-versus-release revision distinction and the effective ccache compression policy. `provenance-clarification.json` records the correction without changing the original capture rows. The current report SHA-256 is `3526c438598b1791ae65642428e634797eb41fe45e1202ef04c8476b13f0779a`; the prior report is preserved as `report-superseded-before-provenance-fix.html`.
