/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateKind;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.App;

/** Regression cases for choosing a primary template or an explicit specialization across TUs. */
class CrossTuTemplateRegressionTest extends CrossTuTestSupport {
    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void implicitInstantiationKeepsTheGenericSourceTemplate(Mode mode) throws Exception {
        var app = parse(mode, sources(
                "use.cpp", "template<class T> T target(T); int use(){return target(1);}",
                "impl.cpp", "template<class T> T target(T value){return value + 1;}"));

        var call = unique(app, CallExpr.class, node -> file(node).equals("use.cpp"));
        var primary = primaryTemplate(app, "impl.cpp");
        var primaryDeclaration = unique(app, FunctionDecl.class, function -> file(function).equals("use.cpp")
                && function.get(FunctionDecl.TEMPLATE_KIND) == TemplateKind.FUNCTION_TEMPLATE);

        assertSame(primaryDeclaration, call.getFunctionDecl().orElseThrow(),
                "Implicit instantiations must keep exposing the generic source template to transformations");
        assertSame(primary, call.getDefinition().orElseThrow());
        assertSame(primary, call.get(CallExpr.DIRECT_CALLEE).orElseThrow().getImplementation().orElseThrow(),
                "Direct implicit-specialization lookups must also resolve through the primary source template");
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void explicitSpecializationDoesNotFallBackToPrimaryWhenParserDropsIt(Mode mode) throws Exception {
        var app = parse(mode, sources(
                "use.cpp", "template<class T> T target(T);\n"
                        + "template<> int target<int>(int);\n"
                        + "int use(){return target(1);}",
                "impl.cpp", "template<class T> T target(T value){return value + 10;}\n"
                        + "template<> int target<int>(int value){return value + 100;}"));

        var call = unique(app, CallExpr.class, node -> file(node).equals("use.cpp"));
        var primary = primaryTemplate(app, "impl.cpp");

        // The parser currently removes specialization declarations before building the App index.
        // A missing explicit body is safe; borrowing the generic primary body is not.
        assertFalse(call.getDefinition().isPresent(),
                "An explicit specialization must not silently resolve to the primary template body");
        assertTrue(primary.hasBody(), "The fixture must contain a primary body that would expose an unsafe fallback");
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void explicitSpecializationDoesNotHijackOtherPrimaryInstantiations(Mode mode) throws Exception {
        var app = parse(mode, sources(
                "use.cpp", "template<class T> T target(T);\n"
                        + "template<> int target<int>(int);\n"
                        + "int use(){return target(3) + target(4L);}",
                "impl.cpp", "template<class T> T target(T value){return value + 10;}\n"
                        + "template<> int target<int>(int value){return value + 100;}"));

        List<CallExpr> calls = app.getDescendants(CallExpr.class).stream()
                .filter(call -> file(call).equals("use.cpp")).toList();
        assertEquals(2, calls.size());
        var intCall = calls.stream().filter(call -> call.getArgs().get(0).getType().getCode().equals("int"))
                .findFirst().orElseThrow();
        var longCall = calls.stream().filter(call -> call.getArgs().get(0).getType().getCode().equals("long"))
                .findFirst().orElseThrow();

        assertFalse(intCall.getDefinition().isPresent(),
                "The deleted explicit-specialization body must not be replaced by the generic primary body");
        assertSame(primaryTemplate(app, "impl.cpp"), longCall.getDefinition().orElseThrow());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void zeroParameterFunctionSpecializationsWithDifferentIntegralArgumentsStayDistinct(Mode mode) throws Exception {
        var app = parse(mode, sources(
                "both.cpp", "template<int N> int target();\n"
                        + "template<> int target<1>(){return 101;}\n"
                        + "template<> int target<2>(){return 202;}\n"
                        + "int first(){return target<1>();}\n"
                        + "int second(){return target<2>();}"));

        var calls = app.getDescendants(CallExpr.class).stream().filter(call -> file(call).equals("both.cpp")).toList();
        assertEquals(2, calls.size());
        var oneCall = calls.stream().filter(call -> call.getAncestor(FunctionDecl.class).getDeclName().equals("first"))
                .findFirst().orElseThrow();
        var twoCall = calls.stream().filter(call -> call.getAncestor(FunctionDecl.class).getDeclName().equals("second"))
                .findFirst().orElseThrow();
        var oneDefinition = oneCall.getDefinition().orElseThrow();
        var twoDefinition = twoCall.getDefinition().orElseThrow();

        assertNotSame(oneDefinition, twoDefinition,
                "No-parameter function templates still need their template arguments in declaration identity");
        assertEquals(2, oneDefinition.getLocation().getStartLine());
        assertEquals(3, twoDefinition.getLocation().getStartLine());
    }

    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void explicitNoParameterSpecializationsFailClosedAcrossTus(Mode mode) throws Exception {
        var app = parse(mode, sources(
                "use.cpp", "template<int N> int target();\n"
                        + "template<> int target<1>();\n"
                        + "template<> int target<2>();\n"
                        + "int use(){return target<1>() + target<2>();}",
                "impl.cpp", "template<int N> int target(){return N;}\n"
                        + "template<> int target<1>(){return 101;}\n"
                        + "template<> int target<2>(){return 202;}"));

        var calls = app.getDescendants(CallExpr.class).stream().filter(call -> file(call).equals("use.cpp")).toList();
        assertEquals(2, calls.size());
        for (var call : calls) {
            assertFalse(call.getDefinition().isPresent(),
                    "The parser removes explicit specialization nodes before indexing, so a call must not borrow "
                            + "the primary template body as a fallback");
        }
    }

    private static FunctionDecl primaryTemplate(App app, String file) {
        return unique(app, FunctionDecl.class, function -> file(function).equals(file)
                && function.get(FunctionDecl.TEMPLATE_KIND) == TemplateKind.FUNCTION_TEMPLATE
                && function.hasBody());
    }

}
