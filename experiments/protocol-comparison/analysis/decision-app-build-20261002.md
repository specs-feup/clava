# Clava App-building performance — 2 October 2026

Performance-first choice for the combined measured workloads: **ccache + eager FlatBuffers**. FlatBuffers beats Protobuf in both suites and every cache state, in all 24 paired comparisons. Text remains fastest on Clava-JS and has the lowest migration burden. This is a recommendation for these implemented pipelines and workload mix, not a universal wire-format ranking.

## What was timed

Entry to the three-argument `ParallelCodeParser.parse` method through completion of the App's final tree transformations. The App is Clava's parsed, linked C/C++ program model. Source discovery, native parsing/cache consumption, reader work, cross-file linking and transformations are inside the interval. Parser/context construction before entry, Gradle/JVM startup, syntax-only validation, assertions, code generation, heap logging and code/AST display are outside it. Natural GC counts; no forced GC or coverage agent is used.

The original suites execute their original grouped calls, shared contexts and generated-code reparses. No isolated-file replay or extra relocation is used. A round total sums every completed-App interval; it is not suite-command wall time. Calls are sequential in the suite runner; native parallelism within a parse remains enabled.

- Four rotated rounds, 80 serial measured invocations, all valid.
- Java: 116 tests pass; 216 completed Apps per invocation; 512 MiB worker.
- Clava-JS: 158 tests pass and six skip; 170 completed Apps, including nine empty-source Apps; 130 syntax-only calls execute but have no App timer.
- Totals: 15,440 timed Apps; 5,200 excluded syntax-only calls. Preflight, cache seeds and include-audit timings are not pooled.
- All test identities, source/configuration fingerprints and context-sharing partitions match the preflight.

## App time by suite

Seconds below are medians of four summed round totals. Percentages elsewhere are medians of paired same-round percentages, not ratios of these medians.

| Suite | Cache state | Before cache | Text | Protobuf | Eager FlatBuffers |
| --- | --- | ---: | ---: | ---: | ---: |
| Clava-JS | Direct | 8.554 | 8.927 | 9.774 | 9.099 |
| Clava-JS | Cold | — | 9.247 | 10.170 | 9.621 |
| Clava-JS | Warm | — | 5.240 | 6.046 | 5.658 |
| Java | Direct | 25.490 | 28.643 | 28.299 | 26.748 |
| Java | Cold | — | 29.227 | 28.983 | 28.037 |
| Java | Warm | — | 17.892 | 17.218 | 16.178 |

“Before cache” has no cold/warm modes. Direct disables caching for the three cache implementations and verifies zero ccache invocations. Cold starts empty; warm restores an unmeasured seed snapshot before each suite invocation, not between individual App calls.

## Paired findings

| Comparison | Clava-JS | Java | Direction repeats |
| --- | ---: | ---: | --- |
| Text warm vs Text direct | −41.1% | −37.4% | 4/4 faster in both |
| Protobuf warm vs Text warm | +15.4% | −2.8% | 4/4 in each direction |
| FlatBuffers warm vs Text warm | +7.8% | −9.5% | 4/4 in each direction |
| FlatBuffers direct vs Protobuf direct | −6.4% | −5.5% | 4/4 faster in both |
| FlatBuffers cold vs Protobuf cold | −5.5% | −4.1% | 4/4 faster in both |
| FlatBuffers warm vs Protobuf warm | −6.4% | −6.5% | 4/4 faster in both |

Negative percentages mean less time. Java's Protobuf improvement is inside the App interval, whereas its separately measured whole test command also includes outside work. The App timings exclude the separately controlled approximately 0.40-second Gradle plugin/generation cost. The Clava-JS Protobuf App regression and Java App improvement both persist without forced GC/heap logging: those operations cannot explain these measured directions. These observations establish which implemented pipeline is faster; they do not isolate a particular decoder function as the cause.

## Combined specified workload

Add the 216 Java and 170 Clava-JS App calls in each matching round. This gives a 386-call workload, not a production-weighted extrapolation.

| Cache state | Before cache | Text | Protobuf | FlatBuffers | FlatBuffers vs Text, paired |
| --- | ---: | ---: | ---: | ---: | ---: |
| Direct | 34.102s | 37.533s | 38.031s | 35.847s | −4.6%, 4/4 faster |
| Cold | — | 38.474s | 39.153s | 37.576s | −2.7%, 4/4 faster |
| Warm | — | 23.107s | 23.308s | 21.839s | −5.6%, 4/4 faster |

This supports FlatBuffers for a performance-first combined-workload choice. For a Clava-JS-dominated workload, Text is faster and avoids migration. Protobuf's easier generated message API does not overturn its measured loss to FlatBuffers in either suite.

## Scientific limits and controls

The four stages use frozen matched-fast artifacts. Baseline Clava is `70c30bdb8cce66dba53c0e2963c3ec31cbc223bc`, with common test-harness corrections and fast syntax-only validation backported; it is not an untouched historical binary. Its live text reader differs from the completed-output reader in the cache implementations, even in bypass. The baseline/Text direct gap is therefore not solely cache-wrapper overhead.

Cached Text/Protobuf retain native Zstd with ccache compression disabled. FlatBuffers retains ccache compression of raw buffers. Direct is uncompressed. Native builds are Release `-O3`, LLVM 18.1.8. The comparison measures those implemented policies rather than an artificial same-compression wire-only experiment.

Cold Java has 208 misses; warm Java has 207 hits and one miss. Cold Clava-JS has 150 misses and 16 within-suite hits; warm Clava-JS has 166 hits and no misses. All cache gates passed. Artifact identities were checked before and after measurement. The output overlay is compiled for Java 17, matching the frozen runtime classes, and does not rewrite the runtime JARs.

Four rounds on one host provide descriptive spread, not confidence intervals. Individual App calls are not independent replicate runs. The report uses one tight axis across all modes within each suite; suite axes differ. This is performance evidence, not release-readiness certification.

## Data and reproducibility

- [Per-App timings, round totals and paired changes](decision-app-build-20261002.csv): 15,440 App rows, 80 round-total rows, 12 paired-median rows.
- [Publishable run audit](decision-app-build-20261002-audit.json): counts, stage/artifact identity, full hashes and recovery evidence.
- [Decision text used by the report](decision-app-build-20261002.json).
- [Harness contract and commands](../app-build/README.md).
- [Overlay builder](../app-build/build_overlay.py), [matrix driver](../app-build/run_app_build_matrix.py), [analysis](../app-build/analyze_app_build.py), and [candle renderer](../report/render_app_build.py).

Accepted run manifest SHA-256: `6e5ecc5d97393b679b6efe6c11075b107b84e4a43bddb8f1c0db2188faa6b1bc`.

Accepted raw report-payload SHA-256: `9f01de87f3d00a9db494e2f6bd7581290fdfcbe1b4c22651a448feb75b8963f7`.

Verified preflight SHA-256: `adef5bc050a4a6f38a6d2a56b0386ade61bbb1ee77abd4ff5ccbe410f3bd82f9`.

The first round stopped only at an order-sensitive metadata gate: sequential C/C++ test files reversed order while preserving their workload and context partitions. All 20 successful cells were revalidated without rerunning. Their 60 raw/enriched/summary hashes are unchanged. The metadata gate now compares context-sharing partitions rather than opaque IDs or independent file order. No timing was selected or discarded based on its value. All 80 saved test reports independently match the same identity digests, and raw JSONL, call CSV and report payload agree exactly on keys and elapsed values.
