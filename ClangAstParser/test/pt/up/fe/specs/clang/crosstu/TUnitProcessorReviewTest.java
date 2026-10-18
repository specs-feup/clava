/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pt.up.fe.specs.clang.codeparser.TUnitProcessor;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.VarDecl;

@Tag("cross-tu-review")
@EnabledIfSystemProperty(named = "clava.crossTuReview", matches = "true")
class TUnitProcessorReviewTest extends TUnitProcessorTestSupport {
    @Test void CTU13_normalizationDistinguishesFullSourcePaths() {
        var unrelated = variable("value", "/project/left/common.hpp", 1);
        var detached = variable("value", "/project/right/common.hpp", 1);
        unrelated.set(VarDecl.STORAGE_CLASS, pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Static);
        detached.set(VarDecl.STORAGE_CLASS, pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Static);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);
        new TUnitProcessor(List.of(data("left.cpp", List.of(unrelated)), data("right.cpp", List.of(owner), owner)), true)
                .getTranslationUnits();
        assertSame(detached, owner.get(Holder.DIRECT), "Matching basenames and coordinates do not imply matching files");
    }

    @Test void CTU14_rewritingAReferenceDoesNotInvalidateItsLookupKey() {
        var canonicalVariable = variable("value", "/project/header.hpp", 1);
        var detachedVariable = variable("value", "/project/header.hpp", 1);
        var treeReference = factory.declRefExpr(detachedVariable);
        var detachedReference = factory.declRefExpr(detachedVariable);
        var location = new SourceRange("/project/header.hpp", 2, 1, 2, 5);
        treeReference.set(ClavaNode.LOCATION, location);
        detachedReference.set(ClavaNode.LOCATION, location);
        assertEquals(treeReference.getNodeSignature(), detachedReference.getNodeSignature());
        var root = holder();
        root.addChild(treeReference);
        var owner = holder();
        owner.set(Holder.DIRECT, detachedReference);
        // The first registry rewrites detachedReference.DECL before the second registry queries detachedReference.
        new TUnitProcessor(List.of(data("header.cpp", List.of(canonicalVariable, root), detachedReference),
                data("use.cpp", List.of(owner), owner)), true).getTranslationUnits();
        assertSame(canonicalVariable, detachedReference.getDeclaration(), "The referenced declaration was normalized first");
        assertSame(treeReference, owner.get(Holder.DIRECT), "A key must remain consistent across indexing and reference rewriting");
    }

    public static class CountingVar extends VarDecl {
        static int renders;
        public CountingVar(org.suikasoft.jOptions.Interfaces.DataStore data,
                java.util.Collection<? extends ClavaNode> children) { super(data, children); }
        @Override public String toContentString() { renders++; return super.toContentString(); }
    }

    @Test void CTU15_ambiguityHandlingDoesNotEagerlyRenderFullNodeData() {
        var first = factory.node(CountingVar.class);
        var second = factory.node(CountingVar.class);
        for (var node : List.of(first, second)) {
            node.set(ClavaNode.LOCATION, new SourceRange("/project/header.hpp", 1, 1, 1, 8));
        }
        CountingVar.renders = 0;
        new TUnitProcessor(List.of(data("a.cpp", List.of(first)), data("b.cpp", List.of(second))), true)
                .getTranslationUnits();
        assertEquals(0, CountingVar.renders, "The collision path should retain compact data and render full diagnostics only on request");
    }
}
