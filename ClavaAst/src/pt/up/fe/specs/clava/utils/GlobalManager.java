/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.util.*;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.decl.enums.*;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.*;
import pt.up.fe.specs.clava.ast.type.Type;

/** Ensures one live, type-compatible shared global; no name-only memory survives AST changes. */
public class GlobalManager {
    private final App owner;
    private final Set<TranslationUnit> units = Collections.newSetFromMap(new IdentityHashMap<>());
    public GlobalManager() { this(null); }
    public GlobalManager(App owner) { this.owner = owner; }
    public VarDecl addGlobal(TranslationUnit unit, String name, Type type, Expr initializer) {
        if (owner != null && !owner.getTranslationUnits().contains(unit)) throw new IllegalArgumentException("TU belongs to another App");
        units.add(unit);
        var declaration = unit.getFactory().varDecl(name, type);
        declaration.set(VarDecl.IS_USED, true);
        declaration.set(VarDecl.HAS_GLOBAL_STORAGE, true);
        declaration.set(NamedDecl.LINKAGE, Linkage.External);
        declaration.set(VarDecl.STORAGE_CLASS, StorageClass.Extern);
        unit.addDeclaration(declaration);
        try {
            var graph = owner == null ? units : owner.getTranslationUnits();
            var inventory = new ArrayList<NamedDecl>();
            for (var tu : graph) {
                tu.getDescendantsStream().filter(NamedDecl.class::isInstance).map(NamedDecl.class::cast).forEach(inventory::add);
                inventory.addAll(tu.get(TranslationUnit.INCLUDED_DECLARATIONS));
            }
            var binding = new PostEditBindings(owner, inventory).bind(declaration);
            if (binding.status() != PostEditBindings.Status.PROVEN)
                throw new RefreshRequiredException("shared global has " + binding.status() + " declaration relationships");
            var definitions = new LinkedHashMap<Object, VarDecl>();
            for (var candidate : binding.occurrences()) if (candidate instanceof VarDecl variable) {
                boolean current = variable.get(NamedDecl.EDITED_ENTITY).isPresent() ? CompilerEntityIdentity.isEditCurrent(variable)
                        : !variable.hasValue(NamedDecl.USR) || CompilerEntityIdentity.isCurrent(variable);
                if (!current) throw new RefreshRequiredException("shared global has stale compiler facts");
                boolean definition = variable.hasValue(NamedDecl.USR) && variable.get(NamedDecl.EDITED_ENTITY).isEmpty()
                        ? Set.of("definition", "tentative").contains(variable.get(NamedDecl.USR_DEFINITION_KIND))
                        : DeclarationContext.isGlobalDefinition(variable);
                if (definition) definitions.putIfAbsent(SourceDeclaration.of(variable).<Object>map(value -> value).orElse(variable), variable);
            }
            if (definitions.size() > 1) throw new RefreshRequiredException("shared global has multiple definition sources");
            // As with get-or-create, an initializer is used only when no compatible definition exists.
            if (definitions.isEmpty() && initializer != null) {
                declaration.addChild(initializer);
                if (!type.isConst()) declaration.set(VarDecl.STORAGE_CLASS, StorageClass.None);
            }
            if (owner != null) owner.clearCache();
            return declaration;
        } catch (RuntimeException failure) {
            var parent = declaration.getParent();
            if (parent instanceof pt.up.fe.specs.clava.ast.stmt.DeclStmt && parent.getNumChildren() == 1) parent.detach();
            else declaration.detach();
            if (owner != null) owner.clearCache();
            throw failure;
        }
    }
}
