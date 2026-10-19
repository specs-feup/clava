/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;

/** Desired semantics, deliberately opt-in until the findings in docs/cross-tu-review.md are decided. */
@Tag("cross-tu-review")
@EnabledIfSystemProperty(named = "clava.crossTuReview", matches = "true")
class CrossTuReviewTest extends CrossTuTestSupport {
    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU03_anonymousNamespaceProjectRemainsParseable(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "namespace{int helper(){return 1;}} int a(){return helper();}",
                "b.cpp", "namespace{int helper(){return 2;}} int b(){return helper();}"));
        for (var call : app.getDescendants(CallExpr.class)) {
            assertEquals(file(call), file(call.getDefinition().orElseThrow()), "Unnamed namespace functions have internal linkage");
        }
    }


}
