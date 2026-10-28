# Cross-TU fixes and supported contract

This document describes the earlier structured-key implementation. The unpublished local USR experiment and its current contract are documented in [cross-tu-usr.md](cross-tu-usr.md).

The approved work addresses CTU01, CTU02, CTU04, CTU05, CTU06, CTU07, CTU08, CTU10, CTU11, CTU13, CTU14, CTU15, and CTU16 together. CTU03 was investigated separately and reported as [clang-dumper issue #58](https://github.com/specs-feup/clang-dumper/issues/58). Its native fix is outside this change.

## Two identities, two responsibilities

`DeclarationIdentity` describes named language entities for application-wide declaration/definition lookup. `NormalizationKey` describes AST occurrences for the parser's reference-repair pass. Neither uses a generated display signature as a semantic identifier.

Declaration keys carry semantic scope, formal linkage, full originating TU paths for internal linkage, structured parameter types, variadicness, member cv/ref qualifiers, calling convention, language linkage, and supported template structure. Typedefs and sugar are unwrapped; array/function parameters are adjusted; top-level parameter cv is ignored while pointee cv and address spaces remain distinct. Return types do not select overloads, but incompatible return types cannot supply a definition. Function-pointer exception specifications distinguish throwing and non-throwing types, with equivalent `noexcept` spellings normalized.

Variable keys identify entities by scope/linkage/name and thread-local storage. A separate compatibility check permits completion of an extern array's outer bound while rejecting incompatible types. Initializers make `extern` declarations definitions. Local statics, block externs, internal C++ const objects, and out-of-line static members retain their separate rules. Generated shared globals now carry global-storage/external-linkage metadata, invalidate the cache after insertion, and emit explicit `extern` for a const definition so reparsing preserves external linkage.

The index keeps immutable prototype lists and distinguishes missing, unique, and conflicting function definitions through `DefinitionResolution`. Separate definition occurrences at different source ranges are ambiguous rather than selecting whichever TU was traversed first. Repeated occurrences at one physical source range use a deterministic origin-path representative. This last rule is **not an ODR or compilation-configuration proof**; CTU19 records the remaining risk.

The parser freezes every candidate and queried reference key before rewriting any field. Locations retain full paths, coordinates, macro flags, and invalid-location descriptions. Signature values retain their runtime type and container structure. References snapshot node class and parser ID without formatting their data stores. A reference rewrite therefore cannot invalidate a later lookup key. Missing or ambiguous candidates are left unchanged. Invalid locations, macros, and semantic/syntactic initializer lists retain their exclusions.

Origin metadata is assigned from each parser registry's owning TU, including detached header declarations. A referring node cannot lend its TU identity to an unrelated target. Normalization indexes are pass-local and released in `finally`. Unchanged lists and optionals are retained; collision diagnostics keep at most five compact samples and never render complete node contents.

## CTU06 compiler validation

Before changing variadic lookup, `compiler_oracles.py` checked both Clang 21.1.8 and GCC 15.2.0. For this project:

```cpp
// a.cpp
int choose(int);
int use() { return choose(1); }
// b.cpp
int choose(int, ...) { return 2; }
// main.cpp
int use();
int main() { return use(); }
```

Each source file compiles independently. Linking the executable fails with an unresolved `choose(int)`: the variadic definition denotes another function. Changing the definition to `int choose(int)` makes the program link and run. Making both overloads visible at the call causes a compile-time ambiguity. Thus a cross-TU declaration lookup must leave the original fixed-arity declaration unresolved; it must not perform overload selection using declarations absent from that TU.

Exact commands and diagnostics are saved in `experiments/cross-tu/compiler-oracles.json`. Regenerate them with:

```sh
python3 experiments/cross-tu/compiler_oracles.py --compiler clang++ --compiler g++ \
  --output experiments/cross-tu/compiler-oracles.json
```

## Template and mutation boundaries

Implicit instantiations still expose their generic source template to transformations. A written specialization is not normalized to the primary merely because a primary link exists: normalization requires valid, equal source starts. Missing evidence preserves the specialization. A producer-supplied explicit specialization/instantiation kind would be stronger than this source-location discriminator.

Integral template arguments currently transport a Java `int`, without original type or width. The new identity treats distinct payload objects conservatively: it will miss some valid cross-TU specialization matches rather than join `template<auto>` arguments such as `1` and `1L`. Unsupported types, dependent builtin placeholders, unresolved exception specifications, and unsupported template arguments likewise do not establish cross-TU equivalence. Function-valued template arguments include their overload identity.

The existing post-parser pass deletes function template specializations, including written explicit specializations. Consequently this branch cannot promise their survival, code generation, or cross-TU definition lookup. CTU18 documents this separately; preventing a wrong primary-body match does not repair that deletion pass.

`DeclarationsCache` remains an immutable **generation**, not an automatically coherent view of arbitrary AST mutations. Call `App.clearCache()` after direct identity-changing edits (name, type, scope, linkage, storage, insertion/removal). Existing supported joinpoint actions and generated-global insertion are exercised, but generic node/data-store writes do not notify the app. CTU20 proposes an app revision or centralized mutation notification.

Other unproven areas include constrained/templates with dependent expressions, template-template and structural non-type arguments, modules/visible-no-linkage declarations, full C tentative-definition/common/inline semantics, differing compiler configurations, and whole-program ODR equivalence. Missing/ambiguous results are intentionally safer than a speculative source transformation. This is a tested supported subset, not a claim to implement every C/C++ linkage rule.

## Validation and performance

All approved native-project cases are ordinary regressions. Source fixtures exercise serial and parallel parsing with both **actual** TU orders, and assert that no requested TU disappears. Graph regressions cover frozen direct/optional/list references, cycles, shared targets, basename collisions, origin ownership, invalid/macro locations, initializer forms, mutation during normalization, ambiguity, repeated invocation, and six shuffled 1,000-target graphs. Additional identity tests cover scope aliases, cv/ref matrices, TLS, parameter adjustment, C arrays, generated/reparsed const globals, incompatible declarations, and conservative metadata fallbacks.

Final validation: **503 parser tests passed, 4 opt-in review methods skipped**; the focused cross-TU run passed **294 cases**. The complete JavaScript suite passed **166 tests**, with 2 existing skips; TypeScript compilation passed. The explicit remaining-review run failed all **16 expected cases**, separately reproducing CTU03/18/19/20.

The full parser and JavaScript results are recorded in `experiments/cross-tu/results.json`. CTU03 and additional desired-behavior review probes remain explicitly opt-in and are reported separately; a passing ordinary suite does not mean those problems are repaired. The experiment init script disables JaCoCo instrumentation and aggregate coverage enforcement, and isolates native resources for the full parser run. It does not disable ordinary tests.

The hot path no longer constructs signature strings or complete-node collision dumps; each normalization target is snapshotted once per pass. Tests verify zero `toContentString()` calls on collisions. Type keys are memoized within one declaration-index generation. These are mechanism and allocation changes, **not a measured end-to-end speedup**. A fresh JFR comparison of identical workloads/configurations is needed before making a timing claim about issue #270.

## Options for the next architecture step

1. **Extend the current structured identity model.** Smallest migration cost and an explicit supported subset. Add exact template argument types/values, canonical scope/owner metadata, compiler definition kinds, configuration fingerprints, and app revisions. The cost is maintaining more C/C++ semantic rules in Java.
2. **Have Clang emit canonical entity identity and compatibility facts.** Recommended direction for broader language coverage. Evaluate Clang USRs or another compiler-produced identity alongside formal linkage, canonical type identity, specialization kind/arguments, definition status, and a configuration fingerprint. Keep per-parse occurrence IDs separate and retain candidate conflicts. A mangled name alone is ABI-dependent and does not prove ODR equivalence.
3. **Replace normalization with an explicit occurrence graph and symbol graph.** Largest refactor, clearest ownership: parser references resolve by `(parse ID, node ID)`; a separate symbol graph connects declarations and definitions. Preserve occurrences in each TU and make ambiguous/unsupported relationships explicit. This removes the need to infer graph ownership from source signatures, but requires producer/schema/reader coordination and a compatibility migration for transformations.

Options 2 and 3 fit together. Stabilize the regression contract first, add compiler-produced facts, then migrate consumers; do not replace tested cases with a second string-signature system.
