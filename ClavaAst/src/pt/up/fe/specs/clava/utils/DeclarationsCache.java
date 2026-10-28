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
    private final DeclarationIdentity generatedIdentity;
    private final Map<Object, List<FunctionDecl>> functions = new HashMap<>();
    private record FunctionLinkageAlias(String name, String translationUnit) {}
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
    private final Map<Object, List<TagDecl>> tags = new HashMap<>();
    private final Map<Object, List<VarDecl>> variables = new HashMap<>();
    private final Set<NamedDecl> indexed = Collections.newSetFromMap(new IdentityHashMap<>());

    public DeclarationsCache(App app) {
        generatedIdentity = new DeclarationIdentity(app);
        app.getDescendantsStream().filter(NamedDecl.class::isInstance).map(NamedDecl.class::cast)
                .forEach(this::add);
        for (var tu : app.getTranslationUnits()) tu.get(TranslationUnit.INCLUDED_DECLARATIONS).forEach(this::add);
    }

    private Object key(NamedDecl declaration) {
        if (declaration.hasValue(NamedDecl.USR) || declaration.get(NamedDecl.EDITED_ENTITY).isPresent()) {
            long version = declaration.getIdentityMutationVersion();
            var previous = keys.get(declaration);
            if (previous != null && previous.version() == version) return previous.value();
            Object key = compilerKey(declaration);
            keys.put(declaration, new CachedKey(version, key));
            return key;
        }
        // Factory-created ASTs retain their existing, generation-scoped identity rules.
        if (declaration instanceof FunctionDecl function) return generatedIdentity.function(function);
        if (declaration instanceof TagDecl tag) return generatedIdentity.tag(tag);
        if (declaration instanceof VarDecl variable) return generatedIdentity.variable(variable);
        return null;
    }

    private Object compilerKey(NamedDecl declaration) {
        if (declaration.get(NamedDecl.EDITED_ENTITY).isPresent()) return declaration.get(NamedDecl.EDITED_ENTITY).get();
        var key = CompilerEntityIdentity.key(declaration).orElse(null);
        if (key == null) return null;
        // This is the same LLVM 18 alias used before, stored once rather than rebuilding two indexes
        // and allocating several identical compiler keys on each query.
        if (declaration instanceof FunctionDecl function
                && function.get(FunctionDecl.TEMPLATE_KIND) == pt.up.fe.specs.clava.ast.decl.enums.TemplateKind.NON_TEMPLATE
                && !key.linkageName().isEmpty()) return new FunctionLinkageAlias(key.linkageName(), key.translationUnit());
        return key;
    }

    private void add(NamedDecl declaration) {
        if (!indexed.add(declaration)) return;
        Object key = key(declaration);
        if (key == null) return;
        if (declaration instanceof FunctionDecl function) add(functions, key, function);
        else if (declaration instanceof TagDecl tag) add(tags, key, tag);
        else if (declaration instanceof VarDecl variable) add(variables, key, variable);
    }

    private static <T> void add(Map<Object, List<T>> map, Object key, T node) {
        map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(node);
    }

    private List<FunctionDecl> functionOccurrences(FunctionDecl function) {
        return functions.getOrDefault(key(function), List.of());
    }

    private boolean current(NamedDecl declaration) {
        if (declaration.get(NamedDecl.EDITED_ENTITY).isPresent()) return CompilerEntityIdentity.isEditCurrent(declaration);
        return !declaration.hasValue(NamedDecl.USR) || CompilerEntityIdentity.isCurrent(declaration);
    }

    public List<NamedDecl> getEntityOccurrences(NamedDecl query) {
        if (!current(query) || key(query) == null) return List.of();
        List<? extends NamedDecl> occurrences;
        if (query instanceof FunctionDecl function) occurrences = functionOccurrences(function);
        else if (query instanceof TagDecl) occurrences = tags.getOrDefault(key(query), List.of());
        else if (query instanceof VarDecl) occurrences = variables.getOrDefault(key(query), List.of());
        else return List.of(query);
        if (occurrences.stream().anyMatch(candidate -> !current(candidate))) return List.of();
        return occurrences.stream().map(NamedDecl.class::cast).toList();
    }

    public boolean sameFunctionEntity(FunctionDecl first, FunctionDecl second) {
        if (!current(first) || !current(second)) return false;
        Object left = key(first), right = key(second);
        if (left == null || right == null) return false;
        return left.equals(right) && compatibleType(first, second) && generatedIdentity.compatibleFunctions(first, second);
    }

    public List<FunctionDecl> getFunctionPrototypes(FunctionDecl function) {
        if (!current(function)) return List.of();
        var occurrences = functionOccurrences(function);
        if (occurrences.stream().anyMatch(candidate -> !current(candidate))) return List.of();
        return occurrences.stream().filter(f -> !isDefinition(f))
                .filter(candidate -> compatibleType(function, candidate)
                        && generatedIdentity.compatibleFunctions(function, candidate)).toList();
    }

    public Optional<FunctionDecl> getFunctionImplementation(FunctionDecl function) {
        var source = (FunctionDecl) pt.up.fe.specs.clava.ClavaNodes.normalizeDecl(function);
        return getFunctionResolution(source).unique();
    }

    public DefinitionResolution<FunctionDecl> getFunctionResolution(FunctionDecl function) {
        return resolve(function, functions, this::isDefinition,
                (a, b) -> compatibleType(a, b) && generatedIdentity.compatibleFunctions(a, b));
    }

    public List<TagDecl> getTagPrototypes(TagDecl tag) {
        if (!current(tag)) return List.of();
        return tags.getOrDefault(key(tag), List.of()).stream().filter(this::current)
                .filter(t -> !t.isCompleteDefinition()).toList();
    }

    public DefinitionResolution<TagDecl> getTagResolution(TagDecl tag) {
        return resolve(tag, tags, this::isDefinition, (a, b) -> true);
    }

    public Optional<TagDecl> getTagImplementation(TagDecl tag) { return getTagResolution(tag).unique(); }

    public DefinitionResolution<VarDecl> getGlobalVarResolution(VarDecl variable) {
        return resolve(variable, variables, this::isDefinition,
                (a, b) -> generatedIdentity.compatibleVariableTypes(a.getType(), b.getType())
                        && a.get(VarDecl.TLS_KIND) == b.get(VarDecl.TLS_KIND));
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
        return declaration instanceof VarDecl variable && DeclarationIdentity.isGlobalDefinition(variable);
    }

    private boolean compatibleType(NamedDecl first, NamedDecl second) {
        if (first instanceof FunctionDecl left && second instanceof FunctionDecl right
                && left.getFunctionType().get(pt.up.fe.specs.clava.ast.type.FunctionType.CALLING_CONVENTION)
                != right.getFunctionType().get(pt.up.fe.specs.clava.ast.type.FunctionType.CALLING_CONVENTION)) return false;
        if (first.get(NamedDecl.EDITED_ENTITY).isPresent() || second.get(NamedDecl.EDITED_ENTITY).isPresent()) return true;
        if (!first.hasValue(NamedDecl.USR) || !second.hasValue(NamedDecl.USR)) return true;
        if (first.get(NamedDecl.USR_TYPE_GENERATION_FAILED) || second.get(NamedDecl.USR_TYPE_GENERATION_FAILED))
            return false;
        return first.get(NamedDecl.USR_TYPE).equals(second.get(NamedDecl.USR_TYPE));
    }

    private <T extends NamedDecl> DefinitionResolution<T> resolve(T query, Map<Object, List<T>> index,
            java.util.function.Predicate<T> definition, java.util.function.BiPredicate<T, T> compatible) {
        if (!current(query)) return result(DefinitionResolution.Kind.STALE, List.of());
        Object key = key(query);
        if (key == null) return result(DefinitionResolution.Kind.UNSUPPORTED, List.of());
        List<T> all;
        if (query instanceof FunctionDecl function) {
            @SuppressWarnings("unchecked") var occurrences = (List<T>) (List<?>) functionOccurrences(function);
            all = occurrences;
        } else all = index.getOrDefault(key, List.of());
        // The bucket's identity was frozen when indexed. Never serve stale entries from a live cache.
        if (all.stream().anyMatch(candidate -> !current(candidate)))
            return result(DefinitionResolution.Kind.STALE, List.of());
        var candidates = candidates(all, definition);
        if (candidates.isEmpty()) return result(DefinitionResolution.Kind.MISSING, List.of());
        if (candidates.stream().anyMatch(candidate -> !compatible.test(query, candidate)))
            return result(DefinitionResolution.Kind.INCOMPATIBLE, candidates);
        if (query instanceof VarDecl && candidates.size() > 1
                && candidates.stream().allMatch(candidate -> candidate.hasValue(NamedDecl.USR))
                && candidates.stream().map(DeclarationIdentity::origin).distinct().count() == 1
                && candidates.stream().anyMatch(candidate -> candidate.get(NamedDecl.USR_DEFINITION_KIND).equals("tentative"))) {
            var complete = candidates.stream()
                    .filter(candidate -> candidate.get(NamedDecl.USR_DEFINITION_KIND).equals("definition")).toList();
            candidates = complete.isEmpty() ? List.of(candidates.get(0)) : complete;
        }
        // Equal source coordinates or USRs are not proof that two definition occurrences are equivalent.
        return result(candidates.size() == 1 ? DefinitionResolution.Kind.UNIQUE
                : DefinitionResolution.Kind.AMBIGUOUS, candidates);
    }

    @SuppressWarnings("unchecked")
    private <T extends NamedDecl> List<T> candidates(List<T> occurrences, java.util.function.Predicate<T> definition) {
        if (occurrences.isEmpty()) return List.of();
        var cached = definitions.get(occurrences);
        if (cached != null && cached.matches(occurrences)) return (List<T>) cached.definitions();
        var candidates = occurrences.stream().filter(definition).sorted(Comparator
                .comparing((T node) -> Objects.toString(DeclarationIdentity.origin(node), ""))
                .thenComparing(node -> node.getLocation().toString()).thenComparing(ClavaNode::getId)).toList();
        long[] versions = new long[occurrences.size()];
        for (int i = 0; i < versions.length; i++) versions[i] = occurrences.get(i).getIdentityMutationVersion();
        definitions.put(occurrences, new CachedDefinitions(versions, candidates));
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
