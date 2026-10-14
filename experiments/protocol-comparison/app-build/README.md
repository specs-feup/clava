# Clava App construction matrix

This harness measures App construction inside the original Clava-JS and Java parser suites. It does not replay individual files or substitute a new workload. The timer covers a `ParallelCodeParser.parse` call from entry to the point after the final App tree transformations, before `SHOW_EXEC_INFO`, heap logging, or code/AST display. Parser-instance construction and a new `ClavaContext` created before method entry are outside the interval. Parsing, TextParser/text passes, cross-file linking, and transformations are inside it. Natural GC during the call remains part of the time; the harness does not force GC. The report sums per-call intervals by suite and round, not process or suite wall time.

The original suite calls, source groups, and context sharing stay intact. Generated reparses made by those suites remain in the call population, though work that generates a file before calling the parser is outside that call's timer. Empty-source-group Apps are retained. The Clava-JS suite runs its established 164-test selection (158 pass, 6 skipped); Java runs the established 116 parser tests. Gradle/Vitest and JVM startup, assertions, code generation, and display are not timed. Java coverage is disabled and the worker is checked for coverage agents; the Clava-JS command does not enable coverage. The runs use no forced-GC option.

## Stages and cache modes

The four frozen stages are `before-cache`, `ccache-text`, `protobuf`, and `flatbuffers`. The first is the direct-only baseline. The other three each run in `direct`, `cold`, and `warm` modes. For those cached stages, direct mode disables ccache and uses a wrapper probe to reject any ccache invocation. Cold mode starts with an empty stage-owned cache and requires misses. Warm mode seeds each stage/suite cache once with an unmeasured full-suite run, snapshots it, then restores that snapshot before each measured suite invocation and requires hits. The cache is not reset between individual App calls within that invocation.

The measurement has exactly four rotated rounds. Each round contains all 20 allowed suite/stage/mode cells: both suites in direct mode for all four stages, plus cold and warm for the three cached stages. Stage order, cache-mode order, and suite order rotate across rounds. A complete run therefore has 80 serial measured commands. The runner rejects a different repeat count or a partial stage/suite selection for measurement.

## Workload identity and provenance

Each parser event records the original inputs and options, resolved source paths and content hashes, parser configuration, result validity, context identity, and elapsed time. The preflight compares the per-suite call-group multiset and context-reuse pattern across all four stages before measurement can use it.

Context sharing is compared as a partition: for each context, the multiset of workload fingerprints it parsed must match. Opaque context IDs and the order of independent test files do not identify a different workload. Original call order remains recorded. The first measured round encountered a reversed, sequential C/C++ test-file order; the original order-sensitive validation gate stopped after its 20 successful commands. Raw captures, test identities, cache checks and artifacts were revalidated without rerunning them, then the remaining rounds resumed. The final manifest retains this recovery audit; no timing was discarded or selected because of its value.

Path canonicalization is for matching metadata only. Captured source paths, resolved paths, compiler options, and order remain in the event records. `normalized_workload` replaces checkout and temporary roots and known generated reparse directory names. It sorts `input_sources` membership because the parser sorts user files before native processing. For the approved Clava-JS file-rebuild cases only, a source-content hash and recognized generated include-root shape must both match before the first `-I` entry is replaced with a stable role token in `normalized_workload`. The compiler arguments passed to the parser are never rewritten by this fingerprinting step.

The accepted identity reference is [`preflight-app-only-verified-r2/app-build-preflight.json`](../results/app-build-20261002-r1/preflight-app-only-verified-r2/app-build-preflight.json), with its command and artifact hashes in the adjacent `plan.json`. It records a valid, complete four-stage/eight-cell preflight, the established suite counts, and workload/context identities for 170 Clava-JS and 216 Java parser calls. The separate generated-include evidence is [`include-audit-run-java17-r3/include-audit.json`](../results/app-build-20261002-r1/include-audit-run-java17-r3/include-audit.json). That audit checks three named tests in each frozen stage and verifies the parser bytecode identity; it is provenance evidence, not a timing stage.

The report shares one scale across stages and cache modes within each suite, but uses a separate scale for each suite. Compare modes within a suite, not the vertical positions between the Java and Clava-JS panels. Four rounds show descriptive spread, not confidence intervals.

## Reproduce

Run from the Clava repository root. Use fresh output directories and keep them with their logs and JSON manifests. Do not delete or reuse a completed results directory.

```sh
APP_BUILD_STAMP="$(date +%Y%m%d-%H%M%S)"
APP_BUILD_RESULTS="experiments/protocol-comparison/results/app-build-20261002-r1/repro-${APP_BUILD_STAMP}"
APP_BUILD_PREFLIGHT="${APP_BUILD_RESULTS}/preflight"
APP_BUILD_MEASURE="${APP_BUILD_RESULTS}/measure"

python3 experiments/protocol-comparison/app-build/run_app_build_matrix.py \
  --phase preflight \
  --frozen-root experiments/protocol-comparison/results/matched-fast-20261002-r1 \
  --output-root "$APP_BUILD_PREFLIGHT"

# Run only with the host reserved for this serial 80-cell measurement.
python3 experiments/protocol-comparison/app-build/run_app_build_matrix.py \
  --phase measure --host-lock-confirmed \
  --lock-note "exclusive host for serial 80-cell App-build matrix" \
  --frozen-root experiments/protocol-comparison/results/matched-fast-20261002-r1 \
  --preflight-manifest "$APP_BUILD_PREFLIGHT/app-build-preflight.json" \
  --output-root "$APP_BUILD_MEASURE"

python3 experiments/protocol-comparison/report/render_app_build.py \
  --input "$APP_BUILD_MEASURE/app-build-matrix.json" \
  --output "$APP_BUILD_MEASURE/app-build-report.html" \
  --csv "$APP_BUILD_MEASURE/app-build-report.csv"
```

Preflight and measurement preserve the per-command logs and raw call records, alongside `plan.json`, the call CSV, and the machine-readable report payload. The renderer validates the payload before writing the HTML and sanitized CSV. Do not report timing results until the measurement finishes with all 80 cells valid. This README records the method only.
