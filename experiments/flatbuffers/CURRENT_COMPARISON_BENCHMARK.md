# Current Text, Protobuf and FlatBuffers benchmarks

7 October 2026. All three implementation series were freshly measured in one rotated serial session. Only Before-cache remains historical from 2 October and is excluded from paired statistics.

[Saved HTML report](CURRENT_COMPARISON_BENCHMARK.html). The original boxplot renderer functions preserve quartiles, medians, full-range whiskers, four dots per cell, colors and per-suite axes.

144 accepted measured commands: four rounds for three implementations, two suites and three cache states, separately for cumulative App creation and uninstrumented suite-command time.

| Suite | Cache | Build | App median seconds | Command median seconds |
|---|---|---|---:|---:|
| Clava-JS | direct | Text | 8.188 | 31.649 |
| Clava-JS | direct | Protobuf | 9.240 | 32.273 |
| Clava-JS | direct | FlatBuffers | 9.622 | 34.064 |
| Clava-JS | cold | Text | 8.705 | 31.988 |
| Clava-JS | cold | Protobuf | 9.732 | 32.977 |
| Clava-JS | cold | FlatBuffers | 10.364 | 35.072 |
| Clava-JS | warm | Text | 4.904 | 28.218 |
| Clava-JS | warm | Protobuf | 5.691 | 29.114 |
| Clava-JS | warm | FlatBuffers | 6.304 | 30.906 |
| Java parser | direct | Text | 25.810 | 32.182 |
| Java parser | direct | Protobuf | 25.115 | 32.043 |
| Java parser | direct | FlatBuffers | 27.098 | 33.774 |
| Java parser | cold | Text | 26.624 | 33.107 |
| Java parser | cold | Protobuf | 26.052 | 32.908 |
| Java parser | cold | FlatBuffers | 28.421 | 35.002 |
| Java parser | warm | Text | 16.027 | 22.281 |
| Java parser | warm | Protobuf | 14.828 | 21.645 |
| Java parser | warm | FlatBuffers | 17.266 | 23.836 |

## Matched rounds

Median of four within-round FlatBuffers relative differences. Positive means slower. Four descriptive observations do not establish statistical significance.

| Suite | Cache | Metric | Versus Text | Versus Protobuf |
|---|---|---|---:|---:|
| Clava-JS | direct | app | +17.71% | +4.01% |
| Clava-JS | cold | app | +18.88% | +6.44% |
| Clava-JS | warm | app | +29.02% | +10.74% |
| Java parser | direct | app | +5.17% | +7.83% |
| Java parser | cold | app | +6.84% | +8.79% |
| Java parser | warm | app | +7.96% | +16.44% |
| Clava-JS | direct | wall | +7.47% | +5.59% |
| Clava-JS | cold | wall | +9.64% | +6.63% |
| Clava-JS | warm | wall | +10.00% | +6.63% |
| Java parser | direct | wall | +5.00% | +5.59% |
| Java parser | cold | wall | +5.81% | +6.31% |
| Java parser | warm | wall | +6.88% | +10.18% |

## Matching App calls

Supplementary intersection selected by input content and ordered compiler options. Boxplots retain every call. Positive relative differences mean FlatBuffers is slower.

| Suite | Cache | Calls | Text seconds | Protobuf seconds | FlatBuffers seconds | FB vs Proto |
|---|---|---:|---:|---:|---:|---:|
| Clava-JS | direct | 170 | 8.188 | 9.240 | 9.622 | +4.01% |
| Clava-JS | cold | 170 | 8.705 | 9.732 | 10.364 | +6.44% |
| Clava-JS | warm | 170 | 4.904 | 5.691 | 6.304 | +10.74% |
| Java parser | direct | 207 | 24.628 | 23.890 | 25.793 | +7.91% |
| Java parser | cold | 207 | 25.411 | 24.771 | 27.053 | +8.90% |
| Java parser | warm | 207 | 15.842 | 14.589 | 16.938 | +16.09% |

## Controls and workload audit

Original timer boundaries and chart functions are retained. App timing spans three-argument parse entry through final App transformations. Native production, reading, AST construction and cross-TU linking count. Earlier parser/context construction, setup, assertions, code generation and syntax-only calls do not. Natural GC counts; forced heap diagnostics are disabled equally.

Wall time is measured with separate uninstrumented npm/Vitest and Gradle test commands. It includes process/worker startup and Gradle configuration. Runtime compilation and resource staging complete before measurement.

Original selected workloads have 116 Java tests and 164 JS tests, with 158 passing and six pending. All test identities match. They are not unfiltered newer production suites. Each Java App run captures 216 timed calls; JS captures 170 timed calls and 130 untimed syntax-only calls. JS retains each metric cohort's original file order. Per-build workloads and context-sharing partitions are checked across all cache modes and rounds.

Clava-JS versus Text: 170 matching calls in 12 observations; Clava-JS versus Protobuf: 170 matching calls in 12 observations; Java parser versus Text: 207 matching calls in 12 observations; Java parser versus Protobuf: 207 matching calls in 12 observations.

Source-content and compiler-option fingerprints define matching calls. Raw timings retain all calls, including differing generated reparses. The audit retains every difference and a supplementary common-call timing diagnostic. Text and Protobuf use their matching GlobalAttributes script and golden while preserving the common C++ input.

Each implementation, metric phase and suite owns a private cache. Cold clears it; warm restores the same complete-suite seed before each observation. Direct sets CCACHE_DISABLE=true and checks zero wrapper invocations; enabled modes unset the flag and validate real ccache counters. FlatBuffers cache entries additionally include schema and executable hashes.

Java uses one 512 MiB worker; JS uses its original default JVM heap observed at about 7.55 GiB. Coverage and GC-policy overrides are absent. Each isolated build sets SHOW_EXEC_INFO=false; production sources and the local user selector are preserved.

## Revisions

| Build | Clava | specs-java-libs | Producer |
|---|---|---|---|
| FlatBuffers | `09542632ad2c9e62db89b466fa81482612f03cd7` | `ae7194a7b3e981913d40a4403acfc759808b6c3a` | `v18.1.8_5-rc3` |
| Protobuf | `ff5e58afa96a07b18a3d2bd2e40a00a59bb28442` | `19c8e3e4c81dbc77a89d77cd7ab2bc8bfc3844fa` | `ab9d0238bd9c27eb9f156fd19967d16025137a37` |
| Text | `bae33249933f57bd78de5121dbb9611ace955aa1` | `f49ca2c4a7f7729c8a3738b64ef24d5938e890f8` | `c549b4b2f490af8a1be20b5f30e7aa875c9c95fe` |

All builds use lara-framework b780d0b8. This compares current branch implementations, rather than a format selector in one source revision. Producer, dependency and fidelity differences remain explicit. No new memory measurements were made. The accepted XStream incompatibility remains documented in the PR.

[Committed evidence](validation/evidence/current-comparison-20261007.json) contains all 144 observations, every plotted value, source/runtime/native/fixture/overlay provenance, workload differences, matching-call diagnostics, driver and report-generator text, artifact hashes and SVGs. Raw logs and captures remain in the ignored suite results tree. Setup failures contribute no measurements.
