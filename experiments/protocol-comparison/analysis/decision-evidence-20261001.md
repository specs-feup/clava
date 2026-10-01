# Uninstrumented protocol decision evidence

The decision brief uses the original uninstrumented Clava-JS partition and a
fresh Java-only uninstrumented matrix. Do not pool coverage-enabled Java rows
into its headline charts.

## Accepted cohorts

- Original JS: `results/deadline-20260930/matrix-r2/results.json`, 40 measured
  commands, four rotated rounds, identical 164-test selection, 158 passes and
  six skips. `select_cohort.py` preserves these rows exactly in a JS-only
  manifest, validated against the fresh Java plan and artifact provenance.
- Java: `results/deadline-20260930/matrix-r2/java-noagent-matrix-r2/results.json`,
  SHA-256 `f53109124bbfaf402b86bbe39f03083b0abd8fb4624cc2227e0b55afcada1e23`.
  Forty measured commands and ten excluded preflights, 116 passes each. Actual
  Test-worker argv proves no agent or GC override and a 512 MB heap. Build tasks
  remain up-to-date, coverage report tasks skipped. Cache outcomes are zero
  wrapper calls direct, 208 misses cold, and 207 hits plus one miss warm.
- Java coverage diagnostics: 16 separately interleaved direct/cold ON/OFF
  commands. JSON SHA-256
  `c45b7c8d7a22a91a85a3c75f3b7cead33cf9b6fe43aad609e975d863ee46db58`.
  Do not merge these into the uninstrumented cohort.

## What explains the suite reversal

The original JS suite has 170 outer AST-building calls and 130 outer syntax-only
calls. Two diagnostic Text/Protobuf pairs show mean differences of +1.091420 s
for AST building, -1.480953 s for syntax checks, -0.295467 s for remaining command
work, and -0.685 s overall. The native/runtime builds differ and input ordering
varies, so this is descriptive accounting, not a format-only intervention.

Text invokes its AST-dumping action during syntax validation, drains stderr into
a Java string, and discards the AST output. Protobuf selects SyntaxOnlyAction.
The same-build replay already applies that fast validation to both formats; it
therefore removes a real implementation difference in the original suite.

The isolated Text-only intervention is in the dual-wire scratch workspace under
`full-js-outer-parse-r1/syntax-only-control-r3`. All eight commands share native
binary SHA-256
`531150d7693823d23d4e9731ebd7c2a2e2642b06f79e4a95cb4ad52c76f5188c`.
The sole code-path switch adds syntax-only validation. Four rotated ON-minus-OFF
command differences are -0.60, -1.12, -0.94, -1.88 s, median -1.03 s. Syntax-only
span differences are -1.562562, -1.488569, -1.409119, -1.517482 s, median
-1.503026 s. Captured stderr falls from 61,126,611 to 14,915 characters per
command. Each command performs 169 native validations inside 130 outer groups.
All test identities/outcomes and paired source-content sets match. Natural input
order varies at 25 distinct ordinals; raw path/options hashes are not identical.
The malformed-input probe preserves failure status and error lines. Earlier
launch-failed/environment-contaminated attempts remain preserved and excluded.

Java does not perform those 130 validation groups. Coverage ON/OFF controls show
a larger Protobuf instrumentation penalty. Without coverage, paired Protobuf
test-body differences are -0.102 s direct, -0.018 s cold, and -0.483 s warm.
These JUnit sums include testcase setup, code generation and assertions, not
pure parsing. Outside-JUnit paired residuals are about +0.53 s across all modes.
Their precise internal cause is not established. Never label the residual a
parser regression or assign it to an unmeasured Gradle task.

## Decision and boundaries

Enable ccache. Seeded versus empty Text cache improves command time in all four
pairs: median -10.6% JS and -30.6% Java. Eager FlatBuffers has the most repeatable
Java improvement in the implemented-branch matrix: -4.5% to -6.5% command time,
all four pairs in every cache state. JS warm directions are mixed. Recommend
eager FlatBuffers for performance-first adoption, subject to release correctness,
generation, cleanup and one-path maintenance checks. Text plus ccache is the
lower-migration-cost choice; fast syntax validation does not require Protobuf.
Protobuf is already pipeline-integrated, not unfinished. None of these branch
results establishes an intrinsic universal wire-format speed ordering.

The before-cache commit also differs in live producer/reader overlap and native
build provenance. Cached Text/Protobuf use native Zstd; FlatBuffers uses ccache
compression. Historical isolated-file measurements are not grouped-suite
controls. Keep raw same-build grouped replay as supplementary evidence, with its
fast-validation normalization and compatibility patches stated explicitly.
