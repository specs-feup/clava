/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.utils.*;
import pt.up.fe.specs.clava.utils.DefinitionResolution.Kind;

class PostEditTemplateRegressionTest extends CrossTuTestSupport {
    @ParameterizedTest @EnumSource(Mode.class)
    void newTemplatesCannotInferConstraintEquivalenceFromAFunctionType(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "template<class T> requires(sizeof(T)>1) int target(T x){return 1;}"));
        var target = function(app, "target", "a.cpp");
        assertEquals(Kind.UNIQUE, app.getFunctionDefinitionResolution(target).kind());
        var query = (FunctionDecl) target.copy();
        CompilerEntityIdentity.makeGeneratedTree(query);
        query.getBody().orElseThrow().detach();
        app.getTranslationUnits().get(0).addChild(query);
        assertEquals(Kind.UNSUPPORTED, app.getFunctionDefinitionResolution(query).kind());
        assertTrue(query.getImplementation().isEmpty());
    }
}
