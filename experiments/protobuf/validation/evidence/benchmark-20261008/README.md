# October 8 Protobuf measurement capture

This capture contains 48 valid measurements: four repetitions for each of two
suites, three cache states and two timing boundaries. App construction timing
and separate uninstrumented full-command wall timing remain separate. The
runtime snapshot, worker settings, actual workload identities, JVM options,
cache counters, artifacts and timestamps are recorded in the results.

The archived driver is the exact session-specific script identified by the
results hash. Its absolute paths describe this capture, rather than a portable
entry point. Use the suite harnesses for new experiments. The only measured
stage in this session was Protobuf. No Text or FlatBuffers controls were rerun.
The frozen October 7 control data is retained for the chart renderer; it is
not a new measurement or a same-session pair.

The measured native producer is `ab358d0c`, executable SHA-256
`876522bb24b04574e68ec2f789e9a6f088346c26ac82d49421e161d56b550ef1`.
Subsequent native changes affect CI, Python helpers and Windows dependency
builds. The Clava snapshot is `8d1705613`; later changes affect the memory
observer, evidence, CI, CMake wrappers and test-resource packaging. These
measurements identify their actual runtime artifacts, rather than claiming to
measure later packaging or build changes.

Memory evidence is a separate October 7 session in the parent directory.
Neither the memory probes' explicit GC nor coverage instrumentation is used
in these runtime measurements. External Zstd compression remains enabled.
