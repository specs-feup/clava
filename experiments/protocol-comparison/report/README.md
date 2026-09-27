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

The HTML has no external assets or libraries. DraftLink's theme switch works through the `html.dark` class. It includes a median trend chart and six horizontal candle charts, one for each suite and cache state, plus the sibling-branch diagram, cache-counter verification, a concise result summary, and collapsed method and revision details. Suite scales are shared across cache-state charts; isolated extremes are marked at the edge with their full value disclosed. It strips local filesystem paths from the published report.

When `--ab-results` is supplied, the renderer requires a passing fidelity gate, complete valid paired repeats for both suites, and verified uncompressed ccache bypass. It adds two same-revision candle charts, a paired-change chart, and separate native/read/AST phase and raw-size bars. The time bars show summed parser work per run, not additive pieces of suite wall time. Without this optional input, the original report remains available.

`--java-gc-profile` is optional and requires `--ab-results`. It adds JVM diagnostics tracing Java's measured slowdown to repeated full garbage collections triggered by execution-info memory logging. The report compares the original JVM condition with explicit GC disabled, then shows five paired worker-JVM conditions for median full-GC pause: default, Protobuf compilation excluded, fixed heap bounds, both controls, and interpreter-only execution. These exploratory runs are not substituted for the six-pair A/B medians.

Cache verification reads counters attached to measured run rows, including nested ccache statistics such as `cache_hit_direct`, `cache_hit_preprocessed`, and `cache_miss`. If the harness does not record counters, the report says they are unavailable instead of inferring a cache hit from the selected mode.

## Post-GC rerun

The post-fix report keeps the original pre-fix measurements as historical context. Assemble the selected six-valid-sample matrix and its audit records before rendering:

```sh
python3 clava/experiments/protocol-comparison/report/assemble_post_gc.py \
  --results-root clava/experiments/protocol-comparison/results/post-gc-20260927T175510Z \
  --output-root clava/experiments/protocol-comparison/results/post-gc-20260927T175510Z/published \
  --pre-fix-jfr-text /path/to/ast-wire-ab-LkMaccmu/clava/experiments/protocol-comparison/results/java-gc-diagnostic-20260925/normal-text.jfr \
  --pre-fix-jfr-protobuf /path/to/ast-wire-ab-LkMaccmu/clava/experiments/protocol-comparison/results/java-gc-diagnostic-20260925/normal-protobuf.jfr \
  --post-fix-jfr-text clava/experiments/protocol-comparison/results/post-gc-20260927T175510Z/preflight/jfr/dual-ab-text.jfr \
  --post-fix-jfr-protobuf clava/experiments/protocol-comparison/results/post-gc-20260927T175510Z/preflight/jfr/dual-ab-protobuf.jfr
```

The assembler requires one warm-up and six measured attempts per selected suite/stage/mode cell, validates all selected tests, and records invalid and superseded attempts separately. One invalid measured Java/Protobuf run in each of Direct and Cold is excluded from timing and replaced by a valid same-cell recovery sample; both failures remain in the failure-rate audit. It verifies the pinned before-cache dumper checksum in both suite downloads. Pre-fix JFR recordings live in a separate historical scratch worktree, not in this report's results bundle.

Render with the existing historical `--input`, `--ab-results`, and `--java-gc-profile` arguments, plus exactly three `--post-gc-input` manifests (Direct, Cold, Warm), `--post-gc-ab-results`, and `--gc-fix-evidence`. The headline summary and six corrected candles appear first. Historical medians, prior same-revision diagnostics, recovery comparisons, and run-level records are retained in a collapsed appendix. The post-fix same-revision A/B is shown as a correctness audit; a failed full-suite gate means its timings are not decision-grade.
