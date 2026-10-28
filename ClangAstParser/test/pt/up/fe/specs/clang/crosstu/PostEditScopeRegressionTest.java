/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.decl.enums.*;
import pt.up.fe.specs.clava.ast.extra.*;
import pt.up.fe.specs.clava.utils.*;
import pt.up.fe.specs.clava.utils.DefinitionResolution.Kind;

class PostEditScopeRegressionTest extends CrossTuTestSupport {
    private FunctionDecl generatedCopy(FunctionDecl original) {
        var copy = (FunctionDecl) original.copy();
        CompilerEntityIdentity.makeGeneratedTree(copy);
        copy.getBody().ifPresent(ClavaNode::detach);
        return copy;
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void namespaceQualificationSeparatesSameSpelledFunctions(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "namespace A{int target(){return 1;}} namespace B{int target(){return 2;}}"));
        for (var namespace : app.getDescendants(NamespaceDecl.class)) {
            var target = namespace.getDescendants(FunctionDecl.class).get(0);
            var query = generatedCopy(target);
            namespace.addChild(query);
            assertSame(target, query.getImplementation().orElseThrow());
        }
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void methodsPreserveRecordAndCvRefOwners(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "struct A{int target() &{return 1;} int target() const &{return 2;} int target() &&{return 3;}};"
                + "struct B{int target() &{return 4;}};"));
        var targets = List.copyOf(app.getDescendants(CXXMethodDecl.class));
        assertEquals(4, targets.size());
        for (var target : targets) {
            var query = generatedCopy(target);
            target.getParent().addChild(query);
            assertSame(target, query.getImplementation().orElseThrow());
            query.detach();
        }
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void internalGeneratedFunctionOnlyBindsWithinItsTranslationUnit(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "static int target(){return 1;}", "b.cpp", "static int target(){return 2;}"));
        for (var unit : app.getTranslationUnits()) {
            var target = function(app, "target", unit.getFilename());
            var query = generatedCopy(target);
            unit.addChild(query);
            assertSame(target, query.getImplementation().orElseThrow());
            var other = app.getTranslationUnits().stream().filter(tu -> tu != unit).findFirst().orElseThrow();
            query.detach(); other.addChild(query);
            assertSame(other.getDescendants(FunctionDecl.class).stream().filter(FunctionDecl::hasBody).findFirst().orElseThrow(), query.getImplementation().orElseThrow());
            query.detach();
        }
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void returnTypeCollisionIsNotAnOverload(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(int x){return x;} double other(int x){return x;}"));
        var query = generatedCopy(function(app, "other", "a.cpp"));
        query.setDeclName("target");
        app.getTranslationUnits().get(0).addChild(query);
        assertEquals(Kind.INCOMPATIBLE, app.getFunctionDefinitionResolution(query).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void variadicSignaturesRemainDistinct(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(int x){return x;} int other(int x,...){return x;}"));
        var query = generatedCopy(function(app, "other", "a.cpp"));
        query.setDeclName("target");
        app.getTranslationUnits().get(0).addChild(query);
        assertEquals(Kind.MISSING, app.getFunctionDefinitionResolution(query).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void nominalRecordsDoNotBecomeEqualThroughLayout(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "struct A{int x;}; struct B{int x;}; int target(A*){return 1;} int other(B*){return 2;}"));
        var query = generatedCopy(function(app, "other", "a.cpp"));
        query.setDeclName("target");
        app.getTranslationUnits().get(0).addChild(query);
        assertEquals(Kind.MISSING, app.getFunctionDefinitionResolution(query).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void generatedExternCRequiresTheCorrectLanguageLinkage(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern \"C\" int target(){return 1;} int other(){return 2;}"));
        var target = function(app, "target", "a.cpp");
        var query = generatedCopy(target);
        app.getTranslationUnits().get(0).addChild(query);
        assertEquals(Kind.MISSING, app.getFunctionDefinitionResolution(query).kind(), "A generated C++ declaration must not adopt historical C metadata");
        query.detach(); target.getParent().addChild(query);
        assertSame(target, query.getImplementation().orElseThrow());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void tlsMismatchIsAConflict(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "thread_local int shared=1;"));
        var query = app.getFactory().varDecl("shared", app.getFactory().builtinType("int"));
        query.set(VarDecl.STORAGE_CLASS, StorageClass.Extern);
        app.getTranslationUnits().get(0).addChild(query);
        assertEquals(Kind.INCOMPATIBLE, app.getGlobalVarDefinitionResolution(query).kind());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void localObjectsAndLocalRecordTypesCannotBindAcrossFunctions(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int a(){struct R{int x;}; R local; return 1;} int b(){struct R{int x;}; R local;return 2;}"));
        var locals = app.getDescendants(VarDecl.class).stream().filter(v -> v.getDeclName().equals("local")).toList();
        assertEquals(2, locals.size());
        assertEquals(TypeSemantics.Verdict.INCOMPATIBLE, new TypeSemantics().compare(locals.get(0).getType(), locals.get(1).getType(), false));
        for (var local : locals) assertSame(local, local.getDefinition());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void aSecondAppCannotSatisfyTheFirstAppsGeneratedQuery(Mode mode) throws Exception {
        var first = parse(mode, sources("a.cpp", "int target(){return 1;}"));
        var second = parse(mode, sources("b.cpp", "int target(){return 2;}"));
        var definition = function(first, "target", "a.cpp");
        var query = generatedCopy(definition);
        first.getTranslationUnits().get(0).addChild(query);
        assertSame(definition, query.getImplementation().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> first.getGlobalManager().addGlobal(second.getTranslationUnits().get(0), "shared",
                first.getFactory().builtinType("int"), first.getFactory().integerLiteral(1)));
    }
}
