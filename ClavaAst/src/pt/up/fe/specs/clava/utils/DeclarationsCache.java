/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.util.*;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;

/**
 * An analysis-group index of compiler entities and their distinct AST occurrences. Parsed declarations
 * never fall back to spelling-based identity. Clearing this index does not refresh stale compiler facts.
 */
public class DeclarationsCache {
    private final App app;
    private final SourceDeclaration.Resolver sourceResolver = new SourceDeclaration.Resolver();
    private final Map<SourceDeclaration, List<NamedDecl>> sourceOccurrences = new HashMap<>();
    private final IdentityHashMap<NamedDecl, SourceDeclaration> sourceKeys = new IdentityHashMap<>();
    private final Map<SourceDeclaration, NamedDecl> sourceOwners = new HashMap<>();
    private PostEditBindings bindings;
    private long revision = -1;
    private final List<NamedDecl> inventory = new ArrayList<>();
    private record CachedKey(long version, Object value) {}
    private record CachedDefinitions(long[] versions, List<? extends NamedDecl> definitions) {
        boolean matches(List<? extends NamedDecl> occurrences) {
            if (versions.length != occurrences.size()) return false;
            for (int i = 0; i < versions.length; i++)
                if (versions[i] != occurrences.get(i).getIdentityMutationVersion()) return false;
            return true;
        }
    }
    private final IdentityHashMap<NamedDecl, CachedKey> keys = new IdentityHashMap<>();
    private final IdentityHashMap<List<?>, CachedDefinitions> definitions = new IdentityHashMap<>();
    private final Set<NamedDecl> indexed = Collections.newSetFromMap(new IdentityHashMap<>());

    public DeclarationsCache(App app) {
        this.app = app;
        refresh();
    }

    private void refresh() {
        long current = app.getContext().getDeclarationRevision();
        if (revision == current) return;
        indexed.clear(); inventory.clear();
        keys.clear(); definitions.clear(); sourceOccurrences.clear(); sourceKeys.clear(); sourceOwners.clear();
        app.getDescendantsStream().filter(NamedDecl.class::isInstance).map(NamedDecl.class::cast).forEach(this::add);
        for (var tu : app.getTranslationUnits()) tu.get(TranslationUnit.INCLUDED_DECLARATIONS).forEach(this::add);
        bindings = new PostEditBindings(app, inventory, this::key);
        revision = app.getContext().getDeclarationRevision();
    }

    private Object key(NamedDecl declaration) {
        long version = declaration.getIdentityMutationVersion();
        var cached = keys.get(declaration);
        if (cached != null && cached.version() == version) return cached.value();
        Object key = PostEditBindings.identity(declaration);
        keys.put(declaration, new CachedKey(version, key));
        return key;
    }

    private void add(NamedDecl declaration) {
        if (!indexed.add(declaration)) return;
        inventory.add(declaration);
        sourceResolver.of(declaration).ifPresent(source -> {
            sourceKeys.put(declaration, source);
            add(sourceOccurrences, source, declaration);
        });
    }

    private static <K, T> void add(Map<K, List<T>> map, K key, T node) {
        map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(node);
    }

    private List<FunctionDecl> functionOccurrences(FunctionDecl function) {
        return bindings.bind(function).occurrences().stream().filter(FunctionDecl.class::isInstance).map(FunctionDecl.class::cast).toList();
    }

    private List<? extends NamedDecl> entityOccurrences(NamedDecl query) {
        return bindings.editOccurrences(query);
    }

    private boolean current(NamedDecl declaration) {
        if (declaration.get(NamedDecl.EDITED_ENTITY).isPresent()) return CompilerEntityIdentity.isEditCurrent(declaration);
        return !declaration.hasValue(NamedDecl.USR) || CompilerEntityIdentity.isCurrent(declaration);
    }

