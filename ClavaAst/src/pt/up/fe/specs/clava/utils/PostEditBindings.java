/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.util.*;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateKind;
import pt.up.fe.specs.clava.ast.extra.App;

/** Binding proofs for declarations introduced through AST APIs. Native entities never use inferred identity. */
public final class PostEditBindings {
    public record CompilerSymbol(String family, String symbol, String translationUnit) {}
    public record GeneratedSymbol(DeclarationContext.Reference declaration) {}
    public record GeneratedRecord(DeclarationContext.Symbol symbol, Object selector) {}
    public enum Status { PROVEN, MISSING, INCOMPATIBLE, UNSUPPORTED, AMBIGUOUS }
    public record Binding(Status status, List<NamedDecl> occurrences, String reason) {
        public Binding { occurrences = List.copyOf(occurrences); }
    }
    private final App app;
    private final Map<DeclarationContext.Symbol, List<NamedDecl>> bySymbol = new HashMap<>();
    private final Map<Object, List<NamedDecl>> byIdentity = new HashMap<>();
    private final IdentityHashMap<NamedDecl, Integer> order = new IdentityHashMap<>();
    private final Set<NamedDecl> generated = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<DeclarationContext.Symbol> adaptedSymbols = new HashSet<>();
    private final Set<List<?>> stableBuckets = Collections.newSetFromMap(new IdentityHashMap<>());
    public PostEditBindings(App app, Collection<NamedDecl> declarations) {
        this(app, declarations, PostEditBindings::identity);
    }
    public PostEditBindings(App app, Collection<NamedDecl> declarations,
            java.util.function.Function<NamedDecl, Object> identities) {
        this.app = app;
        for (var declaration : declarations) {
            order.put(declaration, order.size());
            Object identity = identities.apply(declaration);
            if (identity == null) continue;
            byIdentity.computeIfAbsent(identity, ignored -> new ArrayList<>()).add(declaration);
            if (!declaration.hasValue(NamedDecl.USR) || declaration.get(NamedDecl.EDITED_ENTITY).isPresent()) {
                generated.add(declaration);
                adaptedSymbols.add(DeclarationContext.symbol(declaration, app));
            }
        }
        // No Java declaration-selection inventory is needed until an API-created/edited declaration exists.
        if (!generated.isEmpty()) for (var declaration : declarations)
            if (identities.apply(declaration) != null)
                bySymbol.computeIfAbsent(DeclarationContext.symbol(declaration, app), ignored -> new ArrayList<>()).add(declaration);
        byIdentity.replaceAll((key, values) -> List.copyOf(values));
        bySymbol.replaceAll((key, values) -> List.copyOf(values));
        stableBuckets.addAll(byIdentity.values());
    }
    public boolean stableOccurrences(List<?> occurrences) { return stableBuckets.contains(occurrences); }
    public static Object identity(NamedDecl declaration) {
        if (declaration.get(NamedDecl.EDITED_ENTITY).isPresent()) return declaration.get(NamedDecl.EDITED_ENTITY).get();
        if (!declaration.hasValue(NamedDecl.USR)) return new GeneratedSymbol(new DeclarationContext.Reference(declaration));
        var key = CompilerEntityIdentity.key(declaration);
        if (key.isEmpty()) return null;
        var value = key.get();
        boolean alias = declaration instanceof FunctionDecl function && function.get(FunctionDecl.TEMPLATE_KIND) == TemplateKind.NON_TEMPLATE;
        String symbol = (alias || declaration.get(NamedDecl.USR_IS_C_LINKAGE)) && !value.linkageName().isBlank()
                ? "linkage:" + value.linkageName() : "usr:" + value.usr();
        return new CompilerSymbol(value.family(), symbol, value.translationUnit());
    }
    public DeclarationSchema schema() { return new DeclarationSchema(app, this::nominal); }
    private Object nominal(TagDecl declaration) {
        if (declaration.hasValue(NamedDecl.USR) || declaration.get(NamedDecl.EDITED_ENTITY).isPresent()) return identity(declaration);
        var raw = new DeclarationSchema(app).describe(declaration);
        if (!raw.supported()) return null;
        var candidates = bySymbol.getOrDefault(raw.symbol(), List.of());
        var identities = new HashSet<Object>();
        int definitions = 0;
        for (var candidate : candidates) if (candidate instanceof TagDecl tag) {
            var profile = new DeclarationSchema(app).describe(tag);
            if (!profile.supported() || !raw.selector().equals(profile.selector())) continue;
            if (candidate.hasValue(NamedDecl.USR) || candidate.get(NamedDecl.EDITED_ENTITY).isPresent()) identities.add(identity(candidate));
            if (tag.isCompleteDefinition()) definitions++;
        }
        if (identities.size() == 1) return identities.iterator().next();
        if (identities.size() > 1 || definitions > 1) return null;
        return new GeneratedRecord(raw.symbol(), raw.selector());
    }
    public Binding bind(NamedDecl query) {
        Object identity = identity(query);
        if (identity == null) return new Binding(Status.UNSUPPORTED, List.of(), "compiler identity generation failed");
        var own = byIdentity.getOrDefault(identity, List.of(query));
        if (!query.hasValue(NamedDecl.USR) && query.get(NamedDecl.EDITED_ENTITY).isEmpty()
                && query instanceof FunctionDecl function && function.get(FunctionDecl.TEMPLATE_KIND) != TemplateKind.NON_TEMPLATE)
            return new Binding(Status.UNSUPPORTED, own, "new template bindings require compiler constraint and specialization evidence");
        boolean inferred = !query.hasValue(NamedDecl.USR) || query.get(NamedDecl.EDITED_ENTITY).isPresent();
        // The ordinary native path does no Java structural work when there are no generated declarations.
        if (!inferred && (generated.isEmpty() || !adaptedSymbols.contains(DeclarationContext.symbol(query, app))))
            return new Binding(Status.PROVEN, own, "compiler identity without adapted candidates");
        var schema = schema();
        var profile = schema.describe(query);
        if (!profile.supported()) return new Binding(inferred ? Status.UNSUPPORTED : Status.PROVEN, own, profile.unsupported());
        var matches = new ArrayList<NamedDecl>(own);
        var identities = new HashSet<Object>();
        boolean incompatible = false, unknown = false;
        var conflicts = new ArrayList<NamedDecl>();
        for (var candidate : bySymbol.getOrDefault(profile.symbol(), List.of())) {
            if (candidate == query || matches.contains(candidate)) continue;
            if (!inferred && !generated.contains(candidate)) continue;
            var other = schema.describe(candidate);
            if (!other.supported()) { unknown = true; continue; }
            if (!profile.selector().equals(other.selector())) continue;
            var verdict = schema.compatible(query, candidate);
            if (verdict == TypeSemantics.Verdict.INCOMPATIBLE) { incompatible = true; conflicts.add(candidate); continue; }
            if (verdict == TypeSemantics.Verdict.UNSUPPORTED) { unknown = true; continue; }
            Object candidateId = identity(candidate);
            if (candidate.hasValue(NamedDecl.USR) || candidateId instanceof CompilerSymbol) identities.add(candidateId);
            matches.add(candidate);
        }
        if (incompatible) {
            matches.addAll(conflicts);
            return new Binding(Status.INCOMPATIBLE, matches, "current declarations collide with incompatible types");
        }
        if (inferred && identities.size() > 1) return new Binding(Status.AMBIGUOUS, matches, "more than one compiler entity satisfies the generated declaration");
        if (inferred && matches.size() == own.size()) {
            if (incompatible) return new Binding(Status.INCOMPATIBLE, matches, "same declaration selector has incompatible type");
            if (unknown) return new Binding(Status.UNSUPPORTED, matches, "candidate type could not be proved compatible");
        }
        return new Binding(Status.PROVEN, matches, "typed declaration proof");
    }
    public List<NamedDecl> editOccurrences(NamedDecl query) {
        if (query.hasValue(NamedDecl.USR) || query.get(NamedDecl.EDITED_ENTITY).isPresent()) {
            var result = new ArrayList<>(byIdentity.getOrDefault(identity(query), List.of(query)));
            for (var candidate : bySymbol.getOrDefault(DeclarationContext.symbol(query, app), List.of())) {
                if (candidate.hasValue(NamedDecl.USR) || result.contains(candidate)) continue;
                var proof = bind(candidate);
                if (proof.status() == Status.PROVEN && proof.occurrences().stream().anyMatch(result::contains)) result.add(candidate);
            }
            return List.copyOf(result);
        }
        var relation = bind(query);
        return relation.status() == Status.PROVEN ? relation.occurrences() : List.of();
    }
    public Object identityForEdit(NamedDecl query) {
        if (query.hasValue(NamedDecl.USR) || query.get(NamedDecl.EDITED_ENTITY).isPresent()) return identity(query);
        var result = bind(query);
        if (result.status() != Status.PROVEN) throw new RefreshRequiredException("binding is " + result.status() + ": " + result.reason());
        var compiler = result.occurrences().stream().filter(node -> node.hasValue(NamedDecl.USR) || identity(node) instanceof CompilerSymbol)
                .map(PostEditBindings::identity).distinct().toList();
        if (compiler.size() > 1) throw new RefreshRequiredException("generated binding is ambiguous");
        if (!compiler.isEmpty()) return compiler.get(0);
        return result.occurrences().stream().min(Comparator.comparingInt(node -> order.getOrDefault(node, Integer.MAX_VALUE)))
                .map(PostEditBindings::identity).orElseGet(() -> identity(query));
    }
}
