# Compiler-backed cross-TU entities

This experiment uses an unpublished local clang-dumper build. No release tag or asset is published, and `clang-dumper-release.tag` remains unchanged. The release selected by that file does not implement this new contract. Use the local-build property below; an older schema fails explicitly rather than silently reverting parsed declarations to name-based matching.

## Behavior decisions

The physical source declaration is the source-level truth. Compiler analysis occurrences are retained as internal
context, not exposed as duplicate source definitions or emitted into every including file.

1. **Header definition lookup.** Multiple supported occurrences of the same declaration range in the same
   canonical source file resolve to one header definition. An explicitly selected editable header supplies the
   representative when available; otherwise a stable retained header occurrence represents it.
   `DefinitionResolution.contextualDefinitions()` exposes all compiler views separately. Different physical
   definitions remain separate candidates, even when their source text or basenames match.
2. **Shared-source edits.** One action coordinates physical-source peers, related declarations, and affected uses.
   This includes distinct TU-local entities originating in a shared header. Their runtime identities remain
   distinct. Source-level `getCalls()` includes header callers without repeating the same source call per TU.
   A source action either commits all its changes or raises `RefreshRequiredException` and restores the graph.
3. **Generated-to-parsed matching.** Compatible generated function declarations can find parsed definitions
   without reparsing when the structural bridge establishes the relationship. Generated nodes do not acquire
   fabricated USRs. Unsupported type identity does not bridge; distinct definition sources remain ambiguous.

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

An explicitly selected header is generated through its file tree. A changed included-only header is emitted once
by `App.getCode(destination)`/`App.write(destination)`. The source coordinator stages token-aligned changes to
its original text, retaining surrounding declarations, comments, preprocessing and macro spelling. The including
source files retain their includes and calls. Leading preprocessing in including files is preserved when needed
for the edited header; unsupported conditional or token mappings require refresh instead of choosing a TU's
expanded body. Output paths follow configured source-folder mappings, with the common source root used for otherwise flattened
inputs. Conflicting output mappings and collisions are errors.

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

- `UNIQUE`: exactly one supported compatible physical source definition.
- `MISSING`: no definition in the selected analysis group.
- `AMBIGUOUS`: multiple physical source definitions; all candidates remain available.
- `UNSUPPORTED`: compiler entity identity cannot be established.
- `INCOMPATIBLE`: an entity bucket contains definitions incompatible with the query.
- `STALE`: identity-relevant edits invalidated compiler metadata.

Optional lookup adapters return a value only for `UNIQUE`. A parsed function with a body cannot conceal a
conflict between distinct source definitions or stale metadata by returning itself as a fallback. Repeated header
analysis occurrences share one source candidate. This is source ownership, not an ODR-equivalence proof:
macro-dependent bodies, types and bindings remain available through `contextualDefinitions()`. A stale context
cannot be hidden by selecting another representative. Macro-generated declarations without a unique source
owner are not merged by guessed locations.

Repeated C tentative definitions within one TU can yield to that TU's full definition. Tentative definitions across TUs remain ambiguous; this is not an implementation of platform common-symbol/linker policies.

Written explicit function specializations survive the source-tree cleanup pass and use their own compiler entity identity. Implicit instantiations continue exposing the source primary template through the existing transformation-facing adapter. The specialization flag also preserves explicit template syntax for supported code generation. Older opt-in CTU03/18/19/20 research probes describe the pre-USR implementation; the ordinary `Usr*RegressionTest` cases replace them as the acceptance contract for this experiment. Existing transport limitations still apply, including loss of integral template argument width/type in generated source for some `template<auto>` cases. Compiler USRs distinguish these entities for lookup, but cannot restore source data the transport omitted.

## Editing contract

The parser snapshots identity-relevant state before returning its graphs. The index checks query/candidate state before answering; direct name, signature, storage, or type changes fail closed, including when a cache generation already exists. `App.clearCache()` rebuilds Java indexes but does not rerun Clang or bless an edited compiler USR. Clearing the cache cannot make stale facts current.

Supported function rename, parameter, parameter-type, return-type and body actions collect a closure over both
compiler entities and physical source ownership. Edits assign transformation-owned identities per semantic entity,
so distinct static-header entities remain distinct while their shared source is updated together. Original USRs
remain historical; a later raw identity mutation still reports `STALE`.