    public List<NamedDecl> getEntityOccurrences(NamedDecl query) {
        refresh();
        if (!current(query) || key(query) == null) return List.of();
        // Follow both semantic relationships and physical-source relationships to a fixed point.
        // A static header function has separate entities but one source action must update them all.
        var seen = Collections.newSetFromMap(new IdentityHashMap<NamedDecl, Boolean>());
        var pending = new ArrayDeque<NamedDecl>();
        pending.add(query);
        var result = new ArrayList<NamedDecl>();
        while (!pending.isEmpty()) {
            var next = pending.removeFirst();
            if (!seen.add(next)) continue;
            if (!current(next) || key(next) == null) return List.of();
            result.add(next);
            for (var occurrence : entityOccurrences(next)) if (!seen.contains(occurrence)) pending.add(occurrence);
            Optional.ofNullable(sourceKeys.get(next)).ifPresent(source -> {
                for (var occurrence : sourceOccurrences.getOrDefault(source, List.of()))
                    if (!seen.contains(occurrence)) pending.add(occurrence);
            });
        }
        return List.copyOf(result);
    }

    public Map<NamedDecl, Object> currentIdentities() {
        refresh();
        var result = new IdentityHashMap<NamedDecl, Object>();
        for (var declaration : inventory)
            if ((declaration.hasValue(NamedDecl.USR) || declaration.get(NamedDecl.EDITED_ENTITY).isPresent())
                    && key(declaration) != null && current(declaration)) result.put(declaration, key(declaration));
        return result;
    }

    /** Preserve distinct TU-local semantic entities while coordinating their common source edit. */
    public Map<NamedDecl, Object> editIdentities(List<? extends NamedDecl> declarations) {
        refresh();
        var result = new IdentityHashMap<NamedDecl, Object>();
        for (var declaration : declarations) result.put(declaration, bindings.identityForEdit(declaration));
        return result;
    }

    public boolean sameFunctionEntity(FunctionDecl first, FunctionDecl second) {
        refresh();
        if (!current(first) || !current(second)) return false;
        Object left = key(first), right = key(second);
        if (left == null || right == null) return false;
        boolean same = left.equals(right);
        if (!same) {
            var relation = bindings.bind(first);
            same = relation.status() == PostEditBindings.Status.PROVEN && relation.occurrences().contains(second);
        }
        return same && compatibleType(first, second);
    }

    public List<pt.up.fe.specs.clava.ast.expr.CallExpr> getFunctionCalls(FunctionDecl query) {
        refresh();
        var declarations = Collections.newSetFromMap(new IdentityHashMap<NamedDecl, Boolean>());
        declarations.addAll(getEntityOccurrences(query));
        if (declarations.isEmpty()) return List.of();
        var nodes = new ArrayList<ClavaNode>(app.getDescendants());
        for (var unit : app.getTranslationUnits()) for (var declaration : unit.get(TranslationUnit.INCLUDED_DECLARATIONS))
            nodes.addAll(declaration.getDescendantsAndSelfStream().toList());
        var calls = new LinkedHashMap<Object, pt.up.fe.specs.clava.ast.expr.CallExpr>();
        for (var node : nodes) if (node instanceof pt.up.fe.specs.clava.ast.expr.CallExpr call
                && call.getFunctionDecl().map(declarations::contains).orElse(false)) {
            var location = call.getLocation();
            Object source = location.isValid() && !call.get(ClavaNode.IS_MACRO) ? location : call;
            calls.putIfAbsent(source, call);
        }
        return List.copyOf(calls.values());
    }

    public List<FunctionDecl> getFunctionPrototypes(FunctionDecl function) {
        refresh();
        if (!current(function)) return List.of();
        var occurrences = functionOccurrences(function);
        if (occurrences.stream().anyMatch(candidate -> !current(candidate))) return List.of();
        return occurrences.stream().filter(f -> !isDefinition(f))
                .filter(candidate -> compatibleType(function, candidate)).toList();
    }

    public Optional<FunctionDecl> getFunctionImplementation(FunctionDecl function) {
        var source = (FunctionDecl) pt.up.fe.specs.clava.ClavaNodes.normalizeDecl(function);
        return getFunctionResolution(source).unique();
    }

