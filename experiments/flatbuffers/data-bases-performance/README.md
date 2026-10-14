# Generated data bases performance check

This runner reuses the fixed App benchmark cohorts and instrumentation from T3 thread
`09397f39-9444-4e00-9775-2b351db24f46`. It compares committed revisions in isolated worktrees, using one shared
revision of specs-java-libs and lara-framework and one clang-dumper release. It never changes the canonical
release selectors, test sources or dependency checkouts.

The original thread's 2026-10-10 numbers were one quick run on `44079327e`, using `v18.1.8_7-rc3`. They are a
historical reference. The control for this check is the immediate parent of the generated-bases change,
`aa0946736`, and both measured revisions use `v18.1.8_7-rc4`.

## Reproduce

The original harness must be available in the sibling `ast-protobuf` workspace, or supplied with
`--harness-workspace`. Install the workspace's npm and Gradle dependencies first. `--resource-seed` points at a
verified `clang-dumper` cache directory containing `releases/<tag>` and `includes`. Builds and resource staging
are outside the timed region.

From the workspace root:

```sh
python clava/experiments/flatbuffers/data-bases-performance/run_benchmark.py \
  --baseline aa0946736 --current HEAD \
  --resource-seed /path/to/verified/cache/clang-dumper --repeats 4
```

The output directory contains full observations, App-call traces, workload and cache checks, commands, logs,
and commit/jar identities. By default the runner removes its worktrees and transient caches on completion.
`--keep` retains them; `--output` selects the evidence directory. A shared lock prevents overlapping runs of
this harness. Keep other builds and verification jobs out of the measured session.

The default checks direct and warm-cache modes. `--modes direct cold warm` adds cold cache; `--wall` adds a
separate uninstrumented whole-command matrix. Direct means ccache is bypassed. Warm mode restores the same
verified cache seed before each run. Revision, mode and suite order rotate between repeats.

```sh
python clava/experiments/flatbuffers/data-bases-performance/analyze_benchmark.py \
  /path/to/run/results.json --output /path/to/summary.json
```

The analyzer rejects incomplete, duplicated, failed or cache-invalid measurements, and rejects source-content
drift between revisions. It reports every sample, median, observed range and paired change. Ranges are not
confidence intervals, and a four-run comparison cannot prove exact equivalence.

## Workload and metrics

The pinned cohort contains 116 Java tests and 164 JS tests, with 158 JS tests passing and six skipped.
The harness checks test identities and file order, plus 216 Java and 170 JS App calls. Reparsed input filenames
and contents must match across revisions. The Java test worker uses a 512 MiB heap. Each command starts a fresh
worker, so this checks both startup and subsequent calls within a suite.

App time sums the time in `App.build`, including native parsing, wire decoding, node construction and AST
processing. The additional `instrumented_command_s` metric times the whole instrumented command; it includes
process and Gradle startup and test work outside `App.build`. It is not the separate uninstrumented wall metric.

The normal parser and JS validation suites are broader than this frozen benchmark cohort. New regression tests
are verified separately and do not change the timed test population.

## Previous recorded outcome

The final record compares parent `aa0946736bf241d9bac27a1c8825fffd4df0b2ba` with
`b8f33466bf43cc3425f5b22c5f49f50d8b466e96`, after sharing generated layouts, reading typed fields directly,
and removing duplicate backing-store slots. All 32 measured cells passed the workload and cache gates; no
parsed source contents differed.

| App time, median of four runs | Parent | After fixes | Change |
| --- | ---: | ---: | ---: |
| JS direct | 7.925 s | 7.972 s | +0.6% |
| JS warm cache | 4.635 s | 4.682 s | +1.0% |
| Java direct | 21.851 s | 22.042 s | +0.9% |
| Java warm cache | 12.044 s | 12.412 s | +3.1% |

Most of the initial slowdown was removed, but this is not a performance-neutral result: Java warm-cache
App time still increased. Small changes in the other cells may fall within run variation; four runs do not
establish equivalence or statistical significance. The 37.733 s Java direct sample is retained in the raw
record and chart, without attributing its cause or excluding it from the median.

`results.json` is the previous full record; The current investigation replaces `comparison.json` and `comparison.html` with the latest complete comparison. `shared-layouts-results.json` preserves the preceding four-repeat
check of `978430902`, before removing duplicate slots. `historical-reference.json` preserves the original
thread's quick-run figures and identities, separately from the matched control. The original one-repeat
scout was diagnostic and is not used in the final comparison.

Validation for the current investigation: 225 parser tests plus class coverage, three generator tests,
ClavaWeaver build, and 162 JS tests passed with two skipped. These run separately from the frozen cohort.

## Follow-up investigation

`investigation.md` explains the CPU and allocation costs found in Java-worker profiles and the resulting
changes. `profiling-summary.json` keeps sampled CPU/allocation evidence and the construction probe.
`direct-defaults-results.json` preserves the complete intermediate four-repeat comparison of `78ca2984c`.
`parsed-writes-failed-results.json` preserves the stopped comparison of `d9ee9b7bb`; its workload gate
failed and its observations supply no final estimates.

The current runner initializes parser defaults before parallel workers in both revisions. It applies
this shared correctness fix to older controls through the overlay, explicitly records the control, and
keeps the complete workload fingerprint, including `SHOW_CODE`. See the investigation for the reproduced
configuration race. The canonical parent source remains unchanged.

Use `--suites java --modes warm` for a focused check. Add `--profile` to record Java-worker JFR files.
Profiling timings are diagnostic and should not be compared with unprofiled final measurements.

## Current recorded outcome

The complete comparison of `aa0946736` with `9182af4ca` passed all 32 measured cells.
All normalized workload fingerprints matched across revisions, modes and rounds, and no parsed source
contents differed. Both revisions use the same parser-default initialization correction.

| App time, median of four runs | Parent with parser fix | Typed fields with parser fix | Change |
| --- | ---: | ---: | ---: |
| JS direct | 7.803 s | 7.967 s | +2.10% |
| JS warm | 4.483 s | 4.581 s | +2.19% |
| Java direct | 21.649 s | 21.626 s | -0.10% |
| Java warm | 12.008 s | 12.005 s | -0.03% |

Java is essentially unchanged in this workload. JS remains slower in every paired observation,
with median increases of 2.10% direct and 2.19% warm. The no-degradation performance gate is still unmet.
Four runs are descriptive and do not establish precise statistical confidence or an end-to-end gain.

Steady construction allocation fell from 864 to 688 bytes for `BuiltinType`, and from 1160 to 784 bytes
for `VarDecl`, reductions of 20.4% and 32.4%. These are empty-node construction measurements, not total
App allocation or process RSS. The typed model has a measured storage benefit. That does not imply
a matching throughput gain for a workload that retains generic access and includes native execution,
wire decoding and AST transformations.

`stable-defaults-results.json` preserves the final raw record. `comparison.json` links it by SHA-256
and retains every sample; `comparison.html` shows all samples alongside the construction allocation
measurements. Earlier candidate results are retained separately and supply no final estimates.
