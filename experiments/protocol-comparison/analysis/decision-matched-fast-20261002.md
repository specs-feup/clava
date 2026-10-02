# Matched fast-validation protocol comparison

Measured on 2 October 2026. The reviewed conclusion is in
`decision-matched-fast-20261002.json`; the permanent CSV contains the 80 accepted
whole-suite measurements, not earlier controls or preflight commands.

## Workload and timing contract

- Four rotated rounds: both suites, before-cache/direct plus Text, Protobuf and
  eager FlatBuffers in direct, cold and warm modes. Exactly 80 measured commands
  and 20 excluded preflight commands.
- Java: 116 selected tests, all pass, no skips. Actual Test Executor argv proves
  a 512 MiB worker without coverage agents or explicit-GC overrides. Coverage
  report tasks were skipped; build tasks remained up-to-date during measurement.
- Clava-JS: 164 selected tests, 158 pass and six skip. All test identities match
  across implementations. Original parse groups, cross-TU linking and necessary
  generated-code reparses remain intact. No additional fixture relocation.
- Every selected row has `fast_syntax=true`. Production Text and FlatBuffers
  now use the same Clang syntax-only frontend action already used by Protobuf.
- Whole-command wall time excludes compilation/preparation. Java JUnit sums are
  separately retained; these timers include testcase work beyond parsing. Wall
  minus JUnit is a residual, not a measurement of one exclusive phase.
- Bypass: no ccache invocations. Cold Java: 208 misses. Warm Java: 207 hits and
  one miss. Cold JS: 150 misses and 16 legitimate intra-command hits. Warm JS:
  166 hits and zero misses. Each cold command starts empty; each warm command
  restores the same validated seed, outside measured time.
- Native binaries: Release, `-O3`, LLVM 18.1.8. Text/Protobuf cache payloads use
  native Zstd with ccache compression disabled; FlatBuffers caches raw payloads
  with ccache compression. Bypass payloads are uncompressed. These are implemented
  branch comparisons, not a same-revision, compression-normalized format-only A/B.

## Sources and artifacts

| Stage | Clava source | Native source |
| --- | --- | --- |
| Before cache | `70c30bdb8cce66dba53c0e2963c3ec31cbc223bc` plus syntax-only and common harness backports | `522c60be801256999fd6aba4997038f440212975` plus syntax-only backport |
| Text + ccache | `bae33249933f57bd78de5121dbb9611ace955aa1` | `c549b4b2f490af8a1be20b5f30e7aa875c9c95fe` |
| Protobuf | `52bbaaed4259cbf4431e66e36afe3c163f1ec7cc` | `ab9d0238bd9c27eb9f156fd19967d16025137a37` |
| Eager FlatBuffers | `01c6b739e5606af12734a3a21c63a45ce5dd5e67` | `0a27378395b2f10d232cbd0f7b167e5e588f95b8` |

The before-cache control is not an untouched historical binary. Its native
syntax-only diff is SHA-256
`59b8cd4f8dd535aec53c85480bf04f1e6e2f4fc31ed22221ed41e145c9dd1ac3`.
Production Text and FlatBuffers Java/native changes are committed and pushed;
their original local native builds were subsequently rebuilt to the same hashes
used by the frozen matrix. No user-owned release-tag edits were staged.

## Results

Percentages below are medians of same-round percentage changes.

- Text warm vs Text bypass: JS **10.3% faster**, Java **29.7% faster**, all four
  pairs in each suite. Text cold vs bypass: JS 1.4% slower, Java 2.5% slower.
- Protobuf vs Text, warm JS: **3.1% slower**, all four pairs. FlatBuffers vs Text,
  warm JS: **3.8% slower**, all four pairs.
- FlatBuffers vs Text, warm Java: **6.0% faster**, all four pairs.
- FlatBuffers vs Protobuf, Java: **5.0% faster direct**, **4.4% cold**, **6.2%
  warm**, all four pairs in every state. JS binary directions are mixed in every
  state; no repeatable binary winner is demonstrated there.
- Protobuf vs Text, Java whole-command changes are mixed: median +0.3% direct,
  +0.2% cold and +0.2% warm. On warm Java, Protobuf's JUnit sums decrease in all
  four pairs (0.33–0.67 s), but outside-timer residuals increase in all four
  (0.37–0.78 s), cancelling the improvement in the whole-command ranking.