    public DefinitionResolution<FunctionDecl> getFunctionResolution(FunctionDecl function) {
        return resolve(function, this::isDefinition,
                this::compatibleType);
    }

    public List<TagDecl> getTagPrototypes(TagDecl tag) {
        refresh();
        if (!current(tag)) return List.of();
        return bindings.bind(tag).occurrences().stream().filter(TagDecl.class::isInstance).map(TagDecl.class::cast).filter(this::current)
                .filter(t -> !t.isCompleteDefinition()).toList();
    }

    public DefinitionResolution<TagDecl> getTagResolution(TagDecl tag) {
        return resolve(tag, this::isDefinition, this::compatibleType);
    }

    public Optional<TagDecl> getTagImplementation(TagDecl tag) { return getTagResolution(tag).unique(); }

    public DefinitionResolution<VarDecl> getGlobalVarResolution(VarDecl variable) {
        return resolve(variable, this::isDefinition,
                this::compatibleType);
    }

    public Optional<VarDecl> getGlobalVarDefinition(VarDecl variable) {
        return getGlobalVarResolution(variable).unique();
    }

    private boolean isDefinition(NamedDecl declaration) {
        if (declaration.hasValue(NamedDecl.USR) && declaration.get(NamedDecl.EDITED_ENTITY).isEmpty()) {
            String kind = declaration.get(NamedDecl.USR_DEFINITION_KIND);
            return kind.equals("definition") || kind.equals("tentative");
        }
        if (declaration instanceof FunctionDecl function) return function.hasBody();
        if (declaration instanceof TagDecl tag) return tag.isCompleteDefinition();
        return declaration instanceof VarDecl variable && DeclarationContext.isGlobalDefinition(variable);
    }

    private boolean compatibleType(NamedDecl first, NamedDecl second) {
        return bindings.schema().compatible(first, second) == TypeSemantics.Verdict.COMPATIBLE;
    }

