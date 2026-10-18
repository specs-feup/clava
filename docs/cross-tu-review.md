# Cross-TU experiments and findings for review

2026-10-19. All decisions below are pending. This branch adds tests and experiment tooling; it changes no production algorithms. Approving a finding means accepting the proposed behavior as a requirement for a later fix. Dismissing one means keeping the existing behavior or narrowing the supported contract. Neither decision requires accepting a particular refactor.

The inline checklist source is `experiments/cross-tu/findings-review.html`. It includes every finding's example and evidence, with mutually exclusive Fix, Investigate, and Dismiss checkboxes. Nothing is preselected. Send the generated decision text through chat to request work; checkbox state alone does not approve a change. When direct chat submission is unavailable, the checklist offers copying the text for pasting into chat.

## Workspace and reproduction

Workspace: `/home/lmsousa/Documents/Projects/SPeCS/cross-tu-experiments`. Each nested repository has its own `cross-tu-experiments` worktree and branch, created from the corresponding checkout in `ast-flatbuffers`, without substituting `master` or `staging`:

- `specs-java-libs`: `ae7194a7b3e981913d40a4403acfc759808b6c3a`.
- `lara-framework`: `b780d0b8ba0aac2670ad5aa98998883a2a4f8030`.
- `clava`: `aa0946736bf241d9bac27a1c8825fffd4df0b2ba`.

The original checkouts were left untouched, including untracked parser cache files. The setup-workspace script created the worktrees and Java distributions. Its final Clava symlink operation was obsolete because `installDist` now creates a real `Clava-JS/java-binaries` directory. The remaining TypeScript build was run directly. No second Codex conversation was needed.

Experiments use OpenJDK 26.0.1, Gradle 9.6.1, and the unchanged dumper release selector `v18.1.8_7-rc4`.

Run from this workspace's `clava/ClangAstParser`:

```bash
# Passing regression tests, including deterministic graph tests.
gradle -I ../experiments/cross-tu/tests.init.gradle test \
  --tests 'pt.up.fe.specs.clang.crosstu.*RegressionTest'

# Desired-behavior probes. A nonzero exit is EXPECTED on the starting implementation.
gradle -I ../experiments/cross-tu/tests.init.gradle test \
  --tests 'pt.up.fe.specs.clang.crosstu.*ReviewTest' -PcrossTuReview=true

# One finding only; replace CTU04 with the ID under review.
gradle -I ../experiments/cross-tu/tests.init.gradle test \
  --tests '*ReviewTest.CTU04*' -PcrossTuReview=true

# Entire parser suite, with native resources isolated from the shared /tmp cache.
gradle -I ../experiments/cross-tu/tests.init.gradle test \
  -PcrossTuResults=parser-full-isolated -PcrossTuIsolatedTmp=true
```

Run `npm test` and `npm run build` from `clava/Clava-JS` for the joinpoint/action suite and TypeScript check. The new action tests are in `api/CrossTu.test.ts`.

JUnit XML and HTML are under `ClangAstParser/build/test-results/<run>` and `ClangAstParser/build/reports/tests/<run>`, respectively. Default run names are `cross-tu-regression` and `cross-tu-review`. The experiment init script disables JaCoCo instrumentation and the repository's aggregate coverage threshold for these runs. It does not disable any tests or modify production build settings. Review classes require the explicit system property supplied by `-PcrossTuReview=true`; ordinary test runs skip them until decisions are made.

## What the tests establish

The passing regression suite covers C and C++ external functions, overloads, typedef desugaring, pointer parameters, multiple prototypes, static functions, const methods, methods in different classes and namespaces, inherited methods, constructors/destructors, operators, primary function templates, indirect calls, records, globals, missing definitions, explicit cache invalidation after edits, and code generation followed by reparsing. Separate joinpoint tests exercise lookups after parameter and return-type changes, and static-function renaming. Generated-global tests cover one initializer plus extern declarations and independent application state.

Every source-project scenario runs with serial and parallel parsing and in both TU orders. The parser sorts input paths, so the fixture assigns per-source ordering directories and checks the actual returned TU order. Reversing the argument list alone would not test this. Fixtures also assert that every requested TU survives parsing, preventing vacuous success when the parser ignores failed files.

Graph tests cover direct, optional, and list references; ambiguity; missing targets; invalid locations; macros; initializer semantic/syntactic forms; copy provenance; and six seeded permutations of 1,000 targets and references. They check object identity, list order, and idempotence. These are correctness stress tests, not performance measurements.

