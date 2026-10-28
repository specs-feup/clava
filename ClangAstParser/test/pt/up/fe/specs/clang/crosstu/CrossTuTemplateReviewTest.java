/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;

/** Opt-in probe for preserving and resolving explicit function specialization definitions across TUs. */
@Tag("cross-tu-review")
@EnabledIfSystemProperty(named = "clava.crossTuReview", matches = "true")
class CrossTuTemplateReviewTest extends CrossTuTestSupport {
    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU18_explicitSpecializationDefinitionSurvivesAndResolvesAcrossTus(Mode mode) throws Exception {
        var app = parse(mode, sources(
                "use.cpp", "template<class T> T target(T);\n"
                        + "template<> int target<int>(int);\n"
                        + "int use(){return target(1);}",
                "impl.cpp", "template<class T> T target(T value){return value + 10;}\n"
                        + "template<> int target<int>(int value){return value + 100;}"));

        // Line 2 is the explicit specialization in impl.cpp. Check its source body directly rather than
        // requiring primary-template metadata, which the current parser path does not reliably preserve.
        List<FunctionDecl> explicitDefinitions = app.getDescendants(FunctionDecl.class).stream()
                .filter(function -> file(function).equals("impl.cpp")
                        && function.getLocation().getStartLine() == 2 && function.hasBody())
                .toList();
        assertEquals(1, explicitDefinitions.size(),
                "The explicit specialization body on impl.cpp:2 must survive parsing; "
                        + "DeleteTemplateSpecializations currently removes specialization declarations from the App tree");

        var call = unique(app, CallExpr.class, node -> file(node).equals("use.cpp"));
        var primary = unique(app, FunctionDecl.class, function -> file(function).equals("impl.cpp")
                && function.getLocation().getStartLine() == 1 && function.hasBody());
        assertNotSame(primary, explicitDefinitions.get(0));
        assertSame(explicitDefinitions.get(0), call.getDefinition().orElseThrow());
    }
}
