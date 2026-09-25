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

`--java-gc-profile` is optional and requires `--ab-results`. It adds the one-pair JVM diagnostic that traced Java's measured slowdown to repeated full garbage collections triggered by execution-info memory logging. Its bars distinguish the original JVM condition from a diagnostic run with explicit GC disabled. These exploratory runs are not substituted for the six-pair A/B medians.

Cache verification reads counters attached to measured run rows, including nested ccache statistics such as `cache_hit_direct`, `cache_hit_preprocessed`, and `cache_miss`. If the harness does not record counters, the report says they are unavailable instead of inferring a cache hit from the selected mode.
