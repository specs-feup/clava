/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;

/** Desired automatic coherence, outside the current explicit App.clearCache() contract. */
@Tag("cross-tu-review")
@EnabledIfSystemProperty(named = "clava.crossTuReview", matches = "true")
class CrossTuMutationReviewTest extends CrossTuTestSupport {
    @ParameterizedTest(name = "{displayName} [{arguments}]") @EnumSource(Mode.class)
    void CTU20_directRenameInvalidatesAnExistingIndex(Mode mode) throws Exception {
        var app = parse(mode, sources("a.cpp", "int target(); int use(){return target();}",
                "b.cpp", "int target(){return 7;}"));
        var prototype = function(app, "target", "a.cpp");
        var implementation = function(app, "target", "b.cpp");
        assertSame(implementation, prototype.getImplementation().orElseThrow());
        implementation.set(NamedDecl.DECL_NAME, "renamed");
        assertTrue(prototype.getImplementation().isEmpty(),
                "The existing index still maps target to a declaration now named renamed; a mutation revision is needed");
    }
}
