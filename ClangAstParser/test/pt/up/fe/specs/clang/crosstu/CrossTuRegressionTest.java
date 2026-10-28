/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;

/** Semantic assertions run against real native dumps, both TU orders and both parser scheduling modes. */
class CrossTuRegressionTest extends CrossTuTestSupport {
    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void multiplePrototypesResolveToOneDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("first.cpp", "int target(int); int first(){return target(1);}",
                "second.cpp", "int target(int); int second(){return target(2);}",
                "impl.cpp", "int target(int x){return x;}"));
        var implementation = function(app, "target", "impl.cpp");
        assertEquals(2, implementation.getPrototypes().size());
        for (var prototype : implementation.getPrototypes()) {
            assertSame(implementation, prototype.getImplementation().orElseThrow());
        }
        for (var call : app.getDescendants(CallExpr.class)) {
            assertSame(implementation, call.getDefinition().orElseThrow());
        }
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void pointerParameterOverloadsStaySeparate(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(int*); int target(double*); int use(int*a,double*b){return target(a)+target(b);}",
                "impl.cpp", "int target(int*x){return 1;} int target(double*x){return 2;}"));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        var first = calls.get(0).getDefinition().orElseThrow();
        var second = calls.get(1).getDefinition().orElseThrow();
        assertNotSame(first, second);
        assertEquals("impl.cpp", file(first));
        assertEquals("impl.cpp", file(second));
        for (var call : calls) {
            assertEquals(call.getFunctionDecl().orElseThrow().getSignature(), call.getDefinition().orElseThrow().getSignature());
        }
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void methodsInDifferentNamespacesStaySeparate(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "namespace A{struct R{int get();};} namespace B{struct R{int get();};}",
                "use.cpp", "#include \"api.hpp\"\nint use(A::R&a,B::R&b){return a.get()+b.get();}",
                "impl.cpp", "#include \"api.hpp\"\nint A::R::get(){return 1;} int B::R::get(){return 2;}"));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        var first = calls.get(0).getDefinition().orElseThrow();
        var second = calls.get(1).getDefinition().orElseThrow();
        assertNotSame(first, second);
        assertNotEquals(first.getSignature(), second.getSignature());
        for (var call : calls) {
            assertEquals(call.getFunctionDecl().orElseThrow().getSignature(), call.getDefinition().orElseThrow().getSignature());
        }
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void primaryFunctionTemplatesResolveAcrossTus(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "template<class T>T target(T); int use(){return target(1);}",
                "impl.cpp", "template<class T>T target(T x){return x;}"));
        var call = unique(app, CallExpr.class, node -> true);
        assertEquals("impl.cpp", file(call.getDefinition().orElseThrow()));
        assertEquals("target", call.getDefinition().orElseThrow().getDeclName());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void splitProjectKeepsLinksAfterCodeGenerationAndReparse(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(int); extern int total; int use(){return target(total);}",
                "impl.cpp", "int total=7; int target(int x){return x+1;}"));
        var output = java.nio.file.Files.createDirectories(temporary.resolve("generated"));
        var generated = app.write(output.toFile());
        assertEquals(2, generated.size());
        var regeneratedSources = new java.util.LinkedHashMap<String, String>();
        for (var source : generated) {
            regeneratedSources.put(source.getName(), java.nio.file.Files.readString(source.toPath()));
        }
        var reparsed = parse(mode, regeneratedSources);
        assertSame(function(reparsed, "target", "impl.cpp"),
                function(reparsed, "target", "use.cpp").getImplementation().orElseThrow());
        assertSame(named(reparsed, VarDecl.class, "total", "impl.cpp"),
                named(reparsed, VarDecl.class, "total", "use.cpp").getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void constructorsAndDestructorsResolveOutOfLine(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct R{R(); ~R();};",
                "use.cpp", "#include \"api.hpp\"\nvoid use(){R r;}",
                "impl.cpp", "#include \"api.hpp\"\nR::R(){} R::~R(){}"));
        var construction = unique(app, pt.up.fe.specs.clava.ast.expr.CXXConstructExpr.class, node -> true);
        var constructor = (FunctionDecl) construction.getDecl().orElseThrow();
        assertEquals("impl.cpp", file(constructor.getImplementation().orElseThrow()));
        var destructor = unique(app, pt.up.fe.specs.clava.ast.decl.CXXDestructorDecl.class, FunctionDecl::hasBody);
        assertSame(destructor, destructor.getImplementation().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void inheritedMethodResolvesToBaseImplementation(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct Base{int get();}; struct Derived:Base{};",
                "use.cpp", "#include \"api.hpp\"\nint use(Derived&d){return d.get();}",
                "impl.cpp", "#include \"api.hpp\"\nint Base::get(){return 1;}"));
        var call = unique(app, CallExpr.class, node -> true);
        var method = (CXXMethodDecl) call.getDefinition().orElseThrow();
        assertEquals("Base", method.getRecordDecl().orElseThrow().getDeclName());
        assertEquals("get", method.getDeclName());
        assertEquals("impl.cpp", file(call.getDefinition().orElseThrow()));
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void operatorOverloadFindsItsDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct R{int operator+(int) const;};",
                "use.cpp", "#include \"api.hpp\"\nint use(const R&r){return r+1;}",
                "impl.cpp", "#include \"api.hpp\"\nint R::operator+(int x) const{return x;}"));
        var call = unique(app, CallExpr.class, node -> true);
        assertEquals("impl.cpp", file(call.getDefinition().orElseThrow()));
        assertEquals("operator+", call.getDefinition().orElseThrow().getDeclName());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void indirectFunctionPointerCallsRemainUnresolved(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int use(int(*p)(int)){return p(1);}",
                "impl.cpp", "int target(int x){return x;}"));
        var call = unique(app, CallExpr.class, node -> true);
        assertTrue(call.getFunctionDecl().isEmpty());
        assertTrue(call.getDefinition().isEmpty());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void externalFunctionAndCallResolveAcrossTus(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(int); int use(){return target(7);}",
                "impl.cpp", "int target(int x){return x+1;}"));
        var prototype = function(app, "target", "use.cpp");
        var implementation = function(app, "target", "impl.cpp");
        assertSame(implementation, prototype.getImplementation().orElseThrow());
        assertTrue(implementation.getPrototypes().contains(prototype));
        var call = unique(app, CallExpr.class, node -> file(node).equals("use.cpp"));
        assertSame(implementation, call.getDefinition().orElseThrow());
        assertSame(implementation, prototype.canonical());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void parameterOverloadsStaySeparate(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int pick(int); int pick(double); int use(){return pick(1)+pick(1.5);}",
                "impl.cpp", "int pick(int x){return 11;} int pick(double x){return 22;}"));
        for (var prototype : app.getDescendants(FunctionDecl.class).stream()
                .filter(node -> node.getDeclName().equals("pick") && !node.hasBody()).toList()) {
            var implementation = prototype.getImplementation().orElseThrow();
            assertEquals(prototype.getSignature(), implementation.getSignature());
            assertEquals("impl.cpp", file(implementation));
        }
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        assertNotSame(calls.get(0).getDefinition().orElseThrow(), calls.get(1).getDefinition().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void typedefParameterMatchesItsUnderlyingType(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "typedef int Count; int target(Count); int use(){return target(2);}",
                "impl.cpp", "int target(int x){return x;}"));
        assertSame(function(app, "target", "impl.cpp"), function(app, "target", "use.cpp").getImplementation().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void staticFunctionsInDifferentlyNamedFilesStayLocal(Mode mode) throws Exception {
        var app = parse(mode, sources("left.cpp", "static int helper(){return 1;} int left(){return helper();}",
                "right.cpp", "static int helper(){return 2;} int right(){return helper();}"));
        for (var call : app.getDescendants(CallExpr.class)) {
            assertEquals(file(call), file(call.getDefinition().orElseThrow()));
        }
        assertNotEquals(function(app, "helper", "left.cpp").getSignature(),
                function(app, "helper", "right.cpp").getSignature());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void unresolvedExternalFunctionStaysUnresolved(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int absent(int); int use(){return absent(1);}",
                "other.cpp", "int unrelated(){return 9;}"));
        assertTrue(function(app, "absent", "use.cpp").getImplementation().isEmpty());
        assertTrue(unique(app, CallExpr.class, node -> true).getDefinition().isEmpty());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void methodsInDifferentClassesStaySeparate(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct A{int read();}; struct B{int read();};",
                "use.cpp", "#include \"api.hpp\"\nint use(A&a,B&b){return a.read()+b.read();}",
                "impl.cpp", "#include \"api.hpp\"\nint A::read(){return 1;} int B::read(){return 2;}"));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        var definitions = calls.stream().map(call -> call.getDefinition().orElseThrow()).toList();
        assertNotSame(definitions.get(0), definitions.get(1));
        assertTrue(definitions.stream().allMatch(node -> file(node).equals("impl.cpp")));
        assertNotEquals(definitions.get(0).getSignature(), definitions.get(1).getSignature());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void constMethodOverloadsStaySeparate(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct Counter{int read(); int read() const;};",
                "use.cpp", "#include \"api.hpp\"\nint use(Counter&a,const Counter&b){return a.read()+b.read();}",
                "impl.cpp", "#include \"api.hpp\"\nint Counter::read(){return 1;} int Counter::read() const{return 2;}"));
        var definitions = app.getDescendants(CallExpr.class).stream()
                .map(call -> (CXXMethodDecl) call.getDefinition().orElseThrow()).toList();
        assertEquals(2, definitions.size());
        assertNotSame(definitions.get(0), definitions.get(1));
        assertNotEquals(definitions.get(0).getFunctionType().isConst(), definitions.get(1).getFunctionType().isConst());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void globalExternFindsDefinitionAndIgnoresAutomaticLocal(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "extern int total; int use(){return total;}",
                "impl.cpp", "int total=9; int f(){int total=3;return total;}"));
        var declaration = named(app, VarDecl.class, "total", "use.cpp");
        var definition = unique(app, VarDecl.class, node -> node.getDeclName().equals("total")
                && file(node).equals("impl.cpp") && node.get(VarDecl.HAS_GLOBAL_STORAGE));
        assertSame(definition, declaration.getDefinition());
        var local = unique(app, VarDecl.class, node -> node.getDeclName().equals("total")
                && !node.get(VarDecl.HAS_GLOBAL_STORAGE));
        assertSame(local, local.getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void unresolvedGlobalRemainsNull(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "extern int absent; int use(){return absent;}",
                "impl.cpp", "int unrelated=1;"));
        assertNull(named(app, VarDecl.class, "absent", "use.cpp").getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void forwardRecordFindsDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "struct Record; Record *p;",
                "impl.cpp", "struct Record{int value;};"));
        var declaration = named(app, CXXRecordDecl.class, "Record", "use.cpp");
        var definition = named(app, CXXRecordDecl.class, "Record", "impl.cpp");
        assertSame(definition, declaration.getDefinition().orElseThrow());
        assertTrue(definition.getDeclarations().contains(declaration));
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void cFunctionsAndGlobalsResolveAcrossTus(Mode mode) throws Exception {
        var app = parse(mode, sources("use.c", "int target(int); extern int total; int use(void){return target(total);}",
                "impl.c", "int total=7; int target(int x){return x+1;}"));
        assertSame(function(app, "target", "impl.c"), function(app, "target", "use.c").getImplementation().orElseThrow());
        assertSame(named(app, VarDecl.class, "total", "impl.c"), named(app, VarDecl.class, "total", "use.c").getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void explicitCacheClearReflectsAstEdits(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int target(); int use(){return target();}",
                "impl.cpp", "int target(){return 1;}"));
        var prototype = function(app, "target", "use.cpp");
        var implementation = function(app, "target", "impl.cpp");
        assertSame(implementation, prototype.getImplementation().orElseThrow());
        implementation.detach();
        app.clearCache();
        assertTrue(prototype.getImplementation().isEmpty());
        app.getTranslationUnits().stream().filter(tu -> tu.getFile().getName().equals("impl.cpp"))
                .findFirst().orElseThrow().addChild(implementation);
        app.clearCache();
        assertSame(implementation, prototype.getImplementation().orElseThrow());
        assertEquals(List.of(prototype, implementation), prototype.getDecls());
    }


    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU09_localStaticDoesNotDefineAnExternalGlobal(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern int value; int use(){return value;}",
                "b.cpp", "int other(){static int value=2;return value;}"));
        assertNull(named(app, VarDecl.class, "value", "a.cpp").getDefinition(),
                "Static storage duration alone does not make a block variable a global definition");
    }


    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU12_staticMemberDeclarationFindsOutOfLineDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct R{static int get();};",
                "use.cpp", "#include \"api.hpp\"\nint use(){return R::get();}",
                "impl.cpp", "#include \"api.hpp\"\nint R::get(){return 1;}"));
        var call = unique(app, CallExpr.class, node -> true);
        assertEquals("impl.cpp", file(call.getDefinition().orElseThrow()),
                "A static member is not a TU-local free function");
    }
}
