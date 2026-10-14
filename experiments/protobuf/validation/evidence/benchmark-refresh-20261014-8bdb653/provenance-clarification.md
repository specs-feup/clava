# Provenance clarification

This addendum clarifies metadata for the October 14 capture. The recorded timing and memory observations remain unchanged. The original runtime collector and source records are retained byte-for-byte.

The source workspace records clang-dumper checkout `ec4d80e9c73766df32355cd5380f9d4414443102`. The runtime selected published `v18.1.8_6-rc8`, whose release repository revision is `e874debc111ec105fa5d1efa17ecd97300613178`; the measured executable SHA-256 is `ce467a8355c389c45024490e1ee67930737db090d8e5b236db82ecf1f69c9d27`. The earlier summary's `clang-dumper` revision refers to the selected release repository, not the source workspace checkout. The current report presents those identities separately.

The original `build_environment.ccache_nocompress=true` field records the environment passed by the benchmark launcher to Java. In cache-enabled runs, `ClangCcacheAdapter.java` sets `CCACHE_COMPRESS=true` and removes `CCACHE_NOCOMPRESS`; direct mode disables ccache. The adapter source SHA-256 is `e6f48b8ca52ea62caea6ef203c51bbd720e380434866c40194fcafaabf382686`. The field therefore did not describe the effective cache subprocess policy. The machine-readable correction is in `provenance-clarification.json`.

The updated report was rendered by Clava commit `0ad0eddbd`, which also records these two identities separately in the source table. Its report-only changes do not alter the 48 timed rows, six memory JVMs, or frozen Text and FlatBuffers controls.
