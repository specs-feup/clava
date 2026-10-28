# Compiler-backed cross-TU entities

This experiment uses an unpublished local clang-dumper build. No release tag or asset is published, and `clang-dumper-release.tag` remains unchanged. The release selected by that file does not implement this new contract. Use the local-build property below; an older schema fails explicitly rather than silently reverting parsed declarations to name-based matching.

## Behavior decisions

The first implementation mixed technical limits with behavior choices that had not been agreed. The following
requirements now supersede the corresponding experimental restrictions. The runtime changes remain to be implemented.

1. **Repeated header definitions: pending.** The current implementation reports multiple occurrences as
   `AMBIGUOUS`, even for ordinary inline functions. The proposed behavior is to return the querying TU's
   compiler-selected occurrence and expose project conflicts separately. This choice remains open.
2. **Shared-source edits: agreed.** One action must coordinate all occurrences of the physical source and their
   affected uses, including distinct TU-local entities originating in a shared header. The user should not have to
   repair callers or repeat the action per TU. If safe coordination requires compiler refresh, report that need
   explicitly instead of leaving a partially updated program.
3. **Generated-to-parsed matching: agreed.** Preserve matching from a newly generated declaration to an existing
   compatible parsed definition without reparsing when the match is unambiguous. A separate generated identity
   must not disable this workflow. Ambiguous or unsupported relationships must remain explicit.

Two other items are unfinished implementation work, not decisions the user should have to make:

- Preserve integral template argument type/width in the transport and generated source. The existing loss can
  change `template<auto>` code on a round trip even though USRs distinguish the parsed entities.
- Replace the remaining Java compatibility/mutation model as stronger compiler facts become available. Its
  supported subset must remain explicit; a missing implementation is not a new language rule.

The local-only build, compiler identity workarounds, cache namespace, and explicit decoder errors are implementation
facts. USRs also cannot provide final executable membership/linker selection or rerun C++ semantic analysis after
arbitrary edits. Those facts do not justify silently restricting ordinary lookup or supported transformations.

## Build and run

From the workspace root, with LLVM 18 development libraries installed:

```sh
cmake -S clang-dumper -B clang-dumper/build -DCMAKE_BUILD_TYPE=Release
cmake --build clang-dumper/build --target tool plugin verify_flatbuffers --parallel 4

gradle -p clava/ClangAstParser \
  -I ../experiments/cross-tu/tests.init.gradle \
  -PclangDumperBuild=../../clang-dumper/build \
  -PcrossTuResults=usr-regression test \
  --tests 'pt.up.fe.specs.clang.crosstu.*RegressionTest'
```

`clangDumperBuild` resolves relative to the ClangAstParser project. It supplies both the binary schema and the runtime dumper selection, so the two cannot accidentally select different builds. It also works for the ClavaWeaver included build:

```sh
gradle -p clava/ClavaWeaver -PclangDumperBuild=../../clang-dumper/build installDist
```

The local executable location is embedded in generated resources, not committed as an absolute machine-specific path. After rebuilding the native executable, rebuild the Java distribution so its schema resources match. The existing local loader forces system includes; this is not a local release of the bundled include packages.

## Identities and ownership

Every named declaration transports the raw Clang USR, explicit generation success/failure, TU-local status, C-language-linkage status, compiler linkage name where available, a canonical type USR where supported, and declaration/definition/tentative-definition status. Functions additionally identify written explicit template specializations. Unsupported type USR generation is explicit too.

The producer generates this metadata during its existing parse and dump. Each output is independent of other selected TUs. Implicit top-level declarations that the traversal does not emit are excluded from the top-level ID list, fixing the anonymous-namespace dangling-reference defect locally. Clava retains ordinary wire node IDs, scoped by its reader per TU. It does not replace native declaration targets using source-location signatures. The existing occurrence normalizer remains only for metadata-free synthetic/legacy in-memory graphs; native parser graphs bypass it entirely. Consumer call matching uses entity identity rather than comparing AST objects.

A project index combines entity occurrences from editable TU trees and retained included-file declarations. A declaration and definition may share an entity identity while remaining distinct mutable objects. Header definitions retain the TU in which Clang analyzed them. Internal entities include full TU provenance in the key, even if the raw USR contains only a shared header name.

A failed TU decode aborts parsing by default instead of silently producing an incomplete analysis group. `CONTINUE_ON_PARSING_ERRORS` explicitly permits partial analysis and records omitted files in `App.IGNORED_FILES`. Callers using that option must interpret missing definitions relative to the surviving TUs.

