# Protocol comparison report

Render the boss-facing, self-contained HTML report from the new per-mode results manifests:

```sh
python3 clava/experiments/protocol-comparison/report/render_report.py \
  --input clava/experiments/protocol-comparison/results/comparison-20260923-direct/results.json \
  --input clava/experiments/protocol-comparison/results/comparison-20260923-cold/results.json \
  --input clava/experiments/protocol-comparison/results/comparison-20260923-warm/results.json \
  --ab-results /path/to/passed-dual-ab/results.json \
  --java-gc-profile clava/experiments/protocol-comparison/results/java-gc-diagnostic-20260925/summary.json \
  --output clava/experiments/protocol-comparison/results/report.html
```

Each input must use the comparison runner's `results.json` schema. The renderer uses valid measured repeats, excludes warm-ups, and rejects legacy `bypass` manifests. It checks suite counts as well as the manifest's `valid` and process exit fields. Missing cache states remain visibly empty in the report.

The HTML has no external assets or libraries. DraftLink's theme switch works through the `html.dark` class. It includes a median trend chart and two suite-level candle figures, each grouping Direct, Cold, and Warm sections with visible dividers and one shared, tightly bounded time scale. It also includes the sibling-branch diagram, cache-counter verification, a concise result summary, and collapsed method and revision details. It strips local filesystem paths from the published report.

When `--ab-results` is supplied, the renderer requires a passing fidelity gate, complete valid paired repeats for both suites, and verified uncompressed ccache bypass. It adds two same-revision candle charts, a paired-change chart, and separate native/read/AST phase and raw-size bars. The time bars show summed parser work per run, not additive pieces of suite wall time. Without this optional input, the original report remains available.

`--java-gc-profile` is optional and requires `--ab-results`. It adds JVM diagnostics tracing Java's measured slowdown to repeated full garbage collections triggered by execution-info memory logging. The report compares the original JVM condition with explicit GC disabled, then shows five paired worker-JVM conditions for median full-GC pause: default, Protobuf compilation excluded, fixed heap bounds, both controls, and interpreter-only execution. These exploratory runs are not substituted for the six-pair A/B medians.

Cache verification reads counters attached to measured run rows, including nested ccache statistics such as `cache_hit_direct`, `cache_hit_preprocessed`, and `cache_miss`. If the harness does not record counters, the report says they are unavailable instead of inferring a cache hit from the selected mode.

## Post-GC rerun

The post-fix report keeps the original pre-fix measurements as historical context. Assemble the selected six-valid-sample matrix and its audit records before rendering:

```sh
python3 clava/experiments/protocol-comparison/report/assemble_post_gc.py \
  --results-root clava/experiments/protocol-comparison/results/post-gc-20260927T175510Z \
  --output-root clava/experiments/protocol-comparison/results/post-gc-20260927T175510Z/published \
  --protobuf-java-reruns \
  --pre-fix-jfr-text /path/to/ast-wire-ab-LkMaccmu/clava/experiments/protocol-comparison/results/java-gc-diagnostic-20260925/normal-text.jfr \
  --pre-fix-jfr-protobuf /path/to/ast-wire-ab-LkMaccmu/clava/experiments/protocol-comparison/results/java-gc-diagnostic-20260925/normal-protobuf.jfr \
  --post-fix-jfr-text clava/experiments/protocol-comparison/results/post-gc-20260927T175510Z/preflight/jfr/dual-ab-text.jfr \
  --post-fix-jfr-protobuf clava/experiments/protocol-comparison/results/post-gc-20260927T175510Z/preflight/jfr/dual-ab-protobuf.jfr
```

The assembler requires one warm-up and six measured attempts per selected suite/stage/mode cell, validates all selected tests, and records invalid and superseded attempts separately. With `--protobuf-java-reruns`, it selects complete passing Direct and Cold Java/Protobuf rerun cells while retaining the earlier failed cells and recovery attempts in the audit. It verifies the pinned before-cache dumper checksum in both suite downloads. Pre-fix JFR recordings live in a separate historical scratch worktree, not in this report's results bundle.

Render with the existing historical `--input`, `--ab-results`, and `--java-gc-profile` arguments, plus exactly three `--post-gc-input` manifests (Direct, Cold, Warm), `--post-gc-ab-results`, `--post-gc-ab-java-results`, `--post-gc-evidence-root`, and `--gc-fix-evidence`. The headline summary and two suite-level corrected candle figures appear first. Historical medians, prior same-revision diagnostics, recovery comparisons, and run-level records are retained in a collapsed appendix. The renderer combines only the passing Clava-JS rows from the first post-fix A/B and the later complete six-pair Java rerun, verifying their source revisions and parser runtime identity; it retains the original invalid Java attempt as audit evidence.

`--java-gc-ab-results` adds the interleaved 2-by-2 Java control: Text and Protobuf, each with explicit GC allowed and disabled in the Gradle test worker. The renderer requires six passing pairs per policy, the same 116-test population and 247 AST metric events per run, direct ccache bypass, a passed fidelity gate, and the same parser JAR, native executable, and SpecsUtils revision as the post-fix A/B. It plots all four wall-time distributions on one time scale and reports the paired format gap under each GC policy. Warm-ups and prior historical JFR diagnostics are not substituted for the six measured pairs.

The current GC-off full-suite control is incomplete: two attempts stopped on the same 13 Protobuf test failures. Pass each partial `results.json` through `--java-gc-ab-blocked-results` to show the correctness blocker and run records without making a timing claim. Do not use `--java-gc-ab-results` until all 28 invocations pass with the established single-worker workload.
