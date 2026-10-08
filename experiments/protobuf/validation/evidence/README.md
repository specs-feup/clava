# Integrated validation evidence

The October 7 corpus capture uses producer executable SHA-256
`876522bb24b04574e68ec2f789e9a6f088346c26ac82d49421e161d56b550ef1`.
`integrated-validation-20261007.json` records the source revisions and hashes
of the captured evidence. The earlier `cac547839da8` coverage file remains
historical evidence; use the `876522bb24b0` file for the integrated producer.

All 1,062 native cases produced valid nonempty Protobuf streams. Clava passed
strict parse, generate and reparse stability for 773 cases. Independent LLVM
18 syntax checks passed all 773 generated outputs. The remaining 289 cases
failed and remain in the raw consumer summary, including unsupported nodes,
unsupported printing and generated-output failures. These results do not
establish complete corpus support. Raw generated files and diagnostics remain
at the capture paths recorded in the summaries.

The full Java parser check passed 249 tests, with three published-resource
skips. The full JS suite passed 162 of 164 tests, with two existing pending
tests, using the matching LLVM 18 OpenMP header. The cross-TU log records
linking between two translation units. Shared-library XML failures are
recorded separately against the unchanged base; the XStream warning remains.

The two memory captures each contain three JVM repetitions of 20 parse and
release cycles. All 120 cycles collected the App and left no parser file
descriptors, mapped work files or temporary parser directories. The captured
observer descriptors are excluded only when both their number and target
match the baseline. Live heap, release-retained heap, JVM high-water RSS and
GNU time peak RSS are distinct measurements. Explicit GC belongs to these
memory probes and is absent from runtime benchmark commands.

Runtime measurements are captured separately on October 8. They measure only
updated Protobuf. The report renderer retains the earlier Text and FlatBuffers
controls and does not calculate paired statistics across sessions.

## Final canonical consumer checks, October 8

`final-canonical-20261008.json` records the tested source revisions, local
producer and selector hashes, test counts, and raw-output digests. The clean
ClangAstParser check passed 250 tests with three release-only assumption skips;
binding drift verification, resolver tests, and the coverage gate passed.
`ClavaWeaver installDist` passed. The final Clava-JS source suite passed 162 of
164 tests with two skips. The Lara-JS source suite passed 54 tests with one
expected failure. The source-suite counts exclude generated test copies from
package output. Both package-runtime smoke checks also passed.

The parser check used the local producer selected by the preserved user
selector, with the matching LLVM 18 OpenMP include available to compiler
invocations. The three release-only tests were skipped because they require a
published dumper resource. The archived pre-fix runs document the temporary
package-discovery and module-identity failures; the final source suites passed
after the packaging corrections. Raw logs and JUnit reports are kept in the
local archive identified by `final-canonical-20261008`; the JSON records their
digests without embedding machine-local paths.

## Published RC8 consumer checks, October 8

`released-rc8-20261008.json` records validation against the published
`v18.1.8_6-rc8` prerelease and its selected Linux x64 assets. The clean
ClangAstParser check passed all 253 tests with four skips for direct native
integration cases that require a native build sibling. All three tests that
were previously skipped for unpublished resources ran and passed, including
separate-JVM initialization, cache reuse, and the released AUTO-libc probe.
Binding drift verification, all ten release-resolver tests, resource cleanup
checks, and the normal JaCoCo gate passed. `ClavaWeaver installDist` also
passed, and its installed parser JAR embeds the RC8 selector.

The evidence records the manifest, executable, schema, descriptor, and include
archive hashes, along with the validated extracted-cache key and JUnit, log,
and JaCoCo digests. Raw output stays in the isolated local validation archive;
the committed report contains no machine-local paths. The canonical selector
and Lara engine worktree patch were preserved during this check.

## Nightly consumer CI, October 8

`consumer-ci-rc8-20261008.json` records the successful nightly run against
Clava commit `360a41c5e48385effdbb459fa30cc0efc05da68b`, including the Java
summary and all six Node/OS matrix summaries. Node 24 on Windows first lost
runner communication; GitHub returned 404 for that attempt's log, and the
successful second-attempt job log and original annotation are archived
locally. This is nightly-run evidence only: it does not claim all PR checks
passed; separate SonarCloud/CodeQL, Lara shared-resolver, and
specs-java-libs 17 legacy checks remained unresolved.
