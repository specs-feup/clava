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
Two rotated cold timeline pairs reproduce +0.546/+0.524 seconds outside JUnit.
Internally timestamped project configuration costs +0.533/+0.437 seconds;
task-graph creation is approximately even. A same-root isolated Protobuf
configuration intervention then removes only its plugin/generation setup,
preserving dependencies, sources and generated Java inputs. Four rotated
`test --dry-run` pairs remove 0.4153/0.3923/0.3853/0.4971 seconds of configuration,
median 0.4038 seconds, with all tasks skipped and no test worker. The dominant
cause is Protobuf build integration, not parsing. This combined intervention
does not distinguish plugin internals from custom generation wiring, or account
for every remaining millisecond of command setup.

## Direct binary choice and validation normalization

FlatBuffers-minus-Protobuf Java command paired medians are -2.255 seconds direct,
-2.280 seconds cold and -1.615 seconds warm (6.2--6.5 percent), all four pairs
faster in every mode. JUnit sums independently improve by 5.3--6.2 percent.
This advantage therefore remains after excluding all build setup.

Original warm JS favors Protobuf by 1.015 seconds paired median, but FlatBuffers
also lacks the fast syntax-only validation used by Protobuf. A frozen-object
FlatBuffers overlay adds only that frontend action for validation and leaves
AST producers unchanged. Four rotated Protobuf-original/Flat-fast warm pairs
produce Flat-minus-Protobuf +0.49/+0.09/-0.70/-0.22 seconds, median -0.065 seconds,
two wins each: operationally tied, not a demonstrated FlatBuffers JS speedup.
All eight preserve 164 identities, 158 passes/six skips, and 166 cache hits with
zero misses. The malformed-input probe preserves failure status/error lines.
Both successful cache-prime commands and earlier collector-gate rejection are
excluded from measured rows. Native original, tool object, build metadata and
31 linked objects remain unchanged. Results and sanitized CSVs live under
`matrix-r2/decision-noagent-r1/flat-fast-syntax-control-r1`.

Sanitized permanent decision data is checked in alongside this note:
`decision-binary-pairs-20261001.csv`, `decision-java-setup-20261001.csv`, and
`decision-js-fast-validation-20261001.csv`. Raw diagnostic JSON SHA-256 values:

- Timeline: `889c174a4990eab3d7b5902be0e6ccdf52590f0335ab874083f1b816d181780a`.
- Config intervention: `2a5999c0d4474c0e9153bde1ba27c3c844e2bad7ca7f7447647b3b9ab4477bb7`.
- Fair JS control: `7465e8bf381b122691ef70861eeed4f5ea97fd68a34b705de26b23c9686fa34d`.

The updated private DraftLink `T8Nfm77p4Olp` was read back byte-for-byte against
the rendered HTML (1,548,127 bytes, SHA-256
`daab78c80bd3ef7238d953fb7ea5d6e249894a095fc7e6358b24c3879dc1f5de`).
Validation: 56 report tests and 41 analysis tests pass. Headless browser checks
cover 360/390/1100-pixel layouts, light/dark themes, CSV selection and expanded
details without horizontal overflow. T3 preview status/open explicitly reported
no available automation host, so browser checks used the allowed fallback.

## Decision and boundaries

Enable ccache. Seeded versus empty Text cache improves command time in all four
pairs: median -10.6% JS and -30.6% Java. Eager FlatBuffers has the most repeatable
Java improvement in the implemented-branch matrix: -4.5% to -6.5% command time,
all four pairs in every cache state. JS warm directions are mixed. Recommend
eager FlatBuffers with fast syntax-only validation for performance-first adoption,
given the normalized JS tie and consistent Java win over Protobuf, subject to release correctness,
generation, cleanup and one-path maintenance checks. Text plus ccache is the
lower-migration-cost choice; fast syntax validation does not require Protobuf.
Protobuf is already pipeline-integrated, not unfinished. None of these branch
results establishes an intrinsic universal wire-format speed ordering.

The before-cache commit also differs in live producer/reader overlap and native
build provenance. Cached Text/Protobuf use native Zstd; FlatBuffers uses ccache
compression. Historical isolated-file measurements are not grouped-suite
controls. Keep raw same-build grouped replay as supplementary evidence, with its
fast-validation normalization and compatibility patches stated explicitly.
