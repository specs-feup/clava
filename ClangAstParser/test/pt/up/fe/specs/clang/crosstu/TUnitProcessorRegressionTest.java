/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pt.up.fe.specs.clang.codeparser.TUnitProcessor;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.Linkage;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.util.collections.SpecsList;

/** Deterministic graph tests independent of the native dumper and its traversal order. */
class TUnitProcessorRegressionTest extends TUnitProcessorTestSupport {
    public static class OptionalAndListSignatureVar extends VarDecl {
        public static final DataKey<Optional<String>> OPTIONAL_SIGNATURE = KeyFactory.optional("optionalSignature");
        public static final DataKey<List<String>> LIST_SIGNATURE = KeyFactory.list("listSignature", String.class);

        public OptionalAndListSignatureVar(DataStore data, java.util.Collection<? extends ClavaNode> children) {
            super(data, children);
        }

        @Override
        public SpecsList<DataKey<?>> getSignatureKeys() {
            return super.getSignatureKeys().andAdd(OPTIONAL_SIGNATURE).andAdd(LIST_SIGNATURE);
        }
    }

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

    @Test void invalidCustomLocationsStayUntouched() {
        var canonical = variable("value", "/p/header.hpp", 1);
        var detached = variable("value", "/p/header.hpp", 1);
        detached.set(ClavaNode.LOCATION, SourceRange.newCustomLocation("synthetic:custom-node"));
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

    @Test void cyclicAmbiguousReferencesRemainUntouched() {
        var location = new SourceRange("/p/cycle.hpp", 1, 1, 1, 8);
        var first = holder();
        var second = holder();
        first.set(ClavaNode.LOCATION, location);
        second.set(ClavaNode.LOCATION, location);
        first.set(Holder.DIRECT, second);
        second.set(Holder.DIRECT, first);

        new TUnitProcessor(List.of(data("cycle.cpp", List.of(first, second), first, second)), true)
                .getTranslationUnits();

        assertSame(second, first.get(Holder.DIRECT));
        assertSame(first, second.get(Holder.DIRECT));
    }

    @Test void unchangedNodeListRetainsOriginalListObject() {
        var unrelated = variable("other", "/p/header.hpp", 1);
        var owner = holder();
        var originalNodes = new ArrayList<ClavaNode>(List.of(unrelated));
        owner.set(Holder.NODES, originalNodes);

        new TUnitProcessor(List.of(data("use.cpp", List.of(owner), owner)), true).getTranslationUnits();

        assertSame(originalNodes, owner.get(Holder.NODES));
    }

    @Test void normalizationUsesFullSourcePathsWithEqualBasenames() {
        var unrelated = variable("value", "/project/left/common.hpp", 1);
        var detached = variable("value", "/project/right/common.hpp", 1);
        unrelated.set(VarDecl.STORAGE_CLASS, StorageClass.Static);
        detached.set(VarDecl.STORAGE_CLASS, StorageClass.Static);
        unrelated.set(NamedDecl.LINKAGE, Linkage.Internal);
        detached.set(NamedDecl.LINKAGE, Linkage.Internal);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);

        new TUnitProcessor(List.of(data("use.cpp", List.of(unrelated, owner), detached, owner)), true)
                .getTranslationUnits();

        assertSame(detached, owner.get(Holder.DIRECT), "Matching basenames and coordinates do not imply matching files");
    }

    @Test void internalDeclarationsFromDifferentTranslationUnitsStaySeparate() {
        var canonical = variable("helper", "/project/header.hpp", 1);
        var detached = variable("helper", "/project/header.hpp", 1);
        canonical.set(NamedDecl.LINKAGE, Linkage.Internal);
        detached.set(NamedDecl.LINKAGE, Linkage.Internal);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);

        new TUnitProcessor(List.of(data("/project/left.cpp", List.of(canonical)),
                data("/project/right.cpp", List.of(owner), detached, owner)), true).getTranslationUnits();