Validation at this baseline: all 110 new Java regression cases pass. The entire parser suite passes 319 cases, with the 14 pending-review methods skipped. The entire Clava-JS suite passes 166 cases, with two existing skips; four passing cases are new cross-TU joinpoint/action tests. TypeScript compilation and linting the new TypeScript file pass. The opt-in review run fails all 47 cases as expected, representing the 14 findings below. No failures were converted into assertions that the incorrect behavior should continue.

The first full parser run encountered six CUDA failures because the pre-existing shared native-resource cache was invalid. A complete rerun with `-PcrossTuIsolatedTmp=true` passed, including all six CUDA cases. The shared cache was not deleted or repaired.

There are two independent identity mechanisms:

1. `TUnitProcessor` redirects node-valued fields toward unique AST-tree candidates, using `ClavaNode.getNodeSignature()`.
2. `DeclarationsCache` connects declarations and definitions throughout an application, using function signatures, record names, and global names. `CallExpr.getDefinition()`, function lookup, record lookup, and global lookup consume these maps.

Fixing parser normalization alone will not fix declaration-cache collisions.

Examples below are translation units or headers, not complete executables. An external declaration without its matching definition is intentional in negative tests: lookup must report the missing definition, even if a different entity has a similar name. These tests do not claim that such a project would link successfully.

## CTU01: free functions from different namespaces are connected

Severity: high. Reproduced with native dumps in all four execution modes. Probe: `CTU01_freeFunctionsRespectNamespaces`.

```cpp
// a.cpp
namespace A { int run(); }
int use() { return A::run(); }

// b.cpp
namespace B { int run() { return 2; } }
```

Expected: `A::run` has no implementation in this project. Observed: its `getImplementation()` returns `B::run`. A consumer following that link can inspect or edit an unrelated function.

`FunctionDecl.getSignature()` uses the unqualified name and parameter types. Both functions become `run()`, and `DeclarationsCache` uses that string as its key. Method-specific qualification does not protect free functions.

Proposed behavior: include the complete semantic scope in free-function identity. Preserve typedef/parameter normalization, and distinguish namespace identity from how a name is printed at its current location. Test both missing and present matching definitions, plus namespace aliases, when implementing the fix.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU02: static functions in identically named files are connected

Severity: high. Reproduced with native dumps in all four modes. Probe: `CTU02_staticFunctionsRespectFullTuPaths`.

```cpp
// left/unit.cpp
static int helper() { return 1; }
int left() { return helper(); }

// right/unit.cpp
static int helper() { return 2; }
int right() { return helper(); }
```

Expected: each call resolves to the definition in its own translation unit. Observed: both definitions have the key `helper()[unit.cpp]`; one TU's call resolves to the other TU's definition. Reversing the actual TU order changes which definition enters the cache first.

The static-function discriminator uses `TranslationUnit.getFilename()`, which loses the directory. Static functions in differently named files pass the control test.

Proposed behavior: use full TU identity for internal-linkage free functions. Do not apply this rule merely because a member function is declared `static`; static members have different linkage semantics. Preserve the passing static-member control test.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU03: an anonymous-namespace project loses its translation units during parsing

Severity: high. Reproduced through the parser in all four modes. Probe: `CTU03_anonymousNamespaceProjectRemainsParseable`. Cross-TU linkage semantics remain untested for this example because parsing fails first.

```cpp
// a.cpp
namespace { int helper() { return 1; } }
int a() { return helper(); }

// b.cpp
namespace { int helper() { return 2; } }
int b() { return helper(); }
```

Expected: two translation units, each containing a TU-local helper and a call to it. Observed: the parser logs an unresolved top-level node ID, such as `Could not find ClavaNode with id '@1_1'`, then returns an application with zero TUs. The fixture's TU-count assertion detects the loss.

The failure reaches `ClangAstParser.parseTu()` while resolving `TOP_LEVEL_DECL_IDS` against the decoded node registry. `ParallelCodeParser` catches the per-file exception and ignores the file. The exact producer/reader mismatch has not been localized; the symptom does not establish which side omitted the node. The C++ source is valid, independently checked with the host compiler.

Proposed behavior: first make this project survive the existing transport and parser path. Then require anonymous-namespace functions to remain TU-local. Also decide whether silently dropping every input after internal parser failures should be permitted. Fixing native traversal or wire production would expand this branch's implementation scope and should be reviewed separately.

Decision: [ ] Approve investigation/fix of the parsing failure. [ ] Dismiss. Notes:

## CTU04: lvalue and rvalue method overloads share a definition

Severity: high. Reproduced with native dumps in all four modes. Probe: `CTU04_refQualifiedMethodsStaySeparate`.

```cpp
// api.hpp
struct R { int get() &; int get() &&; };

// use.cpp
#include "api.hpp"
int use(R &r) { return r.get() + static_cast<R&&>(r).get(); }

// impl.cpp
#include "api.hpp"
int R::get() & { return 1; }
int R::get() && { return 2; }
```

Expected: the two calls resolve to different definitions. Observed: both `CallExpr.getDefinition()` results are the same object. The lvalue/rvalue distinction is lost by the cache key, despite being retained in the parsed function type.

`CXXMethodDecl.getSignature()` adds class/namespace information and const qualification, but not the member-function ref qualifier. The first colliding implementation wins.

Proposed behavior: include the ref qualifier in method identity. Preserve an explicit distinction between no ref qualifier, `&`, and `&&`. The fix must not infer it from a call's value category or generated source text.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU05: volatile and non-volatile method overloads share a definition

Severity: high. Reproduced with native dumps in all four modes. Probe: `CTU05_volatileQualifiedMethodsStaySeparate`.

```cpp
// api.hpp
struct R { int get(); int get() volatile; };

// use.cpp
#include "api.hpp"
int use(R &r, volatile R &v) { return r.get() + v.get(); }

// impl.cpp
#include "api.hpp"
int R::get() { return 1; }
int R::get() volatile { return 2; }
```

Expected: two different definition objects. Observed: both calls resolve to one definition. Transformations using the lookup can select the wrong overload.

`CXXMethodDecl.getSignature()` handles const but omits volatile qualification. Const/non-const overloads pass the control test; the omission is specific to incomplete method identity.

Proposed behavior: include all overload-relevant member-function cv/ref qualifiers as structured fields. Treat this independently from qualifiers on return types and parameter types.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU06: variadic and fixed-arity overloads are connected

Severity: high. Reproduced with native dumps in all four modes. Probe: `CTU06_variadicOverloadsStaySeparate`.

```cpp
// a.cpp
int choose(int);
int use() { return choose(1); }

// b.cpp
int choose(int, ...) { return 2; }
```

Expected: the fixed-arity `choose(int)` has no implementation in this project. Observed: its implementation is the different variadic function from `b.cpp`.

`FunctionDecl.getSignature()` enumerates declared parameters but omits the function type's variadic flag. Both functions therefore produce the same cache key.

Proposed behavior: include variadicness in function identity. Keep this separate from default arguments, which do not create a distinct overload identity.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU07: records from different namespaces are connected

Severity: high. Reproduced with native dumps in all four modes. Probe: `CTU07_recordsRespectNamespaces`.

```cpp
// a.cpp
namespace A { struct Item; }
A::Item *p;

// b.cpp
namespace B { struct Item { int value; }; }
```

Expected: `A::Item` remains incomplete. Observed: `A::Item.getDefinition()` returns the complete `B::Item`. This can expose fields or bases belonging to a different type.

`DeclarationsCache.getTagKey()` returns only the unqualified declaration name, `Item`. The complete definition in `B` occupies the key queried by `A`.

Proposed behavior: give tag lookup a scoped identity. Include enclosing records and TU identity where required by linkage. Template specialization identity needs a separate audit before treating name qualification as a complete solution.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU08: global variables from different namespaces are connected

Severity: high. Reproduced with native dumps in all four modes. Probe: `CTU08_globalsRespectNamespaces`.

```cpp
// a.cpp
namespace A { extern int value; }
int use() { return A::value; }

// b.cpp
namespace B { int value = 2; }
```

Expected: `A::value.getDefinition()` returns null, because no definition of that variable was provided. Observed: it returns `B::value`.

`DeclarationsCache.getVarDeclKey()` uses only `DECL_NAME`, ignoring namespace scope. Ordinary external globals in the global namespace pass their control test.

Proposed behavior: include semantic scope and linkage in variable identity. A variable's spelling or type alone cannot determine whether two declarations denote the same entity.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU10: an initialized extern variable is not recognized as a definition

Severity: medium. Reproduced with native dumps in all four modes. Probe: `CTU10_externWithInitializerIsADefinition`.

```cpp
// a.cpp
extern int value;
int use() { return value; }

// b.cpp
extern int value = 2;
```