Before-cache vs Exploration also changes producer/reader overlap and other
implementation details. In particular, the Java direct-stage gap is not the
cost of executing ccache: direct commands make zero wrapper calls. Do not infer
a pure cache-overhead or intrinsic wire-format ranking from these stage totals.

## Causal controls kept separate

`decision-evidence-20261001.md` and its adjacent sanitized CSVs preserve the
separately dated interventions: Text syntax-only validation saves median 1.03 s
per JS command; disabling Protobuf's combined Gradle plugin/generation setup
removes median 0.4038 s of configuration. Neither control is pooled into this
fresh matrix. The latter is not a decoder measurement and does not distinguish
plugin internals from generation wiring. Historical isolated-file and grouped
replay controls are preserved separately, not presented as this suite cohort.

## Frozen-run recipe

Raw manifests are retained under
`results/matched-fast-20261002-r1/java-r1/results.json` and
`results/matched-fast-20261002-r1/js-r2/results.json`. Per-run logs, actual worker
argv, producer identities, JUnit XML and validated cache seeds are beside them.
The rejected first JS setup attempt (`js-r1`) failed before Vitest started because
workspace scaffolding was absent; it collected zero tests and no accepted timing.
The scaffolding was restored without source/fixture edits, aliases were checked
against each fresh stage, and all ten preflight cells reran successfully in `js-r2`.

Run from `experiments/protocol-comparison`, with the archived frozen stage tree:

```sh
python3 run_java_uninstrumented_matrix.py --phase preflight --matrix results/matched-fast-20261002-r1/java-stage-matrix.json --output-root results/matched-fast-20261002-r1/java-r1 --skip-agent-diagnostics
python3 results/matched-fast-20261002-r1/preparation/run_java_off_measurements.py
python3 results/matched-fast-20261002-r1/preparation/run_js_preflight.py
python3 results/matched-fast-20261002-r1/preparation/run_js_off_measurements.py
```

Exact frozen-run entrypoints are archived as `.py.txt` source evidence in
`matched-fast-20261002/`; copy them back to the preparation directory to use this
recipe. They are not a portable, automatic stage-construction tool. The input
stage manifest and built artifacts must be retained or recreated and verified.
Run suites serially on an otherwise quiescent host; do not run native builds,
other suites or browser automation concurrently with timed commands.

## Validation

Post-run audit recomputes stage source heads, native binary hashes, parser JAR
hashes, complete runtime identities and all three fixture categories. Metadata
enrichment records explicit derivations and preserves pre-enrichment manifests;
it never changes measured timings, outcomes, cache counters or captured argv.
The combined cohort must pass `analyze_deadline.analyze_cohort` before publication.
The report additionally requires `fast_syntax=true` in every selected measurement.
Its CSV omits local paths and embeds exact artifact/provenance hashes.

Validated enriched manifests (the original captured inputs remain preserved):

- Java SHA-256: `14013386433b2a7195eb690e62828a861a3f240d84aaca1e93eeea07823912c2`.
- JS SHA-256: `03374497b0ceb7875b1e327b37cf09c9fbbfacee86b0fac14175a415ff91a31b`.
- Enrichment/audit SHA-256: `3b7010b848f6074298dd9751408a51ef1311361a1c2e66592913d161bfbff0de`.
- Accepted measurement CSV SHA-256: `6ba7f846aef526d441e4957c1cd405d1c454bd42e3639a9d4550f6269f3db70f`.

The combined analyzer passes: 20 cell summaries, 26 paired comparisons and 104
paired deltas. Report tests: 61 pass; analysis tests: 41 pass. Focused rebuilt
native tests: Text three pass, FlatBuffers four pass. Browser checks cover
360/390/1100-pixel layouts, light/dark themes, expanded details and the selectable
80-row CSV without horizontal overflow. The integrated browser initially opened
but later navigation/open explicitly reported no connected automation host;
checks therefore used the permitted headless fallback.

Rendered decision brief: 95,464 bytes, SHA-256
`c6e034f7ce0468804acf6bc0afff4005d70adde3da1fcc257d16941cb9124255`.
