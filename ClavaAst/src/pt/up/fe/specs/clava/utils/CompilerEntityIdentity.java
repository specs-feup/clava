/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.util.List;
import java.util.Optional;

import pt.up.fe.specs.clava.ast.decl.*;

/** Compiler identity is authoritative for parsed entities. Generated nodes use an explicitly separate domain. */
public final class CompilerEntityIdentity {
    private CompilerEntityIdentity() {}

    public record Key(String family, String usr, String linkageName, String translationUnit) {}

    public static Optional<Key> key(NamedDecl declaration) {
        if (!declaration.hasValue(NamedDecl.USR) || declaration.get(NamedDecl.USR_GENERATION_FAILED)
                || declaration.get(NamedDecl.USR).isBlank()) return Optional.empty();
        String domain = "";
        if (declaration.get(NamedDecl.USR_IS_TU_LOCAL)) {
            domain = DeclarationContext.origin(declaration);
            if (domain == null) return Optional.empty();
        }
        String family = declaration instanceof FunctionDecl ? "function"
                : declaration instanceof TagDecl ? "tag" : declaration instanceof VarDecl ? "variable" : "other";
        String usr = declaration.get(NamedDecl.USR);
        String linkageName = declaration.get(NamedDecl.USR_LINKAGE_NAME);
        // Clang 18 USRs retain namespaces on extern-C declarations, and omit noexcept in
        // function-pointer overload parameters. The compiler-provided linkage name handles both.
        if (declaration.get(NamedDecl.USR_IS_C_LINKAGE)) {
            if (linkageName.isEmpty()) return Optional.empty();
            usr = "C-linkage";
        }
        return Optional.of(new Key(family, usr, linkageName, domain));
    }

    public static void capture(NamedDecl declaration) {
        if (declaration.get(NamedDecl.USR_SOURCE_STATE).isEmpty())
            declaration.set(NamedDecl.USR_SOURCE_STATE, Optional.of(new CompilerIdentityState(declaration, sourceState(declaration))));
    }

    /** Capture a read-only parser batch, sharing immutable type baselines and observations across occurrences. */
    public static void captureAll(java.util.Collection<? extends NamedDecl> declarations) {
        var identity = new DeclarationState();
        var types = new java.util.IdentityHashMap<pt.up.fe.specs.clava.ast.type.Type,
                CompilerIdentityState.Observation[]>();
        for (var declaration : declarations) {
            if (declaration.get(NamedDecl.USR_SOURCE_STATE).isEmpty())
                declaration.set(NamedDecl.USR_SOURCE_STATE,
                        Optional.of(new CompilerIdentityState(declaration, sourceState(declaration, identity), types)));
        }
    }

    public static boolean isCurrent(NamedDecl declaration) {
        return declaration.get(NamedDecl.USR_SOURCE_STATE).map(state -> state instanceof CompilerIdentityState snapshot
                ? snapshot.isCurrent(declaration) : state.equals(sourceState(declaration)))
                .orElse(false);
    }

    public static boolean ownsCapturedState(NamedDecl declaration) {
        return java.util.stream.Stream.of(declaration.get(NamedDecl.USR_SOURCE_STATE), declaration.get(NamedDecl.EDITED_ENTITY_STATE))
                .flatMap(Optional::stream).anyMatch(state -> state instanceof CompilerIdentityState snapshot && snapshot.owns(declaration));
    }

    public static Object newEditedEntity() { return java.util.UUID.randomUUID(); }

    public static void captureEdit(NamedDecl declaration, Object entity) {
        declaration.set(NamedDecl.EDITED_ENTITY, Optional.of(entity));
        declaration.set(NamedDecl.EDITED_ENTITY_STATE, Optional.of(new CompilerIdentityState(declaration, sourceState(declaration))));
    }

    public static boolean isEditCurrent(NamedDecl declaration) {
        return declaration.get(NamedDecl.EDITED_ENTITY_STATE).map(state -> state instanceof CompilerIdentityState snapshot
                ? snapshot.isCurrent(declaration) : state.equals(sourceState(declaration)))
                .orElse(false);
    }

    public static void makeGeneratedTree(pt.up.fe.specs.clava.ClavaNode node) {
        node.getDescendantsAndSelfStream().filter(NamedDecl.class::isInstance).map(NamedDecl.class::cast)
                .forEach(CompilerEntityIdentity::makeGenerated);
    }

    /** A newly created declaration must not inherit the source entity's compiler identity. */
    public static void makeGenerated(NamedDecl declaration) {
        for (var key : List.of(NamedDecl.USR, NamedDecl.USR_GENERATION_FAILED, NamedDecl.USR_IS_TU_LOCAL,
                NamedDecl.USR_IS_C_LINKAGE, NamedDecl.USR_LINKAGE_NAME, NamedDecl.USR_TYPE,
                NamedDecl.USR_TYPE_GENERATION_FAILED, NamedDecl.USR_DEFINITION_KIND,
                NamedDecl.USR_SOURCE_STATE, NamedDecl.EDITED_ENTITY, NamedDecl.EDITED_ENTITY_STATE))
            declaration.set(key, null);
    }

    public static Object sourceState(NamedDecl declaration) {
        return new DeclarationState().capture(declaration);
    }

    private static Object sourceState(NamedDecl declaration, DeclarationState state) {
        return state.capture(declaration);
    }
}