    @SuppressWarnings("unchecked")
    private <T extends NamedDecl> DefinitionResolution<T> resolve(T query,
            java.util.function.Predicate<T> definition, java.util.function.BiPredicate<T, T> compatible) {
        refresh();
        if (!current(query)) return result(DefinitionResolution.Kind.STALE, List.of());
        Object key = key(query);
        if (key == null) return result(DefinitionResolution.Kind.UNSUPPORTED, List.of());
        var relation = bindings.bind(query);
        if (relation.status() == PostEditBindings.Status.UNSUPPORTED) return result(DefinitionResolution.Kind.UNSUPPORTED, List.of());
        @SuppressWarnings("unchecked") List<T> all = (List<T>) (List<?>) relation.occurrences();
        // The bucket's identity was frozen when indexed. Never serve stale entries from a live cache.
        if (all.stream().anyMatch(candidate -> !current(candidate)))
            return result(DefinitionResolution.Kind.STALE, List.of());
        var candidates = candidates(all, definition);
        if (relation.status() == PostEditBindings.Status.INCOMPATIBLE)
            return result(DefinitionResolution.Kind.INCOMPATIBLE, candidates);
        if (relation.status() == PostEditBindings.Status.AMBIGUOUS)
            return result(DefinitionResolution.Kind.AMBIGUOUS, candidates);
        if (candidates.isEmpty()) return result(DefinitionResolution.Kind.MISSING, List.of());
        if (candidates.stream().anyMatch(candidate -> !compatible.test(query, candidate))) {
            // Different compiler contexts of one physical definition are inspectable separately.
            // A generated query still needs a compatible binding; distinct source definitions remain conflicts.
            boolean oneSource = query.hasValue(NamedDecl.USR) && candidates.stream().allMatch(sourceKeys::containsKey)
                    && candidates.stream().map(sourceKeys::get).distinct().count() == 1
                    && candidates.stream().anyMatch(candidate -> compatible.test(query, candidate));
            if (!oneSource) return result(DefinitionResolution.Kind.INCOMPATIBLE, candidates);
        }
        if (query instanceof VarDecl && candidates.size() > 1
                && candidates.stream().allMatch(candidate -> candidate.hasValue(NamedDecl.USR))
                && candidates.stream().map(DeclarationContext::origin).distinct().count() == 1
                && candidates.stream().anyMatch(candidate -> candidate.get(NamedDecl.USR_DEFINITION_KIND).equals("tentative"))) {
            var complete = candidates.stream()
                    .filter(candidate -> candidate.get(NamedDecl.USR_DEFINITION_KIND).equals("definition")).toList();
            candidates = complete.isEmpty() ? List.of(candidates.get(0)) : complete;
        }
        var contexts = new ArrayList<T>();
        var sources = new LinkedHashMap<Object, T>();
        for (var candidate : candidates) {
            Object source = sourceKeys.containsKey(candidate) ? sourceKeys.get(candidate) : candidate;
            if (sources.containsKey(source)) continue;
            var peers = source instanceof SourceDeclaration physical
                    ? sourceOccurrences.getOrDefault(physical, List.of()) : List.<NamedDecl>of(candidate);
            for (var peer : peers) {
                if (!current(peer)) return result(DefinitionResolution.Kind.STALE, List.of());
                if (definition.test((T) peer) && !contexts.contains(peer)) contexts.add((T) peer);
            }
            // Prefer an occurrence attached to its owning editable source file, when one is selected.
            T owner = source instanceof SourceDeclaration physical
                    ? (T) sourceOwners.computeIfAbsent(physical, ignored -> peers.stream().filter(this::isDefinition).min(Comparator
                            .comparing((NamedDecl node) -> node.getAncestorTry(TranslationUnit.class).isEmpty())
                            .thenComparing(node -> pt.up.fe.specs.clava.ClavaNodes.normalizeDecl(node) != node)
                            .thenComparing(node -> Objects.toString(DeclarationContext.origin(node), ""))
                            .thenComparing(ClavaNode::getId)).orElse(candidate)) : candidate;
            sources.putIfAbsent(source, owner);
        }
        var sourceDefinitions = List.copyOf(sources.values());
        return new DefinitionResolution<>(sourceDefinitions.size() == 1 ? DefinitionResolution.Kind.UNIQUE
                : DefinitionResolution.Kind.AMBIGUOUS, sourceDefinitions, contexts);
    }

    @SuppressWarnings("unchecked")
    private <T extends NamedDecl> List<T> candidates(List<T> occurrences, java.util.function.Predicate<T> definition) {
        if (occurrences.isEmpty()) return List.of();
        var cached = definitions.get(occurrences);
        if (cached != null && cached.matches(occurrences)) return (List<T>) cached.definitions();
        var candidates = occurrences.stream().filter(definition).sorted(Comparator
                .comparing((T node) -> Objects.toString(DeclarationContext.origin(node), ""))
                .thenComparing(node -> node.getLocation().toString()).thenComparing(ClavaNode::getId)).toList();
        long[] versions = new long[occurrences.size()];
        for (int i = 0; i < versions.length; i++) versions[i] = occurrences.get(i).getIdentityMutationVersion();
        // Inferred relationships are freshly proved. Retaining their temporary lists would leak one
        // cache entry per lookup and could conceal an in-place mutation of a generated type.
        if (bindings.stableOccurrences(occurrences)) definitions.put(occurrences, new CachedDefinitions(versions, candidates));
        return candidates;
    }

    private static <T> DefinitionResolution<T> result(DefinitionResolution.Kind kind, List<T> nodes) {
        return new DefinitionResolution<>(kind, nodes);
    }

    /** Diagnostic compatibility only. */
    public String getFunctionKey(FunctionDecl function) { return Objects.toString(key(function), "unsupported"); }
    /** Diagnostic compatibility only. */
    public String getTagKey(TagDecl tag) { return Objects.toString(key(tag), "unsupported"); }
}
