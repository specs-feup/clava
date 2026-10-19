/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;

/** Approved declaration identity corrections, exercised in every ordinary regression run. */
class CrossTuApprovedRegressionTest extends CrossTuTestSupport {
    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU16_generatedGlobalReferencesFindTheirDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int a(){return 1;}", "b.cpp", "int b(){return 2;}"));
        var firstTu = app.getTranslationUnits().get(0);
        var secondTu = app.getTranslationUnits().get(1);
        var factory = app.getFactory();
        var definition = app.getGlobalManager().addGlobal(firstTu, "shared", factory.builtinType("int"), factory.integerLiteral(7));
        var declaration = app.getGlobalManager().addGlobal(secondTu, "shared", factory.builtinType("int"), factory.integerLiteral(7));
        app.clearCache();
        assertSame(definition, declaration.getDefinition(), "A generated extern global must resolve just like a parsed extern global");
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU01_freeFunctionsRespectNamespaces(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "namespace A{int run();} int use(){return A::run();}",
                "b.cpp", "namespace B{int run(){return 2;}}"));
        assertTrue(function(app, "run", "a.cpp").getImplementation().isEmpty(), "A::run must not resolve to B::run");
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU02_staticFunctionsRespectFullTuPaths(Mode mode) throws Exception {
        var app = parse(mode, sources("left/unit.cpp", "static int helper(){return 1;} int left(){return helper();}",
                "right/unit.cpp", "static int helper(){return 2;} int right(){return helper();}"));
        for (var call : app.getDescendants(CallExpr.class)) {
            assertSame(call.getAncestor(pt.up.fe.specs.clava.ast.extra.TranslationUnit.class),
                    call.getDefinition().orElseThrow().getAncestor(pt.up.fe.specs.clava.ast.extra.TranslationUnit.class),
                    "A static function must resolve within its own TU even when filenames match");
        }
    }


    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU04_refQualifiedMethodsStaySeparate(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct R{int get() &; int get() &&;};",
                "use.cpp", "#include \"api.hpp\"\nint use(R&r){return r.get()+static_cast<R&&>(r).get();}",
                "impl.cpp", "#include \"api.hpp\"\nint R::get() &{return 1;} int R::get() &&{return 2;}"));
        var definitions = app.getDescendants(CallExpr.class).stream().map(call -> call.getDefinition().orElseThrow()).toList();
        assertEquals(2, definitions.size());
        assertNotSame(definitions.get(0), definitions.get(1), "Lvalue and rvalue overloads must have distinct definitions");
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU05_volatileQualifiedMethodsStaySeparate(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct R{int get(); int get() volatile;};",
                "use.cpp", "#include \"api.hpp\"\nint use(R&r,volatile R&v){return r.get()+v.get();}",
                "impl.cpp", "#include \"api.hpp\"\nint R::get(){return 1;} int R::get() volatile{return 2;}"));
        var definitions = app.getDescendants(CallExpr.class).stream().map(call -> call.getDefinition().orElseThrow()).toList();
        assertEquals(2, definitions.size());
        assertNotSame(definitions.get(0), definitions.get(1), "Volatile overloads must have distinct definitions");
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU06_variadicOverloadsStaySeparate(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int choose(int); int use(){return choose(1);}",
                "b.cpp", "int choose(int,...){return 2;}"));
        assertTrue(function(app, "choose", "a.cpp").getImplementation().isEmpty(),
                "choose(int) must not resolve to the different overload choose(int,...)");
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU07_recordsRespectNamespaces(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "namespace A{struct Item;} A::Item *p;",
                "b.cpp", "namespace B{struct Item{int value;};}"));
        assertTrue(named(app, CXXRecordDecl.class, "Item", "a.cpp").getDefinition().isEmpty(),
                "A::Item must not acquire B::Item's definition");
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU08_globalsRespectNamespaces(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "namespace A{extern int value;} int use(){return A::value;}",
                "b.cpp", "namespace B{int value=2;}"));
        assertNull(named(app, VarDecl.class, "value", "a.cpp").getDefinition(), "A::value must not resolve to B::value");
    }


    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU10_externWithInitializerIsADefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern int value; int use(){return value;}",
                "b.cpp", "extern int value=2;"));
        assertSame(named(app, VarDecl.class, "value", "b.cpp"), named(app, VarDecl.class, "value", "a.cpp").getDefinition(),
                "extern int value=2 is an external definition");
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU11_internalConstDoesNotDefineAnExternalGlobal(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern const int value; int use(){return value;}",
                "b.cpp", "const int value=2;"));
        assertNull(named(app, VarDecl.class, "value", "a.cpp").getDefinition(),
                "A namespace-scope const without extern has internal linkage in C++");
    }



}
