# October 8 refresh stopped at JavaScript cohort preflight

The repeatable collector stopped before any timed runtime observation. The selected JavaScript source `Clava-JS/api/LegacyIntegrationTests - CXX.test.ts` differs from the frozen cohort: SHA-256 `30d43ecd12d9ce790a5e57e8cbae456407d4358c86b6e336570da146e26f1735` was expected, while the isolated current snapshot contains `36b2e023ccbef4e118bc2e9fdcf958c8f6bff789f3b545affe7545de8afb34fa`. The changed file includes host-dependent CUDA skip handling and a platform-specific Setters golden path. The 164-test cohort was not re-baselined, and no timed runtime rows were collected.

The full failed run manifest, snapshot identity, partial runtime record and snapshot identity are directly readable in `runtime-failed/`; exact build/preflight logs, init script, and driver stdout are preserved in `runtime-failed/raw-logs-and-init.tar.gz`.

A standalone memory diagnostic then completed on the same isolated snapshot: three fresh JVMs per workload and 20 strict-cleanup cycles per JVM (120 total). All six JVMs exited successfully; every cycle reported App collection and clean parser/resource state. These are separate diagnostic measurements; they are not combined with the older runtime capture and were not published to DraftLink. See `memory-followup/memory.json`, both normalized workload sidecars, and the raw command/log/time archive.

Across the three JVMs per workload, median GNU time peak RSS was 736,052 KiB for NAS+ and 273,008 KiB for C++ templates. The median of each JVM's 20-cycle heap medians was 43,456,192 bytes live with the App alive and 16,533,916 bytes retained after App release for NAS+; templates measured 17,798,948 bytes live and 16,859,236 bytes retained. These are fresh standalone diagnostic values, with no cross-session comparison or runtime timing median.

The release was `v18.1.8_6-rc8`, using published manifest SHA-256 `9ed7487eff35ef0cdbcd3628fb7a9f429005b4e14a6fa4070db50ba14d0c8ecc` and Linux tool SHA-256 `ce467a8355c389c45024490e1ee67930737db090d8e5b236db82ecf1f69c9d27`. The existing private report was read back and matched the pre-task HTML byte-for-byte.
