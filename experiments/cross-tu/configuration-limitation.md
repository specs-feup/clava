# Configuration-dependent definitions at one physical header location

Finding ID: **CTU19** (additional limitation; not part of the previously approved fixes).

## Finding

The declaration cache currently treats two function definitions as one repeated physical definition when they have the same AST class and equal valid source locations. It then retains one deterministic representative, selected by translation-unit path. That is unsafe when the same header range is preprocessed differently in different translation units: equal file locations do not imply equal AST bodies.

The initial native-parser probe did **not** confirm that this cache path is reached by ordinary parsing: it found no `get` definition in either TU's descendant tree, so the lookup was never tested. The revised probe inspects each call's raw `DIRECT_CALLEE` target, records the native tree/cache baseline, then attaches a deep copy of each retained definition to its owning TU and queries the cache. The latter is an explicitly synthetic post-mutation scenario, not evidence that normal parsing inserts those header definitions into the cache.

In that synthetic scenario, the two bodies have one physical header range but differ after preprocessing. `DeclarationsCache.Definitions.add()` treats equal source locations of the same AST class as one repeated definition and picks by TU path. The characterization confirmed that both call lookups return that same deterministic representative even though one TU parsed a different body. This is a cache-level limitation, not yet a confirmed native-parser cross-context bug. The probe does not invoke a separate compiler or claim that the example is a valid C++ program.

## Self-contained example

```cpp
// api.hpp
inline int get() { return VALUE; }

// a.cpp
#define VALUE 1
#include "api.hpp"
int a() { return get(); }

// b.cpp
#define VALUE 2
#include "api.hpp"
int b() { return get(); }
```

Both translation units parse the `get` body with a different expansion: the `a.cpp` call target's body returns `1`, while the `b.cpp` target's body returns `2`. In the observed native parser structure, there are no corresponding in-tree `get` definitions for the cache to index. The updated probe checks those detached targets, verifies that the native cache lookup is missing, then attaches deep copies of both targets to their corresponding TUs. The characterization observed one unique representative for both declarations and calls, selected by the full TU path. The committed desired-behavior probe includes both original bodies and the selected copy's origin/body in assertion diagnostics.

This example deliberately violates C++'s multi-TU ODR requirement: corresponding inline definitions must have matching token sequences, and these preprocessed bodies differ. The current C++ working draft says a program with such mismatching definitions is ill-formed with no diagnostic required ([`[basic.def.odr]`](https://eel.is/c%2B%2Bdraft/basic.def.odr)). A compiler accepting and linking the translation units does not make the selected runtime body well-defined. The concern here is Clava's analysis result: silently presenting one TU's parsed body as the definition seen by another caller hides the configuration discrepancy.

## Reproduction

From `ClangAstParser` in this workspace, run:

```bash
gradle -I ../experiments/cross-tu/tests.init.gradle test \
  --tests '*CrossTuLimitationsReviewTest.CTU19*' -PcrossTuReview=true
```

The test runs in forward, reverse, parallel, and parallel-reverse TU modes. It asserts that the native call targets have different bodies and that there are no in-tree definitions/cache candidates. It then creates and attaches the two deep copies, clears the app cache, and requires ambiguity plus preservation of each caller's own body. All four cases fail on the current implementation with UNIQUE results and a shared path-determined body. This confirms the synthetic limitation; it does not establish that ordinary parsing exposes that behavior.

## Options for a robust resolution

1. **Track configuration provenance on definition occurrences.** Add a preprocessing/configuration fingerprint to the occurrence identity, distinct from semantic declaration identity. It could include relevant macro definitions/expansions, language mode, target, include search paths, and forced includes. Coalesce repeated physical definitions only when both location and configuration fingerprint agree. This directly models the distinction, but requires the native dumper/parser to transport enough provenance and define a stable fingerprint contract.
2. **Compare normalized AST bodies before coalescing.** For definitions sharing a file range, compare a structural body representation that ignores parser IDs and source bookkeeping but retains semantics. Equal bodies can remain one physical definition; differing bodies become ambiguous. This avoids a broad compiler-option fingerprint, but requires a carefully maintained equivalence algorithm and may be costly for large inline/template bodies.
3. **Conservatively mark all repeated cross-TU definitions at one header location ambiguous.** This avoids the unsafe winner with minimal new metadata. It can report false ambiguity for identical header definitions included in many TUs, making common inline/header lookups less useful.
4. **Keep per-TU occurrence results and separate them from entity-level declaration lookup.** A caller in a TU can resolve an inline definition from its own parsed tree; cross-TU requests would be ambiguous unless configuration equivalence is established. This preserves local context but adds another lookup model and API behavior.

The key design boundary is to keep semantic entity identity separate from occurrence/configuration identity. A matching external declaration may still denote the same C++ entity while its repeated definition occurrence came from a different preprocessor context. Avoid using a shared physical range as proof that those bodies are interchangeable.

## Observed validation

The characterization ran successfully in all four modes on 2026-10-19. Native trees contain no `get` definition; raw call targets retain detached header bodies returning 1 and 2, and the native declaration index reports MISSING. After attaching a deep copy of each target to its own TU and calling `App.clearCache()`, both index resolutions are UNIQUE and both calls select the lexicographically first TU's copy. The pre-copy bodies remain different. This confirms the same-location coalescing risk for that explicit synthetic post-mutation App, not a cross-configuration redirect in an ordinary unmodified native parse.

The committed opt-in probe now states the desired behavior instead of requiring that incorrect selection to continue: conflicting indexed bodies should be AMBIGUOUS, and calls with their own compiler-selected body should retain that body. It is expected to fail until this additional finding is addressed.
