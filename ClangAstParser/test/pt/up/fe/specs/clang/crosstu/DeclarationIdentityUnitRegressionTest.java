/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentDeclaration;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentIntegral;
import pt.up.fe.specs.clava.ast.decl.enums.Linkage;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.AddressSpaceQualifierV2;
import pt.up.fe.specs.clava.utils.DeclarationIdentity;
import pt.up.fe.specs.clava.utils.DefinitionResolution;

class DeclarationIdentityUnitRegressionTest extends TUnitProcessorTestSupport {
    private FunctionDecl function(String name, Type parameter) {
        var function = factory.functionDecl(name, factory.functionProtoType(factory.builtinType("int"), parameter));
        function.addChild(factory.parmVarDecl("x", parameter));
        return function;
    }

    @Test void builtinSpellingDoesNotChangeCompilerTypeIdentity() {
        var first = factory.builtinType("int");
        var second = factory.builtinType("int");
        second.set(BuiltinType.KIND_LITERAL, "signed int");
        var identities = new DeclarationIdentity();
        assertEquals(identities.type(first), identities.type(second));
    }

    @Test void unsupportedTypesNeverMatchByPrintedSpelling() {
        var first = factory.nullType();
        var second = factory.nullType();
        var identities = new DeclarationIdentity();
        assertNotEquals(identities.type(first), identities.type(second));
        assertEquals(identities.type(first), identities.type(first));
    }

    @Test void missingLinkageDoesNotEstablishCrossTuEquivalence() {
        var first = function("f", factory.builtinType("int"));
        var second = function("f", factory.builtinType("int"));
        first.set(NamedDecl.LINKAGE, Linkage.Invalid);
        second.set(NamedDecl.LINKAGE, Linkage.Invalid);
        var identities = new DeclarationIdentity();
        assertNotEquals(identities.function(first), identities.function(second));
    }

    @Test void addressSpaceIsNotStrippedWithParameterCv() {
        var ordinary = factory.builtinType("int").asConst();
        var device = (QualType) ordinary.copy();
        device.set(QualType.ADDRESS_SPACE_QUALIFIER, AddressSpaceQualifierV2.CUDA_DEVICE);
        var identities = new DeclarationIdentity();
        assertEquals(identities.parameter(ordinary), identities.parameter(factory.builtinType("int")));
        assertNotEquals(identities.parameter(ordinary), identities.parameter(device));
    }

    @Test void integralArgumentsWithoutTypeAndWidthCannotEstablishEquivalence() {
        var first = function("f", factory.builtinType("int"));
        var second = function("f", factory.builtinType("int"));
        first.set(FunctionDecl.TEMPLATE_ARGUMENTS, new java.util.ArrayList<>(List.of(new TemplateArgumentIntegral(1))));
        second.set(FunctionDecl.TEMPLATE_ARGUMENTS, new java.util.ArrayList<>(List.of(new TemplateArgumentIntegral(1))));
        var identities = new DeclarationIdentity();
        assertNotEquals(identities.function(first), identities.function(second));
    }

    @Test void functionDeclarationArgumentsDistinguishOverloads() {
        var first = function("f", factory.builtinType("int"));
        var second = function("f", factory.builtinType("int"));
        var intArgument = new TemplateArgumentDeclaration();
        intArgument.set(TemplateArgumentDeclaration.DECL, function("callback", factory.builtinType("int")));
        var doubleArgument = new TemplateArgumentDeclaration();
        doubleArgument.set(TemplateArgumentDeclaration.DECL, function("callback", factory.builtinType("double")));
        first.set(FunctionDecl.TEMPLATE_ARGUMENTS, new java.util.ArrayList<>(List.of(intArgument)));
        second.set(FunctionDecl.TEMPLATE_ARGUMENTS, new java.util.ArrayList<>(List.of(doubleArgument)));
        var identities = new DeclarationIdentity();
        assertNotEquals(identities.function(first), identities.function(second));
    }

    @Test void resolutionEnforcesItsPublicInvariantsAndCopiesCandidates() {
        assertThrows(IllegalArgumentException.class, () -> new DefinitionResolution<>(DefinitionResolution.Kind.UNIQUE, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DefinitionResolution<>(DefinitionResolution.Kind.AMBIGUOUS, List.of("one")));
        var candidates = new java.util.ArrayList<>(List.of("one"));
        var result = new DefinitionResolution<>(DefinitionResolution.Kind.UNIQUE, candidates);
        candidates.clear();
        assertEquals("one", result.unique().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> result.candidates().clear());
    }
}
