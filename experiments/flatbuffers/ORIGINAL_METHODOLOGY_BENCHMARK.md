# Original-methodology FlatBuffers chart refresh

The [fresh three-implementation comparison](CURRENT_COMPARISON_BENCHMARK.md) supersedes the reused controls with 7 October measurements of current Text, Protobuf and FlatBuffers builds, for both App and separate command timing.

[Saved HTML report](ORIGINAL_METHODOLOGY_BENCHMARK.html).

The new report replaces only the FlatBuffers values in the earlier App and suite-command boxplots. Before-cache, Text and Protobuf observations are reused from 2 October. Updated FlatBuffers was measured on 6 October. These observations are not same-round pairs across dates.

The original renderer functions generate both charts, preserving linear quartiles, medians, full-range whiskers, four per-run dots, colors and one shared axis across each suite's cache states.

| Updated FlatBuffers median seconds | Cache state | Cumulative App | Suite command |
|---|---|---:|---:|
| Clava-JS | direct | 9.639 | 33.992 |
| Clava-JS | cold | 10.343 | 34.873 |
| Clava-JS | warm | 6.299 | 31.086 |
| Java parser | direct | 27.069 | 33.538 |
| Java parser | cold | 28.275 | 34.895 |
| Java parser | warm | 17.194 | 23.780 |

48 accepted measured commands, with four rounds of both suites in direct/cold/warm states for App timing and separate uninstrumented suite commands. Compiled classes and resources are prepared before timing. No production source changes are included. An isolated checkout patches the SHOW_EXEC_INFO default to false, matching the earlier suite-command policy.

App timing uses the original parser overlay, three-argument parse entry through final App transformations. It excludes earlier parser/context construction, assertions, code generation, syntax-only validation, setup and forced heap diagnostics. Natural GC counts. All 216 Java and 170 JS App calls are retained. The 130 JS syntax-only calls execute but are untimed. Per-mode workload and context partitions are checked.

Whole-command timing uses original npm/Gradle command boundaries without the App overlay. Java includes Gradle setup and worker startup; JS includes the npm/Vitest command. Both use the same selected test identities as the earlier report: 116 Java tests and 164 JS tests, with 158 passing and six pending. These are not unfiltered suites containing every test added since the pilot. JS preserves the recorded original test-file order; Java retains the same selected JUnit test identities.

Cold resets the stage cache; warm restores a snapshot seeded by a complete unmeasured suite. Direct sets CCACHE_DISABLE=true and probes zero ccache invocations. Enabled cache states unset the flag and validate counters. Production FlatBuffers cache isolation additionally includes schema and executable hashes.

Java uses 512 MiB. The JS JVM uses its original default, observed as 8,103,395,328 bytes, about 7.55 GiB. The earlier optimization report incorrectly described that JS limit as 512 MiB; its raw captures are correct and that description is corrected. No agent or GC override enters measurements.

The current FlatBuffers consumer and producer differ from the pilot; fidelity can change generated reparse contents. These are updated implemented-pipeline results, not a decoder-only intervention. Historical controls retain their exact previous values and versions. Cross-date rankings have host/software confounding and are not paired statistics. The separate same-day bypass benchmark remains available in [OPTIMIZATION_BENCHMARK.md](OPTIMIZATION_BENCHMARK.md).

[Evidence](validation/evidence/original-methodology-20261006.json) includes the 48 observations, cache and workload validation, runtime/overlay/source provenance, every plotted value and authenticated original renderer/control files. Raw artifacts are retained in the ignored suite results tree. Setup failures are excluded. No new memory measurement was made. The XStream warning remains.
