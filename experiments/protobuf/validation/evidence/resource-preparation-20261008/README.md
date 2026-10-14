# Cached release resource preparation diagnostic

This is a resource-only diagnostic from 2026-10-08, not a refreshed full-suite benchmark. It exercises the published RC8 path with `BUILTIN_AND_LIBC`, warms one lookup before timing, and repeats 200 lookups per fresh JVM. The release executable and all runtime JARs remain unchanged. The fixed case places the committed `ClangResources` classes before the original JAR on the classpath.

Three untraced runs took 1522.047, 1533.942 and 1522.034 ms with the released consumer, then 159.358, 156.130 and 159.069 ms with the fix. Separate process traces cover one warmup plus ten lookups: the released path launched `chmod` eleven times, the fixed path zero times. Neither path queried a system resource directory because this mode requests bundled headers.

The fix retains manifest digest validation and repairs missing current-user executability through Java. Its regression test intercepts external `chmod`, removes executable permissions, and corrupts the executable to verify repair and rejection. The resource test class passed 40 tests, with three published-release assumption skips under the preserved local selector; generated binding verification passed. The filtered test command excludes the aggregate JaCoCo coverage gate because coverage of one class does not represent the full suite.

These measurements isolate setup overhead. They do not establish full-suite runtime recovery or remove the historical differences in bundled headers, native compiler builds and capture sessions. The historical benchmark data remains unchanged. No Text or FlatBuffers controls were rerun.
