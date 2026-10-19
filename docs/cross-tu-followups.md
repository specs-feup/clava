# Additional cross-TU findings for review

These findings extend the original review. They are not included in the thirteen approved fixes. Each can be approved, investigated further, or dismissed independently. The inline checklist contains the same examples and choices.

## CTU17: integral template argument identity loses type and width

Status: confirmed data-model limitation; the new lookup fails conservatively. Severity: medium (missing valid matches), potentially high if value-only matching is reintroduced.

```cpp
template<auto N> struct Box {};
Box<1> integer_box;
Box<1L> long_box;
```

C++ gives these different specialization types: the first argument has type `int`, the second `long`. `TemplateArgumentIntegral` stores only `DataKey<Integer> INTEGRAL`; it cannot express that distinction or arbitrary-width values. Matching only the stored `1` could connect unrelated specializations. The new key instead retains payload-object identity, so independent dumps do not establish a match even for some compatible `template<int N>` specializations.

`DeclarationIdentityUnitRegressionTest.integralArgumentsWithoutTypeAndWidthCannotEstablishEquivalence` verifies the conservative rule. `CrossTuTemplateRegressionTest.unsupportedCrossTuIntegralSpecializationArgumentsFailClosed` verifies that missing specialization support never falls back to an unrelated primary body. Same-TU direct calls can still use the actual compiler-selected declaration.

Proposed action: extend the native/schema/Java representation with exact integral bits, signedness, width, and argument type (or a compiler canonical argument identity); add `template<auto>` and values larger than 32 bits to transport and lookup tests. Do not change the key to compare Java integers alone. This requires coordinated producer and reader work.

Decision: [ ] Fix. [ ] Investigate. [ ] Dismiss.

## CTU18: the post-parser pass removes written function specializations

Status: confirmed parser limitation. Severity: high for source preservation and specialization-aware transformations.

```cpp
// use.cpp
template<class T> T target(T);
template<> int target<int>(int);
int use() { return target(1); }

// impl.cpp
template<class T> T target(T x) { return x + 10; }
template<> int target<int>(int x) { return x + 100; }
```

The call selects the written `int` specialization, whose body adds 100. `DeleteTemplateSpecializations.applySimple()` queues deletion for every function whose template kind is a specialization, without distinguishing a compiler-created instantiation from written source. The explicit implementation therefore disappears from the application tree. A tree-only index cannot find it. The new normalization gate preserves the selected specialization rather than substituting the primary body that adds 10, but that safety improvement does not restore the deleted implementation or guarantee correct regenerated source.

The template regression suite checks that primary templates continue to work and explicit specializations never select the wrong primary body. An opt-in desired-behavior probe separately requires the written specialization to survive and resolve across TUs.

Proposed action: transport Clang's precise specialization/instantiation kind; preserve written explicit specializations and explicit instantiation syntax; remove only compiler-generated copies when the source transformation model requires it. Add compile-and-run round trips for calls, explicit specializations, instantiations, and no-parameter templates. Simply removing the deletion pass is insufficient without validating code generation.

Decision: [ ] Fix. [ ] Investigate. [ ] Dismiss.

## CTU19: one source range can describe different header definitions

Status: confirmed in a synthetic post-mutation application; ordinary native parsing does not reproduce the cache redirect; see `experiments/cross-tu/configuration-limitation.md` for exact lookup evidence. Severity: high for transformations accepting mixed configurations.

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

Both compiler invocations accept their individual TU. The resulting inline definitions occupy the same physical source range, but their parsed bodies differ. The combined project violates the C++ one-definition rule; a linker need not diagnose it. Clava still needs to avoid treating the two bodies as an established equivalent edit target. In an ordinary native parse these header bodies remain detached on the direct call targets and are absent from the declaration index, so this example does not trigger its coalescing rule. The probe then explicitly inserts a deep copy of each retained body into its own TU and clears the cache. In that synthetic post-mutation App, the cache accepts equal source ranges as repeated occurrences and returns one UNIQUE deterministic representative to both calls, despite the bodies remaining different. The mutation is explicitly modeled as:

```java
// For each call, insert a copy of its retained header definition into its own TU.
aTu.addChild(aCall.get(CallExpr.DIRECT_CALLEE).orElseThrow().deepCopy());
bTu.addChild(bCall.get(CallExpr.DIRECT_CALLEE).orElseThrow().deepCopy());
app.clearCache();
// Both cache queries are now UNIQUE and both calls select the same copy,
// although their original compiler-selected bodies return 1 and 2.
```

This setup and result were validated in all four modes.

Proposed action: retain all definition occurrences and their TU/configuration provenance, resolve a TU-local call to its occurrence where possible, and expose conflicts when cross-TU compatibility is unproven. Add compiler-command/configuration fingerprints and definition compatibility evidence before sharing a representative. Exact fingerprints alone do not detect every preprocessor/include-context difference. The separate report compares conservative conflict handling, fingerprints, semantic fingerprints, and occurrence-preserving symbol graphs.

Decision: [ ] Fix. [ ] Investigate. [ ] Dismiss.

## CTU20: direct AST edits still require manual cache invalidation

Status: confirmed API contract limitation. Severity: medium; wrong results are possible after unsupported direct mutations.

```java
// app initially contains a prototype and implementation named target
app.getFunctionImplementation(prototype); // builds an index generation
implementation.set(FunctionDecl.DECL_NAME, "renamed"); // direct data-store edit
app.getFunctionImplementation(prototype); // old index may still select renamed
app.clearCache();
app.getFunctionImplementation(prototype); // rebuilt index correctly finds no target
```

`App` owns a lazily created `DeclarationsCache`. Arbitrary node/data-store writes do not notify it. Structured keys fix identity collisions, but do not make an existing index coherent after edits. The opt-in `CTU20_directRenameInvalidatesAnExistingIndex` probe checks this exact sequence without a clear. Existing mutation regressions explicitly call `clearCache()`; the supported parameter/return-type/rename joinpoint actions and generated-global insertion are tested separately. This branch does not promise automatic invalidation for every `set`, detach, or insertion.

Proposed action: assign an app mutation revision through centralized write/tree operations; rebuild the declaration index when its revision differs. A transaction API could amortize rebuilds for many edits. Preserve clear ownership for detached nodes and independent apps, and test rename, type, linkage, scope moves, insert/remove, and batch edits. Registering invalidation separately at every consumer is easier initially but more fragile to maintain.

Decision: [ ] Fix. [ ] Investigate. [ ] Dismiss.
