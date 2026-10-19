# Cross-TU identity design review

This is the independent pre-implementation design snapshot. The resulting implementation and its verified boundaries are documented in `docs/cross-tu-implementation.md`; additional confirmed limitations are in `docs/cross-tu-followups.md`.

This review examines the current Clava implementation and the CTU findings in `docs/cross-tu-review.md`. It does not change production code or tests. Its purpose is to identify a design that can fix the confirmed cases without turning identity rules into a collection of unrelated string patches.

## Recommendation

Use one identity subsystem with **two separate identity domains**:

1. **Declaration identity** answers whether declarations from different translation units denote the same C or C++ entity. It supports declaration-to-definition lookup.
2. **AST occurrence identity** answers which node in a parsed tree a data-store reference belongs to. It resolves detached or copied references during parsing.

Do not use one universal signature for both jobs. A source location can help identify an AST occurrence, but it does not establish that two declarations are the same language entity. Conversely, an entity key can connect declarations across TUs while their AST occurrences must remain distinct.

The maintainable implementation is a small set of immutable, typed key records plus an index that retains all candidates and reports `Missing`, `Unique`, or `Ambiguous/Conflicting`. Render readable strings only for logs. Unknown language cases should not produce a speculative match.

For AST normalization, prefer resolving parser references within their originating TU using that parse's node IDs or an immutable per-TU occurrence index. Preserve each TU's AST occurrences. Shared source coordinates alone are not enough to merge nodes: the same header can produce different semantic ASTs under different macro definitions, compiler options, or include contexts. Cross-TU declaration relationships belong in the declaration index.

## Source evidence and available metadata

`DeclarationsCache` currently has separate maps, but all keys are strings: `FunctionDecl.getSignature()`, `TagDecl.getDeclName()`, or a variable's bare `DECL_NAME`. The function signature uses the unqualified name and printed, desugared parameter types. `CXXMethodDecl` adds a namespace/record spelling and const, but not volatile or the ref qualifier. The maps retain only one implementation per key; a duplicate logs a message and returns the first entry.

The data model already exposes several useful compiler facts:

| Fact needed | Present in this checkout | Remaining issue |
|---|---|---|
| Formal linkage | `NamedDecl.LINKAGE` includes `None`, `Internal`, `UniqueExternal`, `VisibleNone`, `Module`, and `External`; the dumper emits `getFormalLinkage()` | `Module` and `VisibleNone` have no demonstrated cross-TU resolution policy; internal identities need TU scope |
| Declared name and printed scope | `DECL_NAME` and `QUALIFIED_PREFIX`; the dumper emits Clang's qualified prefix | A display string is not a canonical scope identity; anonymous, inline, and aliased namespace behavior needs explicit coverage |
| Canonical/previous declaration | `FunctionDecl.CANONICAL_DECL` and `PREVIOUS_DECL` are emitted by Clang | IDs are local to one dump and cannot by themselves join separate compiler invocations |
| Function parameters and ellipsis | Function type contains structured parameter types and `IS_VARIADIC` | `getSignature()` omits variadicness and turns types into code strings |
| Method cv/ref qualifiers | `FunctionType` has const/volatile; `FunctionProtoType` has `REFERENCE_QUALIFIER`; `CXXMethodDecl` also has `IS_VOLATILE` | `getSignature()` uses const only. The C++ method dumper comment says `// RefQualifier?`, so confirm the parsed field for real method declarations before relying on it |
| Function template specialization | `FunctionDecl` has primary-template links and typed template arguments | Constrained templates, template parameter structure, and all specialization cases are not established as a complete key |
| Class template specialization | `ClassTemplateSpecializationDecl` has the specialized template and typed arguments | The current tag cache ignores both and keys on the bare name |
| Variable storage and initialization | `VarDecl` has storage class, TLS kind, initializer children, static-data-member flags, global-storage flag, and `NamedDecl.LINKAGE` | The cache treats all non-`None` storage classes as non-definitions and uses no linkage or scope |
| Source path and TU path | `SourceLocation` stores a canonicalized path; `TranslationUnit` stores a `File` and has a relative-path stable ID | `getFilename()` is basename-only; source file identity is not currently a semantic entity key |

The type tree is structured, so a typed key can walk it. It still needs a deliberate canonicalization policy: typedef desugaring, parameter adjustment, top-level qualifiers, record identity, dependent types, template arguments, and language-specific type rules cannot safely be approximated by comparing generated code strings.

`TUnitProcessor` currently indexes valid-location nodes from all parsed TUs using `ClavaNode.getNodeSignature()`, then rewrites node-valued fields while recomputing those signatures. The default node signature includes a display location and `PREVIOUS_ID`; referenced node IDs can therefore change during rewriting. `SourceLocation.toString()` displays only a basename, although the object itself stores a full canonical path. On collision, `addNode()` eagerly retains formatted node contents.

