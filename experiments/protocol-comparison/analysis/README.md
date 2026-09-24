# Protocol timing breakdown

From the `ast-protobuf` workspace root, run the analyzer with Python 3:

```sh
python3 clava/experiments/protocol-comparison/analysis/analyze_protocol_runs.py
```

By default it reads the stored direct, cold, and warm manifests from the
2026-09-23 comparison and writes
`clava/experiments/protocol-comparison/results/protocol-breakdown-20260923.json`.
Pass three `--input` options and `--output` to choose different paths.
After a separate instrumentation run finishes, add its output directory with
`--phase-followup`, for example:

```sh
python3 clava/experiments/protocol-comparison/analysis/analyze_protocol_runs.py \
  --phase-followup clava/experiments/protocol-comparison/results/phase-followup-20260924-protobuf-direct
```

For every measured Java run, the JSON records the sums and counts of the
`Code to AST` and `AST Processing` log entries. It requires 216 entries of each
type in every log. For each Clava-JS run, it reads the C and CXX integration
test file durations from the companion `vitest.json` report. The whole-run
elapsed time comes from the manifest.

Each run's residual is the whole-run time minus those two timing values. The
script reports the median of the per-run residuals, so the median residual need
not equal median whole-run time minus the two median timings. Residual time is
an arithmetic remainder, not a separately measured phase. The analyzer checks
that every measured suite, mode, and stage cell has repeats 1 through 6, and
exits with an error if a required log, file duration, or count is missing.

When present, `PROTOBUF_METRIC` and `CLAVA_AST_METRIC` JSON log lines are
aggregated by numeric `*_ms` phase field. The JSON keeps these independent
phase sums separate from elapsed time because work across parser jobs can
overlap. `--phase-followup` reads those metrics from a completed follow-up
manifest and records each run plus the median per-run phase sums.

The Java `Code to AST` and `AST Processing` totals are enclosing ClavaMetrics
spans. Per-TU protocol metrics run inside AST parsing and overlap those spans.
Do not add the per-TU phase sums to the Java totals when describing elapsed
time. Follow-up summaries also retain the Clava and native revision SHAs, the
runtime manifest hash, each run's staged parser jar SHA, and run/test validity
counts.
