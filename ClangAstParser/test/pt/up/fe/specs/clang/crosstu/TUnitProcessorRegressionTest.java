/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pt.up.fe.specs.clang.codeparser.TUnitProcessor;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;

/** Deterministic graph tests independent of the native dumper and its traversal order. */
class TUnitProcessorRegressionTest extends TUnitProcessorTestSupport {
    @Test void directOptionalAndListReferencesResolveToTheTreeObject() {
        var canonical = variable("value", "/project/header.hpp", 1);
        var detached = variable("value", "/project/header.hpp", 1);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);
        owner.set(Holder.OPTIONAL, Optional.of(detached));
        owner.set(Holder.NODES, new ArrayList<>(List.of(detached, detached)));
        new TUnitProcessor(List.of(data("header.cpp", List.of(canonical)), data("use.cpp", List.of(owner), owner)), true)
                .getTranslationUnits();
        assertSame(canonical, owner.get(Holder.DIRECT));
        assertSame(canonical, owner.get(Holder.OPTIONAL).orElseThrow());
        assertEquals(2, owner.get(Holder.NODES).size());
        owner.get(Holder.NODES).forEach(node -> assertSame(canonical, node));
        assertFalse(detached.hasParent());
        assertEquals("header.cpp", canonical.getAncestor(TranslationUnit.class).getFile().getName());
    }

    @Test void disablingNormalizationLeavesReferencesUntouched() {
        var canonical = variable("value", "/p/header.hpp", 1);
        var detached = variable("value", "/p/header.hpp", 1);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);
        var tus = new TUnitProcessor(List.of(data("header.cpp", List.of(canonical)),
                data("use.cpp", List.of(owner), owner)), false).getTranslationUnits();
        assertEquals(2, tus.size());
        assertSame(detached, owner.get(Holder.DIRECT));
    }

    @Test void invalidLocationsStayUntouched() {
        var canonical = variable("value", "/p/header.hpp", 1);
        var detached = variable("value", "/p/header.hpp", 1);
        detached.set(ClavaNode.LOCATION, SourceRange.invalidRange());
        var owner = holder();
        owner.set(Holder.DIRECT, detached);
        new TUnitProcessor(List.of(data("header.cpp", List.of(canonical)), data("use.cpp", List.of(owner), owner)), true)
                .getTranslationUnits();
        assertSame(detached, owner.get(Holder.DIRECT));
    }

    @Test void macroReferencesStayUntouched() {
        var canonical = variable("value", "/p/header.hpp", 1);
        var detached = variable("value", "/p/header.hpp", 1);
        detached.set(ClavaNode.IS_MACRO, true);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);
        new TUnitProcessor(List.of(data("header.cpp", List.of(canonical)), data("use.cpp", List.of(owner), owner)), true)
                .getTranslationUnits();
        assertSame(detached, owner.get(Holder.DIRECT));
    }

    @Test void initializerSemanticAndSyntacticFormsAreNotMerged() {
        var first = factory.node(InitListExpr.class);
        var second = factory.node(InitListExpr.class);
        first.set(ClavaNode.LOCATION, new SourceRange("/p/a.cpp", 1, 1, 1, 3));
        second.set(ClavaNode.LOCATION, first.getLocation());
        var root = holder();
        root.addChild(first);
        var owner = holder();
        owner.set(Holder.DIRECT, second);
        new TUnitProcessor(List.of(data("a.cpp", List.of(root)), data("b.cpp", List.of(owner), owner)), true)
                .getTranslationUnits();
        assertSame(second, owner.get(Holder.DIRECT));
    }

    @Test void ambiguousCandidatesNeverSelectAnArbitraryNode() {
        var first = variable("value", "/p/header.hpp", 1);
        var second = variable("value", "/p/header.hpp", 1);
        var third = variable("value", "/p/header.hpp", 1);
        var detached = variable("value", "/p/header.hpp", 1);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);
        new TUnitProcessor(List.of(data("a.cpp", List.of(first)), data("b.cpp", List.of(second)),
                data("c.cpp", List.of(third)), data("use.cpp", List.of(owner), owner)), true).getTranslationUnits();
        assertSame(detached, owner.get(Holder.DIRECT));
    }

    @Test void copyProvenancePreventsMergingAnEditedCopy() {
        var canonical = variable("value", "/p/header.hpp", 1);
        var copy = canonical.copy();
        var owner = holder();
        owner.set(Holder.DIRECT, copy);
        new TUnitProcessor(List.of(data("header.cpp", List.of(canonical)), data("use.cpp", List.of(owner), owner)), true)
                .getTranslationUnits();
        assertSame(copy, owner.get(Holder.DIRECT));
        assertNotEquals(canonical.getNodeSignature(), copy.getNodeSignature());
    }

    @Test void namesAndSourceCoordinatesDiscriminateCandidates() {
        var first = variable("first", "/p/header.hpp", 1);
        var second = variable("second", "/p/header.hpp", 1);
        var third = variable("first", "/p/header.hpp", 2);
        var detached = variable("first", "/p/header.hpp", 1);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);
        new TUnitProcessor(List.of(data("header.cpp", List.of(first, second, third)),
                data("use.cpp", List.of(owner), owner)), true).getTranslationUnits();
        assertSame(first, owner.get(Holder.DIRECT));
    }

    @Test void missingTargetsEmptyOptionalsAndNonNodeListsAreSafe() {
        var detached = variable("absent", "/p/header.hpp", 1);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);
        owner.set(Holder.OPTIONAL, Optional.empty());
        owner.set(Holder.NODES, List.of());
        owner.set(Holder.TEXT, List.of("unrelated", "text"));
        new TUnitProcessor(List.of(data("use.cpp", List.of(owner), owner)), true).getTranslationUnits();
        assertSame(detached, owner.get(Holder.DIRECT));
        assertTrue(owner.get(Holder.OPTIONAL).isEmpty());
        assertTrue(owner.get(Holder.NODES).isEmpty());
        assertEquals(List.of("unrelated", "text"), owner.get(Holder.TEXT));
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 7, 23, 101, 65537})
    void shuffledLargeReferenceGraphsPreserveEveryTargetAndListPosition(int seed) {
        int count = 1000;
        List<VarDecl> targets = IntStream.range(0, count).mapToObj(i -> variable("v" + i, "/p/header.hpp", i + 1)).toList();
        List<ClavaNode> references = new ArrayList<>(IntStream.range(0, count)
                .mapToObj(i -> (ClavaNode) variable("v" + i, "/p/header.hpp", i + 1)).toList());
        Collections.shuffle(references, new Random(seed));
        var owner = holder();
        owner.set(Holder.NODES, references);
        List<Decl> shuffledTargets = new ArrayList<>(targets);
        Collections.shuffle(shuffledTargets, new Random(seed + 1));
        var inputs = new ArrayList<>(List.of(data("header.cpp", shuffledTargets), data("use.cpp", List.of(owner), owner)));
        if ((seed & 1) != 0) { Collections.reverse(inputs); }
        new TUnitProcessor(inputs, true).getTranslationUnits();
        for (int i = 0; i < count; i++) {
            var old = (VarDecl) references.get(i);
            int index = Integer.parseInt(old.getDeclName().substring(1));
            assertSame(targets.get(index), owner.get(Holder.NODES).get(i), "seed=" + seed + ", position=" + i);
        }
        var normalized = new ArrayList<>(owner.get(Holder.NODES));
        new TUnitProcessor(inputs, true).getTranslationUnits();
        for (int i = 0; i < count; i++) { assertSame(normalized.get(i), owner.get(Holder.NODES).get(i)); }
    }
}
