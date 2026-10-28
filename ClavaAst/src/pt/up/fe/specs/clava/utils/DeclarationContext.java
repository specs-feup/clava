/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.nio.file.Path;
import java.util.*;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.decl.enums.*;
import pt.up.fe.specs.clava.ast.extra.*;
import pt.up.fe.specs.clava.ast.extra.data.Language;

/** Source scope and linkage rules. No type spelling, node ID or cached signature establishes identity. */
public final class DeclarationContext {
    private DeclarationContext() {}
    public enum Family { FUNCTION, OBJECT, RECORD, ENUM, OTHER }
    public enum LanguageLinkage { C, CXX }
    public record TranslationUnitDomain(String path) {}
    public record LexicalDomain(Reference owner) {}
    public record ModuleDomain(Object compilerIdentity) {}
    public record Symbol(Family family, List<String> scope, String name, Object domain, LanguageLinkage language) {
        public Symbol { scope = List.copyOf(scope); }
    }
    /** Exact AST ownership is legitimate evidence for a local scope or a generated nominal declaration. */
    public static final class Reference {
        private final Object value;
        public Reference(Object value) { this.value = Objects.requireNonNull(value); }
        @Override public boolean equals(Object other) { return other instanceof Reference r && value == r.value; }
        @Override public int hashCode() { return System.identityHashCode(value); }
        @Override public String toString() { return "local-owner"; }
    }
    public static String origin(ClavaNode node) {
        return node.getAncestorTry(TranslationUnit.class).map(unit -> unit.getFile().toPath().toAbsolutePath().normalize().toString())
                .orElseGet(() -> node.get(ClavaNode.ORIGINATING_TRANSLATION_UNIT)
                        .map(path -> Path.of(path).toAbsolutePath().normalize().toString()).orElse(null));
    }
    public static LanguageLinkage language(NamedDecl node, App app) {
        if (node instanceof CXXMethodDecl) return LanguageLinkage.CXX;
        for (var parent = node.getParent(); parent != null; parent = parent.getParent())
            if (parent instanceof LinkageSpecDecl spec)
                return spec.get(LinkageSpecDecl.LINKAGE_TYPE).toString().equals("C") ? LanguageLinkage.C : LanguageLinkage.CXX;
        if (node.hasValue(NamedDecl.USR) && node.get(NamedDecl.USR_IS_C_LINKAGE)) return LanguageLinkage.C;
        var unit = node.getAncestorTry(TranslationUnit.class);
        if (unit.isEmpty() && app != null) unit = app.getTranslationUnits().stream()
                .filter(tu -> Objects.equals(origin(node), tu.getFile().toPath().toAbsolutePath().normalize().toString())).findFirst();
        return unit.map(tu -> tu.get(TranslationUnit.LANGUAGE).get(Language.C_PLUS_PLUS) ? LanguageLinkage.CXX : LanguageLinkage.C)
                .orElse(node.hasValue(NamedDecl.USR) && node.get(NamedDecl.USR_IS_C_LINKAGE) ? LanguageLinkage.C : LanguageLinkage.CXX);
    }
    public static Symbol symbol(NamedDecl node, App app) {
        Family family = node instanceof FunctionDecl ? Family.FUNCTION : node instanceof VarDecl ? Family.OBJECT
                : node instanceof EnumDecl ? Family.ENUM : node instanceof TagDecl ? Family.RECORD : Family.OTHER;
        var language = language(node, app);
        var names = new ArrayList<String>();
        String prefix = node.get(NamedDecl.QUALIFIED_PREFIX);
        if (node instanceof CXXMethodDecl method && method.getRecordDecl().isPresent()) {
            var owner = symbol(method.getRecordDecl().get(), app);
            names.addAll(owner.scope()); names.add(owner.name());
        } else if (!prefix.isBlank()) for (String part : prefix.split("::")) { if (!part.isBlank()) names.add(part); }
        else {
            for (var parent = node.getParent(); parent != null; parent = parent.getParent())
                if (parent instanceof NamespaceDecl || parent instanceof TagDecl) names.add(0, ((NamedDecl) parent).getDeclName());
            if (node instanceof CXXMethodDecl method && method.getRecordDecl().isPresent() && names.isEmpty()) {
                var owner = symbol(method.getRecordDecl().get(), app);
                names.addAll(owner.scope()); names.add(owner.name());
            }
        }
        Linkage linkage = node.hasValue(NamedDecl.LINKAGE) ? node.get(NamedDecl.LINKAGE) : Linkage.Invalid;
        boolean internal = linkage == Linkage.Internal || linkage == Linkage.UniqueExternal
                || (node.hasValue(NamedDecl.USR) && node.get(NamedDecl.USR_IS_TU_LOCAL));
        if (node instanceof FunctionDecl function && !(function instanceof CXXMethodDecl))
            internal |= function.get(FunctionDecl.STORAGE_CLASS) == StorageClass.Static;
        if (node instanceof VarDecl variable) {
            internal |= variable.get(VarDecl.STORAGE_CLASS) == StorageClass.Static && !variable.get(VarDecl.IS_STATIC_DATA_MEMBER);
            // C++ namespace const objects have internal linkage unless external linkage was explicitly established.
            if (!node.hasValue(NamedDecl.USR) && linkage == Linkage.Invalid && language == LanguageLinkage.CXX
                    && variable.getAncestorTry(FunctionDecl.class).isEmpty() && variable.getType().isConst()
                    && variable.get(VarDecl.STORAGE_CLASS) != StorageClass.Extern) internal = true;
        }
        for (var parent = node.getParent(); parent != null; parent = parent.getParent())
            if (parent instanceof NamespaceDecl namespace && namespace.getDeclName().isEmpty()) internal = true;
        Object domain;
        var functionOwner = node.getAncestorTry(FunctionDecl.class);
        boolean blockExtern = node instanceof VarDecl variable && variable.get(VarDecl.STORAGE_CLASS) == StorageClass.Extern;
        if (functionOwner.isPresent() && !blockExtern) {
            ClavaNode block = node.getParent() == null ? functionOwner.get() : node.getParent();
            domain = new LexicalDomain(new Reference(block));
        } else if (linkage == Linkage.Module || node.get(Decl.IS_MODULE_PRIVATE)) {
            domain = new ModuleDomain(node.hasValue(NamedDecl.USR) ? PostEditBindings.identity(node) : new Reference(node));
        } else if ((linkage == Linkage.None || linkage == Linkage.VisibleNone) && node.hasValue(NamedDecl.USR)) {
            domain = new LexicalDomain(new Reference(node));
        } else if (internal) {
            String path = origin(node);
            domain = path == null ? new LexicalDomain(new Reference(node)) : new TranslationUnitDomain(path);
        } else if (linkage == Linkage.External || node.getAncestorTry(TranslationUnit.class).isPresent()
                || node.getParent() instanceof NamespaceDecl) domain = "external";
        else if (node instanceof CXXMethodDecl method && method.getRecordDecl().isPresent())
            domain = symbol(method.getRecordDecl().get(), app).domain();
        else domain = new LexicalDomain(new Reference(node));
        if (language == LanguageLinkage.C && domain.equals("external")
                && (family == Family.FUNCTION || family == Family.OBJECT)) names.clear();
        return new Symbol(family, names, node.getDeclName(), domain, language);
    }
    public static boolean isGlobalDefinition(VarDecl variable) {
        if (variable.getAncestorTry(FunctionDecl.class).isPresent() || variable instanceof ParmVarDecl) return false;
        if (variable.get(VarDecl.IS_STATIC_DATA_MEMBER) && variable.getAncestorTry(CXXRecordDecl.class).isPresent()) return false;
        boolean global = variable.get(VarDecl.HAS_GLOBAL_STORAGE) || variable.getParent() instanceof TranslationUnit
                || variable.getParent() instanceof NamespaceDecl;
        return global && (variable.hasInit() || variable.get(VarDecl.STORAGE_CLASS) != StorageClass.Extern);
    }
}