Parameter replacement rebinds body references to the new parameter objects. Parameter-type changes copy the
signature list rather than mutating another declaration's shared type. Method renames update stored member
reference names as well as declaration names. Parameter and caller changes may be staged in the order existing Clava transformations use. Source emission
checks that edited signatures have compatible caller arities after those steps. Removing a parameter still used
in a body requires an explicit body update; an unsafe source action restores its original graph. The modified-file output filter includes files changed indirectly by these coordinated actions.

The joinpoint rename adapter also updates record/global occurrences together. Clones are new generated entities
and discard the original compiler identity. A generated function declaration may bridge to a compatible parsed
entity; unsupported structural types cannot establish a bridge. Parsed generation failures never use this fallback.

The source transaction checks original files against their parse-time snapshots. If a source changed on disk,
contexts require incompatible source edits, source owners overlap, or token/preprocessing mappings cannot be
preserved, it raises `RefreshRequiredException`. It restores original declaration data, child objects, parameter
references and member names before returning the failure. No partial header output is staged.

These are coordinated source transformations. They do not rerun compiler overload resolution, template
instantiation or arbitrary expression semantics. That boundary remains explicit rather than silently replacing
compiler facts with name matching.

## Validation cost

Unchanged lookups reuse captured semantic baselines. Local node mutation stamps cover normal setters and tree
operations; separate observations cover exposed mutable lists, exception specifications, and TU language data.
A changed dependency triggers the full semantic comparison. Equivalent edits refresh the observations; changes
to identity-relevant state still produce `STALE`. Shared-data views share their mutation counter, while ordinary
copies retain independent counters. Tracking starts when a node is observed, so unobserved statement trees and
parser construction do not allocate counters.

The parser captures compiler declarations as a read-only batch, sharing immutable type baselines and dependency
observations across occurrences. This sharing ends with capture; validation after an edit collects fresh state.
The index caches compiler keys, physical source ownership and sorted definition candidates, checks candidate
state before reuse, and leaves factory-created declaration keys dynamic. Physical paths are canonicalized once
per capture/index pass; structural bridging is built lazily when generated functions need it. None of these caches stores a cross-TU definition in a native dump.

## Caching and tests

Schema changes select a new schema-hash cache namespace. Reader node-ID scopes are assigned after loading a dump; a TU's numeric position in the App is no longer sent as a changing native argument. Adding or reordering another TU therefore does not invalidate an unchanged dump solely because its position changed.

The tests use actual source projects, both TU orders and serial/parallel parsing. They assert every requested TU survives parsing. New coverage includes raw USR transport, exact reference ownership, included declarations, all conflict candidates, failed metadata, stale edits, C linkage, noexcept overloads, top-level cv, typed integral template identities, explicit specializations, compiler-checked code generation/reparsing, tentative globals, and removal of TU-owned definitions. Source-ownership coverage checks inline/static headers, macro-dependent
contexts, template instantiations, selected and retained-only headers, dependent member callers, staged caller
updates, generated declarations, rollback, disk changes, and configured output paths.

The ccache tests check real hit counters and resolved results for warm dumps, mixed cached/fresh dumps, changed shared headers, compile-flag changes, TU addition/removal, and relocation of TU-local entities. Warm-lookup regressions also exercise nested type edits, in-place parameter/qualifier lists, mutable exception/language data, raw child lists, shared views, independent copies, equivalent edits followed by real changes, metadata failure, and changed TU provenance. They do not reset shared cache statistics or delete shared installations. Native corpus validation runs the standalone and plugin producers against the same wire verifier.

## Validation of this implementation

The native implementation is committed locally as `b29bf01` in clang-dumper. The standalone and plugin corpus
runs each pass 153 cases, with 7 platform/requirement skips. These native counts come from the initial USR
implementation; the Java-only optimization does not change the producer.

Clava's full parser run passes 714 tests, with 7 existing conditional/review skips. This includes all 65 new
source-ownership cases, covering both TU orders and serial/parallel parsing. The output regressions compile
generated source files with an independent C++ compiler. The ClavaWeaver distribution builds against the local
schema, and TypeScript compilation passes.

The JavaScript integration run passes 168 tests with 3 skips when selecting
`--testNamePattern='^(?!.*OmpThreadsExplore)'`. Two skips already exist; the third explicitly excludes the
OpenMP fixture because this local system has no compatible `omp.h`. The unrestricted run exposes that missing
header. These runs use the experiment init script, which disables JaCoCo instrumentation/aggregate coverage
checks and isolates native resources for the full parser run. The existing shared CUDA cache is left untouched.