`App` builds `DeclarationsCache` lazily and only invalidates it through `App.clearCache()`. A durable editing model needs either explicit invalidation at every semantic write or a revision owned by the AST/App and checked by the index. A correct key does not make a stale snapshot correct.

## Suggested declaration-key model

The records below describe the needed distinctions; they are not a claim that all current metadata can populate every field reliably.

```text
DeclarationKey = EntityKind + LinkageDomain + SemanticContext + EntityName + Signature

LinkageDomain = External
              | TranslationUnitLocal(TU identity)
              | ModuleLocal(module identity)
              | NoLinkage
              | Unsupported/Unknown
```

`SemanticContext` should be a structured chain of canonical namespace/record/template scopes, not the source spelling at the use site. `EntityName` should distinguish identifier names, operators, constructors/destructors, and conversion functions. `Signature` is kind-specific.

For functions, include canonical parameter types after the language's parameter adjustments, variadicness, member cv/ref qualifiers, and function-template identity/arguments where applicable. Keep return type out of overload selection, but validate it as declaration compatibility: a return-type mismatch must not be silently treated as an unrelated valid overload or accepted definition. Preserve language linkage and calling-convention/exception compatibility as explicit fields or compatibility checks where the target language requires them.

For variables, use semantic scope, name, and linkage domain. Use type and declaration properties to detect incompatible declarations, not to make two variables with the same external name look like separate valid entities. Classify declarations and definitions from compiler semantics and initializer/storage data together. C tentative definitions, C++ internal-linkage namespace `const`, `extern` with an initializer, thread-local objects, and static data members need separate cases.

For tags, include kind, semantic scope, and template identity/arguments. A class template primary, full specialization, and partial specialization with the same spelling are not interchangeable. Anonymous/local types require an explicit TU/owner identity or an unsupported result.

Build indexes as `Map<Key, List<Decl>>`, then classify candidates. Do not silently overwrite a previous definition. Multiple ODR-equivalent inline/template definitions may be expected; multiple incompatible definitions are a conflict. If the public API must continue returning one node, choose a documented deterministic representative only after the index has established the candidate set, and expose conflicts separately.

Do not adopt a mangled name as the sole semantic key. It can be a useful compiler-produced diagnostic or ABI cross-check, but it is target/ABI dependent and does not prove ODR compatibility. A Clang USR may be worth evaluating later, but it would require producer/schema support and still needs tests for local entities, templates, modules, and different compilation configurations.

## Conservative boundaries

The present metadata does not establish complete C/C++ semantic identity for every case. Keep an explicit supported subset and return unknown/ambiguous outside it.

- **Compilation configuration:** the same header parsed under different macros, language modes, target triples, ABI flags, or include roots can describe different entities. The application currently has no demonstrated compatibility fingerprint for these settings. A project identity should carry a TU configuration fingerprint before it joins declarations from independently configured commands.
- **Namespaces and contexts:** qualified-prefix strings do not prove canonical scope identity for inline namespaces, namespace aliases, anonymous scopes, or all nested contexts. Parent declaration chains are better input, but canonical namespace/record mapping across TUs remains a design task.
- **Function templates:** primary templates, overloads, constrained templates, explicit specializations, explicit instantiations, parameter packs, non-type/template-template arguments, and dependent signatures must not collapse to the same `f()`-like key. Constraints can affect overload identity and are not represented as a complete normalized predicate here.
- **Types:** a printed type is not a canonical type ID. Record/enum types need declaration identity; typedefs need correct desugaring; function pointers, arrays/functions adjusted as parameters, top-level cv, member pointers, dependent types, `decltype`, `auto`, and structural template arguments need the language's type rules.
- **Language linkage and C rules:** `extern "C"`, C++ linkage, C tentative definitions, C inline semantics, and compiler extensions such as GNU inline/common behavior differ. Do not infer C behavior from C++ storage-class tests.
- **ODR and configuration conflicts:** equal names and signatures do not prove definitions are ODR-equivalent. Different macros may change inline bodies or class layouts while retaining the same source path and symbol. The cache should report such conflict evidence rather than quietly selecting the first tree encountered.
- **Modules and unusual linkage:** module linkage and visible-no-linkage entities are represented by enum values but have no tested module-aware index. Exclude them from ordinary external lookup until a module policy exists.
- **Occurrence identity:** macro spelling/expansion locations, implicit nodes, invalid/system-header locations, symlink aliases, and multiple nodes at one coordinate can defeat location matching. Keep source-file IDs, node kind, parse-local IDs, and owning TU in the occurrence model; use ambiguity when these do not identify exactly one target.
- **Mutation:** rename, type changes, scope moves, adding/removing declarations, changing linkage/storage, and generated nodes can stale an index. `clearCache()` is manual, so tests should not imply automatic coherence until a revision/invalidation design exists.
- **Parse failures:** CTU03 shows that a failed TU can disappear behind per-file exception handling. Identity work must not make a partial application appear complete; keep requested/ignored TU counts and surface parser errors to callers.