        assertSame(detached, owner.get(Holder.DIRECT));
        assertEquals(Optional.of("/project/left.cpp"), canonical.get(ClavaNode.ORIGINATING_TRANSLATION_UNIT));
        assertEquals(Optional.of("/project/right.cpp"), detached.get(ClavaNode.ORIGINATING_TRANSLATION_UNIT));
    }

    @Test void nonDeclarationOccurrencesFromDifferentTranslationUnitsStaySeparate() {
        var canonical = holder();
        var detached = holder();
        var location = new SourceRange("/project/header.hpp", 1, 1, 1, 8);
        canonical.set(ClavaNode.LOCATION, location);
        detached.set(ClavaNode.LOCATION, location);
        var owner = holder();
        owner.set(Holder.DIRECT, detached);

        new TUnitProcessor(List.of(data("/project/left.cpp", List.of(canonical)),
                data("/project/right.cpp", List.of(owner), detached, owner)), true).getTranslationUnits();

        assertSame(detached, owner.get(Holder.DIRECT));
    }

    @Test void referenceKeysAreSnapshottedBeforeAnyReferenceIsRewritten() {
        var canonicalVariable = variable("value", "/project/header.hpp", 1);
        var detachedVariable = variable("value", "/project/header.hpp", 1);
        DeclRefExpr treeReference = factory.declRefExpr(detachedVariable);
        DeclRefExpr detachedReference = factory.declRefExpr(detachedVariable);
        var location = new SourceRange("/project/header.hpp", 2, 1, 2, 5);
        treeReference.set(ClavaNode.LOCATION, location);
        detachedReference.set(ClavaNode.LOCATION, location);
        assertEquals(treeReference.getNodeSignature(), detachedReference.getNodeSignature());

        var root = holder();
        root.addChild(treeReference);
        var owner = holder();
        var references = new ArrayList<ClavaNode>(List.of(detachedReference, detachedReference));
        owner.set(Holder.DIRECT, detachedReference);
        owner.set(Holder.OPTIONAL, Optional.of(detachedReference));
        owner.set(Holder.NODES, references);

        // The registered detached reference is rewritten before owner is visited.
        new TUnitProcessor(List.of(data("header.cpp", List.of(canonicalVariable, root), detachedReference),
                data("use.cpp", List.of(owner), owner)), true).getTranslationUnits();

        assertSame(canonicalVariable, detachedReference.getDeclaration(), "The declaration field is normalized first");
        assertSame(treeReference, owner.get(Holder.DIRECT));
        assertSame(treeReference, owner.get(Holder.OPTIONAL).orElseThrow());
        assertSame(treeReference, owner.get(Holder.NODES).get(0));
        assertSame(treeReference, owner.get(Holder.NODES).get(1));
        assertEquals(List.of(treeReference, treeReference), owner.get(Holder.NODES), "Repeated references keep order");
    }

    public static class CountingVar extends VarDecl {
        static int renders;
        public CountingVar(org.suikasoft.jOptions.Interfaces.DataStore data,
                java.util.Collection<? extends ClavaNode> children) { super(data, children); }
        @Override public String toContentString() { renders++; return super.toContentString(); }
    }

    @Test void ambiguousKeysAreRejectedWithoutRenderingNodeContents() {
        var first = factory.node(CountingVar.class);
        var second = factory.node(CountingVar.class);
        for (var node : List.of(first, second)) {
            node.set(ClavaNode.LOCATION, new SourceRange("/project/header.hpp", 1, 1, 1, 8));
            node.set(NamedDecl.LINKAGE, Linkage.External);
        }
        CountingVar.renders = 0;

        new TUnitProcessor(List.of(data("a.cpp", List.of(first)), data("b.cpp", List.of(second))), true)
                .getTranslationUnits();

        assertEquals(0, CountingVar.renders,
                "The collision path should retain compact identity data without rendering full node contents");
    }

    @Test void optionalAndListSignatureValuesAreSnapshottedStructurally() {
        var canonical = optionalSignatureVar(Optional.of("present"), List.of("left", "right"));
        var matching = optionalSignatureVar(Optional.of("present"), List.of("left", "right"));
        var reversedList = optionalSignatureVar(Optional.of("present"), List.of("right", "left"));
        var emptyOptional = optionalSignatureVar(Optional.empty(), List.of("left", "right"));
        var matchingOwner = holder();
        matchingOwner.set(Holder.DIRECT, matching);
        var reversedOwner = holder();
        reversedOwner.set(Holder.DIRECT, reversedList);
        var emptyOwner = holder();
        emptyOwner.set(Holder.DIRECT, emptyOptional);

        new TUnitProcessor(List.of(data("header.cpp", List.of(canonical)),
                data("use.cpp", List.of(matchingOwner, reversedOwner, emptyOwner), matchingOwner, reversedOwner,
                        emptyOwner)), true).getTranslationUnits();

        assertSame(canonical, matchingOwner.get(Holder.DIRECT));
        assertSame(reversedList, reversedOwner.get(Holder.DIRECT), "List order is part of a structured signature value");
        assertSame(emptyOptional, emptyOwner.get(Holder.DIRECT), "An empty optional differs from a present optional");
    }

    private OptionalAndListSignatureVar optionalSignatureVar(Optional<String> optional, List<String> values) {
        var node = factory.node(OptionalAndListSignatureVar.class);
        node.set(ClavaNode.LOCATION, new SourceRange("/project/header.hpp", 1, 1, 1, 8));
        node.set(NamedDecl.LINKAGE, Linkage.External);
        node.set(OptionalAndListSignatureVar.OPTIONAL_SIGNATURE, optional);
        node.set(OptionalAndListSignatureVar.LIST_SIGNATURE, new ArrayList<>(values));
        return node;
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