Expected: the declaration in `a.cpp` resolves to the initialized definition in `b.cpp`. Observed: lookup returns null. `extern` with an initializer is a definition, although the compiler may warn about that spelling.

`DeclarationsCache.processVarDecl()` treats any storage class other than `None` as not being a definition. It therefore excludes the initialized `Extern` declaration. `VarDecl.getDefinition()` also sends an initialized extern declaration through the external-lookup path.

Proposed behavior: classify definitions using declaration semantics, including an initializer, rather than storage class alone. Include C tentative definitions, thread-local variables, and static data members in the later classification audit; this reproduction establishes only the initialized-extern case.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU11: an internal const variable supplies an unrelated external definition

Severity: high. Reproduced with native dumps in all four modes. Probe: `CTU11_internalConstDoesNotDefineAnExternalGlobal`.

```cpp
// a.cpp
extern const int value;
int use() { return value; }

// b.cpp
const int value = 2;
```

Expected: the external `value` in `a.cpp` remains unresolved. The namespace-scope const definition in `b.cpp` has internal linkage in this C++ example. Observed: lookup returns that internal variable as the external definition.

The variable cache uses the bare name and a storage-class check. Internal linkage is not limited to the `static` storage-class spelling, so that check cannot protect this case. This example is deliberately C++; C has different rules for namespace/file-scope const objects.

Proposed behavior: use actual linkage information and distinguish TU-local variables from external entities. Do not implement a language-independent rule that all const globals are internal.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU13: normalization confuses distinct files with the same basename

Severity: high. Deterministically reproduced in a constructed parser graph. Probe: `CTU13_normalizationDistinguishesFullSourcePaths`. A complete native-source reproduction has not been established.

Consider these distinct headers, each defining its own TU-local variable:

```cpp
// left/common.hpp, line 1
static int value;

// right/common.hpp, line 1
static int value;
```

The graph reproduction contains a tree candidate for the left variable and a detached reference target for the right variable. Both have matching node kinds, names, storage classes, types, and coordinates. A holder's node-valued field initially points to the right variable.

Expected: that field stays attached to the right variable because no matching tree candidate exists. Observed: `TUnitProcessor` redirects it to the left variable.

`SourceLocation.toString()` keeps only the basename, and normalization embeds that display string in the signature. Both source ranges become the same `common.hpp:line:column` text. If both definitions happen to be indexed, ambiguity handling may prevent a redirect; the tested graph has only one indexed candidate, which exposes the unsafe match.

Proposed behavior: key source identity by full normalized paths or explicit source-file IDs. Keep the short basename representation for diagnostics. This is a matching-rule change and should have explicit tests for shared headers and genuinely distinct files.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU14: rewriting a reference invalidates a previously indexed signature

Severity: medium. Deterministically reproduced with real `VarDecl` and `DeclRefExpr` objects in a constructed parser graph. Probe: `CTU14_rewritingAReferenceDoesNotInvalidateItsLookupKey`. The frequency in native projects is not measured.

Example graph, where `D0` and `D1` represent the same declaration location, and `R0` and `R1` represent the same reference location:

```text
D0: declaration in the AST tree
D1: detached declaration copy with a different parser ID
R0: reference in the AST tree, DECL -> D1
R1: detached reference copy,     DECL -> D1
owner.DIRECT -> R1
```

Initially, `R0` and `R1` have equal signatures. Indexing stores the signature for `R0`, including the ID of `D1`. The first node registry then rewrites `R1.DECL` to `D0`. The next registry tries to normalize `owner.DIRECT`, which still points to `R1`.

Expected: the owner's reference resolves to tree node `R0`. Observed: `R1` now has a different signature because its `DECL` field contains `D0`'s ID, so lookup misses the key indexed earlier. The owner keeps the detached reference. The test explicitly controls the registry order rather than relying on hash-map iteration.

`TUnitProcessor` indexes keys before rewriting fields, then recomputes keys against mutable objects while rewriting. A node-valued signature component can therefore change during the pass.

Proposed behavior: define one consistent identity snapshot for indexing and all lookups, or resolve references in a phase that cannot change the keys used by later lookups. A lazy cache alone is insufficient if a node's first key calculation occurs after it was mutated. Do not add a permanent AST-node cache without an invalidation contract.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## CTU15: ambiguity handling eagerly renders full node data

Severity: performance/maintainability concern. Deterministically reproduced by instrumenting node content rendering. Probe: `CTU15_ambiguityHandlingDoesNotEagerlyRenderFullNodeData`. No speedup or share of total parse time is claimed.

