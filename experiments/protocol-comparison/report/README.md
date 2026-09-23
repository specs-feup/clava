# Protocol comparison report

Render the boss-facing, self-contained HTML report from the new per-mode results manifests:

```sh
python3 clava/experiments/protocol-comparison/report/render_report.py \
  --input clava/experiments/protocol-comparison/results/direct/results.json \
  --input clava/experiments/protocol-comparison/results/cold/results.json \
  --input clava/experiments/protocol-comparison/results/warm/results.json \
  --output clava/experiments/protocol-comparison/report/report.html
```

Each input must use the comparison runner's `results.json` schema. The renderer uses valid measured repeats, excludes warm-ups, and rejects legacy `bypass` manifests. It checks suite counts as well as the manifest's `valid` and process exit fields. Missing cache states remain visibly empty in the report.

The HTML has no external assets or libraries. DraftLink's theme switch works through the `html.dark` class. It includes six horizontal candle charts, one for each suite and cache state, plus the sibling-branch diagram, cache-counter verification, a concise result summary, and collapsed method and revision details. It strips local filesystem paths from the published report.

Cache verification reads counters attached to measured run rows, including nested ccache statistics such as `cache_hit_direct`, `cache_hit_preprocessed`, and `cache_miss`. If the harness does not record counters, the report says they are unavailable instead of inferring a cache hit from the selected mode.
