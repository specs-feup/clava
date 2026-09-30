# Decision experiment, 30 September 2026

## Questions

1. Which implemented configuration is fastest for each suite: the before-cache baseline, Text with ccache, Protobuf, or eager FlatBuffers?
2. Does ccache save enough time on repeated parses to justify its cold-miss overhead?
3. Why did the older branch comparison show a Protobuf gain for Clava-JS and a loss for Java? Does that opposite-direction result survive controlled measurement?

The final recommendation must consider runtime first, then the code a two-person maintenance team must understand and support. A smaller wire file is not, by itself, a faster parser.

## Primary comparison

Measure the complete original suites with their parser groups, parallel settings, generated-code round trips, App construction, and cross-translation-unit linking intact. Keep 158 passing Clava-JS tests plus six established skips, and 116 passing Java parser tests. No test failure contributes a timing.

Prepare owned source/build overlays. Preserve the four branch implementations and record their revisions, exact patches, dependency revisions, native executable checksums, parser/runtime JAR checksums, commands, and environment. Apply the same harness-only input-copy removal and execution-info policy to every stage. Use the corrected shared utility dependency, including the redundant-GC and concurrent-option fixes. Do not modify user branch worktrees to run the experiment.

Execution-info heap sampling is off for every parser invocation. This means no harness-requested explicit GC; normal JVM automatic GC remains enabled. Do not use `DisableExplicitGC` as a substitute for removing heap sampling from the measurement path.

Build once before measuring. Report separately the complete command wall time and, where available, the Java test-execution duration. Compilation is outside the measured boundary. Use one untimed warm-up and six measured runs per cell. Rotate stage order between rounds and serialize all measured commands on the host.

| Cache state | Eligible stages | Validation |
| --- | --- | --- |
| Direct | All four | No ccache invocation or hit/miss activity |
| Cold | Text, Protobuf, eager FlatBuffers | Fresh owned cache, recorded misses; any within-command hits disclosed |
| Warm | Text, Protobuf, eager FlatBuffers | Untimed seed, counters reset, measured hits and remaining misses disclosed |

The before-cache branch has no cold or warm state. It provides historical implementation context. Attribute the benefit or cost of ccache using Direct versus Cold/Warm within the same implementation, not the difference between two branch heads.

## Format-only control and explanation

Replay captured **parent parser calls**, not one call per source file. Preserve each call's resolved file list, compiler options, parallel policy, working directory, and generated-root policy. Retain files at their original generated locations rather than copying them into a replay tree. Allocate unique generated locations before capture when a producer would otherwise overwrite a path. Reject missing files, changed contents, changed group membership, and mismatched native invocation counts.

Use the same-revision dual-format Text/Protobuf build. Time the outer `CodeParser.parse` call, including App processing and cross-file linking, with execution-info reporting and diagnostic metrics off. Validate equivalent output before accepting the result. Keep full-suite timing and grouped replay timing distinct.

Run phase diagnostics separately. Native execution, wire reading, AST construction, and parent-call residuals must have explicit boundaries. Parallel worker occupancy is not additive suite wall time. Use serial diagnostic controls only as labelled explanations, never as replacements for the original parallel workload.

The older opposite-direction result needs an explanation supported by an intervention or measured contribution. Differences in revisions are a warning about causal attribution, not a sufficient root-cause explanation. If the fresh result still differs by suite, identify the files/groups and phases responsible, then validate the proposed cause. If the sign reversal disappears, distinguish a disproved format claim from any remaining unassigned branch effect.

## Analysis and report gates

- Show all six valid repeat points and median/interquartile candles, not only one fastest run.
- Use paired deltas only for genuinely matched rounds or calls. Do not imply independent branch medians are a controlled format A/B.
- Separate total runtime, per-call latency spread, wire size, cache counters, and memory measurements.
- Publish CSVs and a complete run audit. Preserve failed attempts as excluded evidence and rerun repaired cells with the same workload.
- Use one tightly bounded time scale across Direct, Cold, and Warm within each suite chart. Mark the non-zero axis origin and show distinct second labels.
- Put the recommendation, suite charts, discrepancy explanation, and maintenance tradeoffs in the main five-minute report. Put detailed provenance, per-input rankings, and diagnostics in expandable sections.
- Verify readable desktop and phone layouts, light and dark themes, and absence of horizontal overflow in the shared browser. Keep the final DraftLink private.