The analysis group is the App's current set of TUs. Removing a TU and clearing the index removes its included declarations too. This index does not discover executable membership or reproduce archive selection, weak-symbol selection, or final linker resolution. A compilation database alone does not provide those decisions.

## Header AST occurrences and output files

When `a.cpp` and `b.cpp` include `api.hpp`, Clang parses the header in both compilation contexts. An inline
function body is a normal `FunctionDecl` with a body in each context; Clang's source AST does not replace each
call with that body. The occurrences can differ after preprocessing and name lookup.

Clava's editable file tree contains declarations belonging to that file. Included header declarations remain
available through native declaration references and `TranslationUnit.INCLUDED_DECLARATIONS`; they are not
children to emit into each including `.cpp`. `TranslationUnit.getCode()` emits the file's children and includes,
and `App.getCode(destination)` emits the selected file trees. Retaining another analysis occurrence does not
create another output definition.

To regenerate a header from its AST, the header needs its own selected editable file tree. Merely including it
from a parsed source file does not make Clava emit a modified header automatically. Coordinated header editing
must therefore connect analysis occurrences to the owning output file as well as update callers. This is part of
the agreed shared-source edit requirement, not something a definition lookup result alone can accomplish.

The intended source layout keeps a header-defined inline function in its header and keeps includes/calls in the
source files. The later compiler chooses machine-code inlining; C++ `inline` does not guarantee substitution.
See the [C++ inline rules](https://eel.is/c++draft/dcl.inline).

## LLVM 18 identity safeguards

Raw USRs are useful compiler data, but not an infallible equivalence oracle. Native-source regressions establish three relevant LLVM 18 behaviors:

- Function USRs can distinguish top-level parameter cv even when the declarations denote the same function. The producer normalizes parameter types through Clang's signature-parameter API for the compatibility type USR. Non-template function lookup uses a compiler-linkage-name alias as its index key.
- Function USRs omit a noexcept distinction inside function-pointer overload parameters. The compiler linkage name disambiguates those overloads.
- Namespace spelling can remain in C-language-linkage USRs across TUs. C-linkage functions and variables therefore use the compiler's linkage name within the appropriate entity family and TU domain.

No USR text is parsed, and failed generation never falls back to a source name or location. Template entities use their compiler USRs; the non-template linkage-alias rule is not applied to primary templates or specializations. Compatibility checks reject inconsistent function signatures and variable types. The prior conservative Java type model remains for compatibility, mutation detection, and factory-created declarations; it no longer establishes identity between parsed entities.

## Lookup results

Function, tag, and global-variable resolution exposes immutable candidate lists and these states:

- `UNIQUE`: exactly one supported compatible definition occurrence.
- `MISSING`: no definition in the selected analysis group.
- `AMBIGUOUS`: multiple definition occurrences; all candidates remain available.
- `UNSUPPORTED`: compiler entity identity cannot be established.
- `INCOMPATIBLE`: an entity bucket contains definitions incompatible with the query.
- `STALE`: identity-relevant edits invalidated compiler metadata.

Optional lookup adapters return a value only for `UNIQUE`. A parsed function with a body cannot conceal ambiguity or stale metadata by returning itself as a fallback. Same-location header definitions remain separate and ambiguous, including apparently identical inline definitions. Establishing safe equivalence across preprocessing configurations is future work; equal USRs and source coordinates do not prove the ODR. This is the current implementation's unresolved lookup policy, not an agreed product requirement; see the pending decision above.

Repeated C tentative definitions within one TU can yield to that TU's full definition. Tentative definitions across TUs remain ambiguous; this is not an implementation of platform common-symbol/linker policies.

Written explicit function specializations survive the source-tree cleanup pass and use their own compiler entity identity. Implicit instantiations continue exposing the source primary template through the existing transformation-facing adapter. The specialization flag also preserves explicit template syntax for supported code generation. Older opt-in CTU03/18/19/20 research probes describe the pre-USR implementation; the ordinary `Usr*RegressionTest` cases replace them as the acceptance contract for this experiment. Existing transport limitations still apply, including loss of integral template argument width/type in generated source for some `template<auto>` cases. Compiler USRs distinguish these entities for lookup, but cannot restore source data the transport omitted.

## Editing contract

The parser snapshots identity-relevant state before returning its graphs. The index checks query/candidate state before answering; direct name, signature, storage, or type changes fail closed, including when a cache generation already exists. `App.clearCache()` rebuilds Java indexes but does not rerun Clang or bless an edited compiler USR. Clearing the cache cannot make stale facts current.

Supported entity-wide function edits maintain declaration relationships through a separate transformation-owned identity. Rename, parameter, parameter-type, and return-type operations first collect all current entity occurrences, update them, assign an explicit edit identity, and invalidate the index. Original USRs and compiler snapshots remain historical. A later raw mutation still reports `STALE`. These operations can rename all occurrences of a conflicting entity without choosing one definition; definition lookup remains `AMBIGUOUS`. Unsupported, incompatible, or stale relationships cannot be silently adopted by an edit.

The joinpoint rename adapter also updates record/global occurrences together. Clones are new generated entities and discard the original compiler identity. Cloning inserts prototypes only where an editable parent exists; retained header declarations are not insertion points. Adding a method to a different owning class also creates generated identity.

Factory-created declarations use a separate identity domain. They cannot acquire a parsed entity identity by matching its spelling. Explicit binding of independently generated declarations to existing parsed entities remains future work. Arbitrary edits still do not rerun overload resolution, template instantiation, or name lookup. The mutation guard uses the existing supported type model, not a complete C++ semantic invalidation engine; direct mutation outside that model is not a compiler-validity guarantee. A physical shared-header edit can affect several distinct TU-local entities. Entity-wide editing does not itself synchronize those separate entities or refresh all dependent expressions.

## Validation cost

Unchanged lookups reuse captured semantic baselines. Local node mutation stamps cover normal setters and tree
operations; separate observations cover exposed mutable lists, exception specifications, and TU language data.
A changed dependency triggers the full semantic comparison. Equivalent edits refresh the observations; changes
to identity-relevant state still produce `STALE`. Shared-data views share their mutation counter, while ordinary
copies retain independent counters. Tracking starts when a node is observed, so unobserved statement trees and
parser construction do not allocate counters.

The parser captures compiler declarations as a read-only batch, sharing immutable type baselines and dependency
observations across occurrences. This sharing ends with capture; validation after an edit collects fresh state.
The index caches compiler keys and sorted definition candidates, checks candidate state before reuse, and leaves
factory-created declaration keys dynamic. None of these caches stores a cross-TU definition in a native dump.

## Caching and tests

Schema changes select a new schema-hash cache namespace. Reader node-ID scopes are assigned after loading a dump; a TU's numeric position in the App is no longer sent as a changing native argument. Adding or reordering another TU therefore does not invalidate an unchanged dump solely because its position changed.

The tests use actual source projects, both TU orders and serial/parallel parsing. They assert every requested TU survives parsing. New coverage includes raw USR transport, exact reference ownership, included declarations, all conflict candidates, failed metadata, stale edits, C linkage, noexcept overloads, top-level cv, typed integral template identities, explicit specializations, compiler-checked code generation/reparsing, tentative globals, and removal of TU-owned definitions.

The ccache tests check real hit counters and resolved results for warm dumps, mixed cached/fresh dumps, changed shared headers, compile-flag changes, TU addition/removal, and relocation of TU-local entities. Warm-lookup regressions also exercise nested type edits, in-place parameter/qualifier lists, mutable exception/language data, raw child lists, shared views, independent copies, equivalent edits followed by real changes, metadata failure, and changed TU provenance. They do not reset shared cache statistics or delete shared installations. Native corpus validation runs the standalone and plugin producers against the same wire verifier.

## Validation of this implementation

The native implementation is committed locally as `b29bf01` in clang-dumper. The standalone and plugin corpus
runs each pass 153 cases, with 7 platform/requirement skips. These native counts come from the initial USR
implementation; the Java-only optimization does not change the producer.

Clava's optimized full parser run passes 645 tests, with 7 existing conditional/review skips. This includes 16
additional warm-lookup validation cases. The ClavaWeaver distribution builds against the local schema, and
TypeScript compilation passes.

The JavaScript integration run passes 165 tests with 3 skips when selecting
`--testNamePattern='^(?!.*OmpThreadsExplore)'`. Two skips already exist; the third explicitly excludes the
OpenMP fixture because this local system has no compatible `omp.h`. The unrestricted run exposes that missing
header. These runs use the experiment init script, which disables JaCoCo instrumentation/aggregate coverage
checks and isolates native resources for the full parser run. The existing shared CUDA cache is left untouched.
