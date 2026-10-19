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
import pt.up.fe.specs.clava.utils.DeclarationIdentity;

class CrossTuIdentityRegressionTest extends CrossTuTestSupport {
    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void externallyLinkedConstArrayCompletionFindsItsDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern const int values[]; int use(){return values[0];}",
                "b.cpp", "extern const int values[]={1,2,3};"));
        assertSame(named(app, VarDecl.class, "values", "b.cpp"), named(app, VarDecl.class, "values", "a.cpp").getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void threadLocalDeclarationsFindOnlyThreadLocalDefinitions(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern thread_local int value; int use(){return value;}",
                "b.cpp", "thread_local int value=1;"));
        assertSame(named(app, VarDecl.class, "value", "b.cpp"), named(app, VarDecl.class, "value", "a.cpp").getDefinition());
        var incompatible = parse(mode, sources("a.cpp", "extern thread_local int value; int use(){return value;}",
                "b.cpp", "int value=1;"));
        assertNull(named(incompatible, VarDecl.class, "value", "a.cpp").getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void addingMethodsPreservesCvAndRefOverloadsWithoutDuplicatingThem(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "struct Source{int get()&;int get()&&;int get()volatile&;int get()volatile&&;};struct Destination{};"));
        var source = named(app, CXXRecordDecl.class, "Source", "a.cpp");
        var destination = named(app, CXXRecordDecl.class, "Destination", "a.cpp");
        for (var method : source.getMethods()) destination.addMethod((CXXMethodDecl) method.deepCopy());
        assertEquals(4, destination.getMethods().size());
        for (var method : source.getMethods()) destination.addMethod((CXXMethodDecl) method.deepCopy());
        assertEquals(4, destination.getMethods().size());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void namespaceAliasesUseTheDeclarationScope(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "namespace Actual{int get();}namespace Alias=Actual;int use(){return Alias::get();}",
                "b.cpp", "namespace Actual{int get(){return 7;}}"));
        assertSame(function(app, "get", "b.cpp"), unique(app, CallExpr.class, call -> true).getDefinition().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void cppConstObjectCannotSupplyAnExternalMutableArray(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern double values[]; double use(){return values[0];}",
                "b.cpp", "const double values[]={1,2,3};"));
        assertNull(named(app, VarDecl.class, "values", "a.cpp").getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void cEmptyParameterDeclarationFindsItsZeroParameterDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("a.c", "int get(); int use(){return get();}", "b.c", "int get(){return 7;}"));
        assertSame(function(app, "get", "b.c"), unique(app, CallExpr.class, call -> true).getDefinition().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void incompatibleReturnTypeDoesNotSupplyADefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int value(); int use(){return value();}", "b.cpp", "double value(){return 1;}"));
        assertTrue(function(app, "value", "a.cpp").getImplementation().isEmpty());
        assertEquals(pt.up.fe.specs.clava.utils.DefinitionResolution.Kind.MISSING,
                app.getFunctionDefinitionResolution(function(app, "value", "a.cpp")).kind());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void equivalentNoexceptSpellingsInFunctionPointersMatch(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int apply(int(*)()noexcept); int callback()noexcept{return 7;} int use(){return apply(callback);}",
                "b.cpp", "int apply(int(*callback)()noexcept(true)){return callback();}"));
        var call = unique(app, CallExpr.class, candidate -> file(candidate).equals("a.cpp"));
        assertEquals("b.cpp", file(call.getDefinition().orElseThrow()));
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void throwingAndNonthrowingPointerOverloadsRemainDistinct(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int apply(int(*)()); int apply(int(*)()noexcept); int callback()noexcept{return 7;} int use(){return apply(callback);}",
                "b.cpp", "int apply(int(*callback)()){return 1;} int apply(int(*callback)()noexcept){return 2;}"));
        var call = unique(app, CallExpr.class, candidate -> file(candidate).equals("a.cpp"));
        assertTrue(call.getDefinition().orElseThrow().getBody().orElseThrow().getCode().contains("return 2;"));
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void incompleteExternArrayFindsItsCompletedDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern double values[]; double use(){return values[0];}",
                "b.cpp", "double values[3]={1,2,3};"));
        assertSame(named(app, VarDecl.class, "values", "b.cpp"), named(app, VarDecl.class, "values", "a.cpp").getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void incompatibleVariableTypesNeverSatisfyAnExtern(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "extern int value; int use(){return value;}", "b.cpp", "double value=1;"));
        assertNull(named(app, VarDecl.class, "value", "a.cpp").getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void cVariableLengthArrayParametersMatchAcrossTus(Mode mode) throws Exception {
        var app = parse(mode, sources("a.c", "int get(int n,int a[][n]); int use(int n,int(*a)[n]){return get(n,a);}",
                "b.c", "int get(int width,int values[][width]){return values[0][0];}"));
        assertSame(function(app, "get", "b.c"), unique(app, CallExpr.class, call -> true).getDefinition().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void generatedConstGlobalHasExternalDefinitionBeforeAndAfterReparse(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int a(){return 1;}", "b.cpp", "int b(){return 2;}"));
        var factory = app.getFactory();
        var first = app.getTranslationUnits().get(0);
        var second = app.getTranslationUnits().get(1);
        var definition = app.getGlobalManager().addGlobal(first, "shared", factory.builtinType("int").asConst(), factory.integerLiteral(7));
        var declaration = app.getGlobalManager().addGlobal(second, "shared", factory.builtinType("int").asConst(), factory.integerLiteral(7));
        assertSame(definition, declaration.getDefinition());
        assertTrue(definition.getCode().startsWith("extern "));
        var reparsed = parse(mode, sources(first.getFile().getName(), first.getCode(), second.getFile().getName(), second.getCode()));
        var declarations = reparsed.getDescendants(VarDecl.class).stream().filter(var -> var.getDeclName().equals("shared")).toList();
        assertEquals(2, declarations.size());
        var parsedDefinition = declarations.stream().filter(VarDecl::hasInit).findFirst().orElseThrow();
        assertSame(parsedDefinition, declarations.stream().filter(var -> !var.hasInit()).findFirst().orElseThrow().getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void namespaceDeclarationsJoinOnlyTheirOwnDefinitions(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "namespace A{int get(int);} namespace B{int get(int);} int use(){return A::get(1)+B::get(2);}",
                "impl.cpp", "namespace A{int get(int x){return 11;}} namespace B{int get(int x){return 22;}}"));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        var identity = new DeclarationIdentity(app);
        for (var call : calls) {
            var expected = call.getFunctionDecl().orElseThrow();
            var definition = call.getDefinition().orElseThrow();
            assertEquals(identity.function(expected), identity.function(definition));
            assertEquals("impl.cpp", file(definition));
        }
        assertNotSame(calls.get(0).getDefinition().orElseThrow(), calls.get(1).getDefinition().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void templateParameterNamesDoNotChangeFunctionIdentity(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "template<class T>T target(T); int use(){return target(1);}",
                "impl.cpp", "template<class U>U target(U x){return x;}"));
        assertEquals("impl.cpp", file(unique(app, CallExpr.class, call -> true).getDefinition().orElseThrow()));
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void topLevelParameterCvDoesNotChangeFunctionIdentity(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int value(const int); int pointer(int* const); int use(int*p){return value(1)+pointer(p);}",
                "impl.cpp", "int value(int x){return x;} int pointer(int*p){return *p;}"));
        assertEquals(2, app.getDescendants(CallExpr.class).size());
        for (var call : app.getDescendants(CallExpr.class)) assertEquals("impl.cpp", file(call.getDefinition().orElseThrow()));
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void arrayAndFunctionParametersAdjustToPointers(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int array(int[3]); int apply(int fn(int)); int local(int x){return x;} int use(int*p){return array(p)+apply(local);}",
                "impl.cpp", "int array(int*p){return *p;} int apply(int(*fn)(int)){return fn(1);}"));
        var calls = app.getDescendants(CallExpr.class).stream().filter(call -> file(call).equals("use.cpp")).toList();
        assertEquals(2, calls.size());
        for (var call : calls) assertEquals("impl.cpp", file(call.getDefinition().orElseThrow()));
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void pointeeCvAndReferenceCategoriesRemainDistinct(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int get(int*); int get(const int*); int ref(int&); int ref(int&&); int use(int*p,const int*q,int&r){return get(p)+get(q)+ref(r)+ref(1);}",
                "impl.cpp", "int get(int*p){return 1;} int get(const int*p){return 2;} int ref(int&r){return 3;} int ref(int&&r){return 4;}"));
        var definitions = app.getDescendants(CallExpr.class).stream().map(call -> call.getDefinition().orElseThrow()).toList();
        assertEquals(4, definitions.size());
        assertEquals(4, definitions.stream().map(FunctionDecl::getId).distinct().count());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void namespaceScopedRecordParameterTypesRemainDistinct(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "namespace A{struct Item{};} namespace B{struct Item{};} int get(A::Item*); int get(B::Item*);",
                "use.cpp", "#include \"api.hpp\"\nint use(A::Item*a,B::Item*b){return get(a)+get(b);}",
                "impl.cpp", "#include \"api.hpp\"\nint get(A::Item*){return 1;} int get(B::Item*){return 2;}"));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        assertNotSame(calls.get(0).getDefinition().orElseThrow(), calls.get(1).getDefinition().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void allMemberCvAndRefOverloadsResolveIndependently(Mode mode) throws Exception {
        StringBuilder header = new StringBuilder("struct R{");
        StringBuilder implementation = new StringBuilder("#include \"api.hpp\"\n");
        StringBuilder use = new StringBuilder("#include \"api.hpp\"\n");
        int index = 0;
        for (String cv : List.of("", "const", "volatile", "const volatile")) {
            for (String ref : List.of("&", "&&")) {
                header.append("int get() ").append(cv).append(' ').append(ref).append(';');
                implementation.append("int R::get() ").append(cv).append(' ').append(ref)
                        .append("{return ").append(index).append(";}\n");
                use.append("int use").append(index).append("(R&r){return static_cast<")
                        .append(cv).append(" R").append(ref).append(">(r).get();}\n");
                index++;
            }
        }
        header.append("};");
        var app = parse(mode, sources("api.hpp", header.toString(), "use.cpp", use.toString(), "impl.cpp", implementation.toString()));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(8, calls.size());
        for (var call : calls) {
            int expected = Integer.parseInt(call.getAncestor(FunctionDecl.class).getDeclName().substring(3));
            var definition = (CXXMethodDecl) call.getDefinition().orElseThrow();
            assertTrue(definition.getBody().orElseThrow().getCode().contains("return " + expected + ";"));
        }
        assertEquals(8, calls.stream().map(call -> call.getDefinition().orElseThrow().getId()).distinct().count());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void headerStaticFunctionsKeepTheirOriginatingTu(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "static int helper(){return 7;}",
                "a.cpp", "#include \"api.hpp\"\nint a(){return helper();}",
                "b.cpp", "#include \"api.hpp\"\nint b(){return helper();}"));
        var calls = app.getDescendants(CallExpr.class);
        assertEquals(2, calls.size());
        for (var call : calls) {
            assertEquals(DeclarationIdentity.origin(call), DeclarationIdentity.origin(call.getDefinition().orElseThrow()));
        }
        assertNotSame(calls.get(0).getFunctionDecl().orElseThrow(), calls.get(1).getFunctionDecl().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void blockExternFindsNamespaceScopeDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int use(){extern int value;return value;}", "b.cpp", "int value=7;"));
        assertSame(named(app, VarDecl.class, "value", "b.cpp"), named(app, VarDecl.class, "value", "a.cpp").getDefinition());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void conflictingDefinitionsRemainUnresolvedFromAPrototype(Mode mode) throws Exception {
        var app = parse(mode, sources("use.cpp", "int get(); int use(){return get();}",
                "a.cpp", "int get(){return 1;}", "b.cpp", "int get(){return 2;}"));
        assertTrue(function(app, "get", "use.cpp").getImplementation().isEmpty());
        assertTrue(unique(app, CallExpr.class, call -> true).getDefinition().isEmpty());
        var resolution = app.getFunctionDefinitionResolution(function(app, "get", "use.cpp"));
        assertEquals(pt.up.fe.specs.clava.utils.DefinitionResolution.Kind.AMBIGUOUS, resolution.kind());
        assertEquals(2, resolution.candidates().size());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void cAndCppLanguageLinkagesDoNotSupplyEachOthersDefinitions(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int get(); int use(){return get();}",
                "b.cpp", "extern \"C\" int get(){return 1;}"));
        assertTrue(function(app, "get", "a.cpp").getImplementation().isEmpty());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void cLanguageLinkageAcrossNamespaceDeclarationsDenotesOneFunction(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "namespace A{extern \"C\" int get();} int use(){return A::get();}",
                "b.cpp", "extern \"C\" int get(){return 1;}"));
        assertSame(function(app, "get", "b.cpp"), function(app, "get", "a.cpp").getImplementation().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void nestedRecordsAndClassStructRedeclarationsResolve(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "class Item; struct Outer{struct Inner;};",
                "b.cpp", "struct Item{}; struct Outer{struct Inner{};};"));
        assertEquals("b.cpp", file(named(app, CXXRecordDecl.class, "Item", "a.cpp").getDefinition().orElseThrow()));
        assertEquals("b.cpp", file(named(app, CXXRecordDecl.class, "Inner", "a.cpp").getDefinition().orElseThrow()));
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void staticDataMemberDeclarationFindsOutOfLineDefinition(Mode mode) throws Exception {
        var app = parse(mode, sources("api.hpp", "struct R{static int value;};",
                "a.cpp", "#include \"api.hpp\"\nint use(){return R::value;}",
                "b.cpp", "#include \"api.hpp\"\nint R::value=7;"));
        var reference = unique(app, pt.up.fe.specs.clava.ast.expr.DeclRefExpr.class,
                ref -> ref.getDeclaration().getDeclName().equals("value"));
        assertEquals("b.cpp", file(((VarDecl) reference.getDeclaration()).getDefinition()));
    }
}
