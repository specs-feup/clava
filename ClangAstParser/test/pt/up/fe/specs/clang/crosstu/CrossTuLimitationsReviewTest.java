/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.utils.DeclarationIdentity;
import pt.up.fe.specs.clava.utils.DefinitionResolution;

/** Opt-in probes for limitations outside the approved behavior fixes. */
@Tag("cross-tu-review")
@EnabledIfSystemProperty(named = "clava.crossTuReview", matches = "true")
class CrossTuLimitationsReviewTest extends CrossTuTestSupport {
    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU19_macroConfiguredInlineDefinitionsExposeTheCacheCoalescingRisk(Mode mode) throws Exception {
        var app = parse(mode, sources(
                "api.hpp", "inline int get(){return VALUE;}",
                "a.cpp", "#define VALUE 1\n#include \"api.hpp\"\nint a(){return get();}",
                "b.cpp", "#define VALUE 2\n#include \"api.hpp\"\nint b(){return get();}"));

        var treeDefinitions = app.getDescendants(FunctionDecl.class).stream()
                .filter(function -> function.getDeclName().equals("get") && function.hasBody())
                .toList();
        var aCall = unique(app, CallExpr.class, call -> file(call).equals("a.cpp"));
        var bCall = unique(app, CallExpr.class, call -> file(call).equals("b.cpp"));
        var aContext = directCallee(aCall);
        var bContext = directCallee(bCall);
        String aBody = body(aContext);
        String bBody = body(bContext);

        // The native parser retains the included definitions on the call targets, not as TU tree children.
        assertTrue(treeDefinitions.isEmpty(), "The setup assumes included header declarations are detached");
        assertTrue(aBody.contains("return 1;"), () -> "a.cpp AST body: " + aBody);
        assertTrue(bBody.contains("return 2;"), () -> "b.cpp AST body: " + bBody);

        var nativeAResolution = app.getFunctionDefinitionResolution(aContext);
        var nativeBResolution = app.getFunctionDefinitionResolution(bContext);
        assertEquals(DefinitionResolution.Kind.MISSING, nativeAResolution.kind());
        assertEquals(DefinitionResolution.Kind.MISSING, nativeBResolution.kind());

        // Model a later AST mutation that inserts the retained header definitions into their owning TUs.
        // This isolates and exercises the Definitions.add() same-location coalescing rule.
        var aTreeCopy = (FunctionDecl) aContext.deepCopy();
        var bTreeCopy = (FunctionDecl) bContext.deepCopy();
        translationUnit(app, "a.cpp").addChild(aTreeCopy);
        translationUnit(app, "b.cpp").addChild(bTreeCopy);
        app.clearCache();

        var aResolution = app.getFunctionDefinitionResolution(aContext);
        var bResolution = app.getFunctionDefinitionResolution(bContext);
        var aResolved = aCall.getDefinition().orElseThrow();
        var bResolved = bCall.getDefinition().orElseThrow();

        String evidence = "native detached bodies: " + describe(aContext) + " and " + describe(bContext)
                + "; after attaching deep copies, resolutions are a.cpp=" + describe(aResolution)
                + ", b.cpp=" + describe(bResolution) + "; call definitions are " + describe(aResolved)
                + " and " + describe(bResolved);

        assertAll(evidence,
                () -> assertEquals(DefinitionResolution.Kind.AMBIGUOUS, aResolution.kind()),
                () -> assertEquals(DefinitionResolution.Kind.AMBIGUOUS, bResolution.kind()),
                () -> assertEquals(body(aContext), body(aResolved)),
                () -> assertEquals(body(bContext), body(bResolved)),
                () -> assertNotEquals(body(aContext), body(bContext)));
    }

    private static FunctionDecl directCallee(CallExpr call) {
        return call.get(CallExpr.DIRECT_CALLEE).orElseThrow();
    }

    private static TranslationUnit translationUnit(App app, String tuFile) {
        return app.getTranslationUnits().stream()
                .filter(translationUnit -> translationUnit.getFile().getName().equals(tuFile))
                .findFirst().orElseThrow();
    }

    private static String body(FunctionDecl function) {
        return function.getBody().orElseThrow().getCode();
    }

    private static String describe(FunctionDecl function) {
        return DeclarationIdentity.origin(function) + " " + body(function).replaceAll("\\s+", " ").trim();
    }

    private static String describe(DefinitionResolution<FunctionDecl> resolution) {
        List<String> candidates = resolution.candidates().stream()
                .map(CrossTuLimitationsReviewTest::describe)
                .toList();
        return resolution.kind() + " " + candidates;
    }
}
