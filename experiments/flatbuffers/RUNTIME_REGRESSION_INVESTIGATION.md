# Runtime comparison investigation, 6 October 2026

The RC3 report's 18.9% Java penalty does not reproduce the conditions of [the earlier decision report](https://draftlink.lmsousa.workers.dev/d/T8Nfm77p4Olp). It included forced-GC heap diagnostics that the earlier controls disabled. It also compares different implemented revisions. Treat the speed-based adoption gate as unresolved.

Each final Java observation executed 216 heap diagnostic samples. In the RC3 and Text dependency revisions, `SpecsSystem.getUsedMemory(true)` requests `System.gc()` twice, including an unconditional second request. In the Protobuf dependency revision, it requests GC once: that branch contains `cea5be9123be8508805c03be381a26af796ae7ff`, which removes the unconditional request. Thus the final comparison executes 432 explicit requests in eager/Text versus 216 in Protobuf. These are requests, not proof of exactly that many completed collections.

The earlier App timer stops before diagnostics and its overlay sets `SHOW_EXEC_INFO=false`. Its separate suite-control harness also requires that default to be false in the frozen sources. The earlier whole-suite controls therefore do not rescue comparability with the final GC-enabled measurements.

## Controlled diagnostic

A fresh run used the existing fixed 116-test comparison harness, with `-XX:+DisableExplicitGC` added equally to all three test workers. All nine observations passed with the pinned test identity, precompiled classes, one 512 MiB worker, no coverage agent, ccache disabled and absent from PATH, and rotated serial stage order. Heap logging and the production FlatBuffers verifier remained enabled. This is a diagnostic intervention; it changes GC policy and does not replace either the production-default or earlier App-construction benchmark.

Median summed JUnit testcase durations were eager **27.590 s**, Text **26.352 s**, and Protobuf **25.839 s**. The eager-minus-Protobuf gap was **1.751 s (6.8%)**, compared with **6.030 s (18.9%)** in the RC3 report. The gap shrank by **71.0%**. The original observations and the diagnostic were captured on different dates, so that reduction is descriptive, not an exact decomposition of runtime phases.

All three paired diagnostic differences remained positive: 1.659 s, 1.822 s, 1.786 s. A smaller performance reversal remains. This experiment does not isolate the second GC request from other explicit requests, and does not measure the equivalent intervention on the JS suite.

## Remaining explanation

The production reader adds `WireVerifier.verify` before generated accessor reads and `GeneratedNodes.validateRecord` before record decoding. Structural verification traverses the payload before its data is consumed; fidelity fixes also changed the AST and generated source behavior. Those changes can add work, but this experiment does not attribute the residual gap to a specific change. Removing validation to improve production timings would invalidate the malformed-input requirement.

The earlier report remains evidence for its frozen implementations. The final report remains evidence for its GC-enabled test bodies. Neither establishes the current implementation's App-building advantage. Reproduce the original App timer and disabled diagnostics on current builds, then profile the residual before accepting the performance gate.

[Committed diagnostic evidence](validation/evidence/runtime-gc-diagnostic-20261006.json) records every accepted duration, test counts and identity, source/classpath provenance, raw-result hash, and exact temporary diagnostic driver and init script. No runtime source was changed by this investigation.

## Optimization follow-up

The [matched optimization benchmark](OPTIMIZATION_BENCHMARK.md) measured updated Clava `09542632a` with specs-java-libs `ae7194a7` against the frozen production reader and Protobuf. Across 27 accepted serial observations, production Java test time fell 12.0%, while App construction improved only 0.6% in Java and 0.9% in JS. Updated FlatBuffers remained 7.3% and 3.6% slower than Protobuf in the respective App comparisons. The production improvement does not restore the earlier decision report's App advantage.
