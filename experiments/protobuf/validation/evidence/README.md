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
