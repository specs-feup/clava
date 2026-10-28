/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import pt.up.fe.specs.clang.codeparser.TUnitProcessor;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.utils.CompilerEntityIdentity;

/** Native dump references have exact occurrence identity and must never be matched by source signatures. */
class UsrOccurrenceRegressionTest extends TUnitProcessorTestSupport {
    private VarDecl nativeVariable(String name) {
        var node = variable(name, "/project/shared.hpp", 1);
        node.set(NamedDecl.USR, "c:@" + name);
        node.set(NamedDecl.USR_GENERATION_FAILED, false);
        node.set(NamedDecl.USR_IS_TU_LOCAL, true);
        node.set(NamedDecl.USR_TYPE, "I");
        node.set(NamedDecl.USR_TYPE_GENERATION_FAILED, false);
        node.set(NamedDecl.USR_DEFINITION_KIND, "definition");
        return node;
    }

    @Test void nativeGraphPreservesDirectOptionalAndListTargets() {
        var first = nativeVariable("value");
        var second = nativeVariable("value");
        var owner = holder();
        owner.set(Holder.DIRECT, first);
        var optional = Optional.<ClavaNode>of(first);
        var list = List.<ClavaNode>of(first, second, first);
        owner.set(Holder.OPTIONAL, optional);
        owner.set(Holder.NODES, list);
        var firstData = data("/project/a.cpp", List.of(owner), owner, first);
        var secondData = data("/project/b.cpp", List.of(second), second);
        new TUnitProcessor(List.of(firstData, secondData), true).getTranslationUnits();
        assertSame(first, owner.get(Holder.DIRECT));
        assertSame(optional, owner.get(Holder.OPTIONAL));
        assertSame(list, owner.get(Holder.NODES));
        assertNotEquals(CompilerEntityIdentity.key(first), CompilerEntityIdentity.key(second));
    }

    @Test void originAndBaselineAreAssignedEvenWhenLegacyNormalizationIsDisabled() {
        var node = nativeVariable("value");
        var data = data("/project/a.cpp", List.of(), node);
        new TUnitProcessor(List.of(data), false).getTranslationUnits();
        assertEquals("/project/a.cpp", node.get(ClavaNode.ORIGINATING_TRANSLATION_UNIT).orElseThrow());
        assertTrue(CompilerEntityIdentity.isCurrent(node));
        assertEquals("/project/a.cpp", CompilerEntityIdentity.key(node).orElseThrow().translationUnit());
    }

    @Test void missingTuProvenanceCannotEstablishInternalEntityIdentity() {
        var node = nativeVariable("value");
        assertTrue(CompilerEntityIdentity.key(node).isEmpty());
    }

    @Test void inconsistentSuccessMetadataIsRejected() {
        var node = nativeVariable("value");
        node.set(NamedDecl.USR, "");
        var data = data("/project/a.cpp", List.of(), node);
        assertThrows(IllegalArgumentException.class,
                () -> new TUnitProcessor(List.of(data), true).getTranslationUnits());
    }

    @Test void failedGenerationCannotRetainAPartialUsr() {
        var node = nativeVariable("value");
        node.set(NamedDecl.USR_GENERATION_FAILED, true);
        var data = data("/project/a.cpp", List.of(), node);
        assertThrows(IllegalArgumentException.class,
                () -> new TUnitProcessor(List.of(data), true).getTranslationUnits());
    }
    @Test void inconsistentTypeUsrStatusIsRejected() {
        var node = nativeVariable("value");
        node.set(NamedDecl.USR_TYPE_GENERATION_FAILED, true);
        var data = data("/project/a.cpp", List.of(), node);
        assertThrows(IllegalArgumentException.class,
                () -> new TUnitProcessor(List.of(data), true).getTranslationUnits());
    }

    @Test void unknownDefinitionKindIsRejected() {
        var node = nativeVariable("value");
        node.set(NamedDecl.USR_DEFINITION_KIND, "unknown");
        var data = data("/project/a.cpp", List.of(), node);
        assertThrows(IllegalArgumentException.class,
                () -> new TUnitProcessor(List.of(data), true).getTranslationUnits());
    }

    @Test void repeatedProcessingCannotBlessChangedCompilerFacts() {
        var node = nativeVariable("value");
        var data = data("/project/a.cpp", List.of(), node);
        var processor = new TUnitProcessor(List.of(data), true);
        processor.getTranslationUnits();
        node.set(NamedDecl.DECL_NAME, "changed");
        processor.getTranslationUnits();
        assertFalse(CompilerEntityIdentity.isCurrent(node));
    }

}