Example: two parsed occurrences produce the same signature. `addNode()` removes that signature from the candidate map and records it as ambiguous. While doing so, it concatenates the old node and appends the new node into `collisionReport`.

Expected proposed behavior: retain a bounded, compact collision record and render full node data only when diagnostic output is requested. Observed: an instrumented collision invokes full `toContentString()` twice even when no caller requests a report. The report string is retained for the processor's lifetime.

This finding is narrower than the original discussion's subtree-printing explanation. In this checkout, `ClavaNode.toString()` formats its DataStore contents. Its formatter uses short strings for referenced nodes; it does not recursively print the child tree. Likewise, node-valued signature fields already use `StringProvider.getString()`.

Proposed behavior: make detailed diagnostics lazy and bounded while preserving ambiguity rejection. Retain counts and a small sample of source identities if those are useful. Verify the change with allocation profiling before attributing a particular fraction of issue #270 to it.

Decision: [ ] Approve the diagnostic change. [ ] Dismiss. Notes:

## CTU16: generated extern globals do not resolve like parsed extern globals

Severity: medium. Reproduced by parsing a two-TU application and then using its real `GlobalManager` API, in all four modes. Probe: `CTU16_generatedGlobalReferencesFindTheirDefinition`.

Start with these valid source files:

```cpp
// a.cpp
int a() { return 1; }

// b.cpp
int b() { return 2; }
```

Then request a shared global in each TU through the API used by the file joinpoint's `addGlobal` action:

```text
definition  = app.globalManager.addGlobal(firstTU,  "shared", intType, integerLiteral(7))
declaration = app.globalManager.addGlobal(secondTU, "shared", intType, integerLiteral(7))
app.clearCache()
declaration.getDefinition()
```

The manager emits the equivalent of `int shared = 7;` in the first TU and `extern int shared;` in the second.

Expected: the extern declaration's definition is the initialized variable in the first TU. Observed: `getDefinition()` returns the extern declaration itself. Clearing the application cache does not correct it.

`GlobalManager.addGlobal()` creates variables through the factory but does not mark them with `HAS_GLOBAL_STORAGE`. `VarDecl.getDefinition()` treats a variable without that flag as local and immediately returns it. `DeclarationsCache` also excludes such nodes from its global-variable index.

Proposed behavior: generated global declarations must carry the same required storage/linkage metadata as parsed globals. Audit the creation API, rather than teaching every lookup consumer to guess global storage from ancestry. Preserve the passing controls for one initializer, later extern declarations, independent names, and independent applications.

Decision: [ ] Approve finding and desired behavior. [ ] Dismiss. Notes:

## Suspicions that did not reproduce

CTU09, block-local static variables polluting external-global lookup, did not reproduce. For `a.cpp: extern int value; int use(){return value;}` and `b.cpp: int other(){static int value=2;return value;}`, the external declaration correctly remains unresolved. The static variable has global storage duration, but the existing storage-class filter excludes it. This is now a passing regression test in all four modes.

CTU12, static member declarations failing to find out-of-line definitions, did not reproduce. For a shared header `struct R{static int get();};`, a use TU calling `R::get()`, and an implementation TU defining `int R::get(){return 1;}`, lookup reaches the implementation. This is also a passing regression test in all four modes. The reason is not inferred from the `static` spelling; the observed parsed behavior is what the test protects.

The anonymous-namespace case is not dismissed: an earlier version of its test passed vacuously after the parser dropped all input files. The explicit TU-count guard exposed CTU03.

## Limits and next steps

The test suite is a baseline for refactoring, not proof of full C++ entity identity. Explicit function specializations, class-template specialization identity, constrained overloads, mixed compile configurations, virtual runtime dispatch, symlink/case-sensitive source identity, modules, full cache invalidation for every editing action, and mutation of shared header occurrences still need dedicated investigations. Primary function templates, static call lookup for inherited methods, and selected edit actions are covered; they do not establish those broader contracts.

No behavior-changing fixes were applied. Review CTU01, CTU02, CTU04-08, CTU10-11, and CTU16 as declaration/definition-identity work; CTU13-14 as parser-normalization work; CTU15 as a diagnostic cost change; and CTU03 as a parser/transport investigation. These groups can be approved independently.

Validation results and any environment failures are recorded in `experiments/cross-tu/results.json`. Raw JUnit reports remain under `build`, so they can be regenerated without committing temporary paths and large diagnostic dumps.