## Test plan for an airtight supported contract

Use the repository's existing cross-TU fixture harness and run each applicable case with serial/parallel parsing and both actual TU orders. Keep compiler parse/link outcomes separate from Clava lookup results. For negative lookup cases, state whether each source compiles by itself, whether the project links, and which declaration a compiler AST reference points to.

### Declaration identity and lookup

- Same external function declaration/definition in one scope joins; same spelling/signature in different namespaces does not. Add namespace aliases, nested namespaces, inline namespaces, and nested records.
- Distinguish C linkage from C++ linkage and test compatible C/`extern "C"` cross-language cases. Verify overload calls against Clang's AST reference or mangled-name metadata, not only Clava's own generated signature.
- Parameter controls: typedef aliases of the same type; distinct pointee/reference types; pointer vs reference; top-level parameter cv that the language ignores; array/function parameter adjustment; variadic vs fixed arity; C no-prototype functions where supported.
- Method matrix: no qualifier, `const`, `volatile`, `const volatile`; no ref qualifier, `&`, `&&`; static data/member functions; nested template records; operators and conversion functions. Assert all candidates/definitions are the expected object identities.
- Template matrix: overloaded function templates, primary template definitions, explicit specializations, explicit instantiations, constrained overloads, parameter packs, dependent arguments, and class primary/full/partial specializations with same spelling but different arguments.
- Linkage matrix: external, `static`, anonymous namespace, namespace-scope C++ `const`, block-scope `extern`, local/no-linkage declarations, and module/visible-no-linkage graceful unsupported behavior.
- Variable definitions: plain `extern`, `extern` with initializer, C tentative definitions, multiple tentative declarations, C++ internal `const`, TLS, static data members, weak/common where supported, and generated globals. Check storage/linkage metadata as well as `getDefinition()`.
- Tag definitions: same name in different scopes; enum vs record; nested types; template specializations; local/anonymous types; multiple complete definitions that are equivalent vs conflicting.
- Missing and duplicate candidates: missing definition remains missing; a same-name unrelated entity cannot satisfy it; duplicate equivalent definitions have documented behavior; conflicting definitions report ambiguity/conflict independent of TU order.

### AST occurrence normalization

- Resolve detached references to the correct node inside their originating TU. Assert references from two TUs to the same shared-header declaration remain attached to each TU's own AST occurrence while entity lookup may connect them.
- Two `common.hpp` files with the same basename, line, and column never alias. Same physical header included by multiple TUs with a TU-local `static` or anonymous-namespace declaration stays TU-local.
- Parse one header under differing macro definitions and differing compile options; ensure occurrences are not merged when node kind, initializer, type, or referenced declarations differ.
- Exercise macro spelling/expansion locations, invalid locations, implicit nodes sharing source coordinates, system headers, symlinked paths, semantic/syntactic initializer pairs, and list/optional/direct node-valued fields.
- Make normalization deterministic under registry iteration changes, parse order, and serial/parallel parsing. Assert idempotence and that a rewrite cannot invalidate a later lookup key.
- Test ambiguous candidates: no unsafe redirect; diagnostics retain bounded metadata; detailed node formatting occurs only when diagnostics are requested.
- Surface per-file parse failures and assert the resulting application reports missing TUs rather than silently claiming a complete parse.

### Cache and performance

- After index construction, mutate each identity input (rename, parameter/return type, qualifiers, linkage/storage, namespace/parent, insert/remove a definition, generated global). Verify a revision-aware index rebuilds or that documented explicit invalidation is required.
- Test two independent `App` instances and repeated parses with the same paths to catch accidental process-global identity state.
- Add a benchmark fixture with many declarations and references. Measure index construction, lookup, allocation count, and full parse time. Compare structured keys to string generation; do not claim a speedup from fewer source lines or from a single synthetic lookup.
- Count formatted-node rendering on collision. The expected count is zero unless the diagnostic is enabled/requested.

## Practical implementation sequence

1. Stabilize parser behavior and error reporting for CTU03, keeping that producer/parser issue distinct from declaration-key changes.
2. Introduce typed declaration keys for currently supported named external entities, with exact tests for CTU01/02/04-08/10/11/16 and a conflict/missing outcome. Use existing typed metadata first; add producer data only where evidence shows a field is absent.
3. Replace cross-TU node-signature rewriting with per-TU occurrence resolution and preserve ambiguity. Freeze keys before any mutation if a full replacement cannot land at once.
4. Add template/type/configuration cases incrementally, expanding the supported key model only when the test suite proves the metadata and compiler semantics.
5. Add revision-based cache invalidation and performance measurements after semantic behavior is stable.

This sequence keeps the first correctness fixes centered in the two actual mechanisms while leaving a clear path to a more complete symbol model.
