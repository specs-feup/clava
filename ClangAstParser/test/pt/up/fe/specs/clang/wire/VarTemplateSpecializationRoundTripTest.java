/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.VarTemplateDecl;
import pt.up.fe.specs.clava.ast.decl.VarTemplatePartialSpecializationDecl;
import pt.up.fe.specs.clava.ast.decl.VarTemplateSpecializationDecl;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.TemplateTypeParmKind;

class VarTemplateSpecializationRoundTripTest {

    @Test
    void readsTypedSpecializationIdentityArgumentsParametersSourceAndCode() {
        var context = new ClavaContext();
        var data = new ClangAstData();
        data.set(ClangAstData.CONTEXT, context);
        var clavaNodes = new ClavaNodes(context.get(ClavaContext.FACTORY));
        data.set(ClangAstData.CLAVA_NODES, clavaNodes);

        var primary = new VarTemplateDecl(DataStore.newInstance(
                StoreDefinitions.fromInterface(VarTemplateDecl.class), true), List.of());
        var intTypeData = DataStore.newInstance(StoreDefinitions.fromInterface(BuiltinType.class), true);
        intTypeData.set(BuiltinType.KIND, BuiltinKind.Int);
        intTypeData.set(BuiltinType.KIND_LITERAL, "int");
        var intType = new BuiltinType(intTypeData, List.of());
        var parameterData = DataStore.newInstance(StoreDefinitions.fromInterface(TemplateTypeParmDecl.class), true);
        parameterData.set(TemplateTypeParmDecl.KIND, TemplateTypeParmKind.TYPENAME);
        parameterData.set(TemplateTypeParmDecl.IS_PARAMETER_PACK, false);
        parameterData.set(TemplateTypeParmDecl.DEFAULT_ARGUMENT, java.util.Optional.empty());
        parameterData.set(TemplateTypeParmDecl.DECL_NAME, "T");
        var parameter = new TemplateTypeParmDecl(parameterData, List.of());
        clavaNodes.getNodes().put("@1_unit", primary);
        clavaNodes.getNodes().put("@2_unit", intType);
        clavaNodes.getNodes().put("@4_unit", parameter);

        var files = new ProtoAstReader.Files("unit");
        files.addPath("/tmp/var-template-header.cpp");
        var partialWire = Node.newBuilder()
                .setId(3)
                .setClassName("VarTemplatePartialSpecializationDecl")
                .setVarTemplatePartialSpecializationDeclData(partialData(5))
                .build();
        var partialStore = ProtoNodeDataReader.read(partialWire, data, files::id, files);
        var partialNode = new VarTemplatePartialSpecializationDecl(partialStore, List.of());
        clavaNodes.getNodes().put("@3_unit", partialNode);
        var implicitWire = Node.newBuilder()
                .setId(5)
                .setClassName("VarTemplateSpecializationDecl")
                .setVarTemplateSpecializationDeclData(specializationData(8)
                        .setSpecializationKind(TemplateSpecializationKind.TEMPLATESPECIALIZATIONKIND_IMPLICITINSTANTIATION)
                        .setSpecializedPartialSpecialization(3)
                        .setPointOfInstantiation(SourceLocation.newBuilder()
                                .setFile(1).setLine(10).setColumn(45)))
                .build();
        var explicitWire = Node.newBuilder()
                .setId(6)
                .setClassName("VarTemplateSpecializationDecl")
                .setVarTemplateSpecializationDeclData(specializationData(12))
                .build();
        var implicitStore = ProtoNodeDataReader.read(implicitWire, data, files::id, files);
        var explicitStore = ProtoNodeDataReader.read(explicitWire, data, files::id, files);
        clavaNodes.runQueuedActions();

        assertSame(primary, partialStore.get(VarTemplateSpecializationDecl.SPECIALIZED_TEMPLATE));
        assertSame(primary, implicitStore.get(VarTemplateSpecializationDecl.SPECIALIZED_TEMPLATE));
        assertSame(primary, explicitStore.get(VarTemplateSpecializationDecl.SPECIALIZED_TEMPLATE));
        assertTrue(partialStore.get(VarTemplateSpecializationDecl.SPECIALIZED_PARTIAL_SPECIALIZATION).isEmpty());
        assertTrue(partialStore.get(VarTemplatePartialSpecializationDecl.INSTANTIATED_FROM_MEMBER).isEmpty());
        assertTrue(partialStore.get(VarTemplateSpecializationDecl.POINT_OF_INSTANTIATION).isEmpty());
        assertEquals(10, implicitStore.get(VarTemplateSpecializationDecl.POINT_OF_INSTANTIATION)
                .orElseThrow().getLine());
        assertTrue(explicitStore.get(VarTemplateSpecializationDecl.POINT_OF_INSTANTIATION).isEmpty());
        assertSame(partialNode, implicitStore.get(VarTemplateSpecializationDecl.SPECIALIZED_PARTIAL_SPECIALIZATION)
                .orElseThrow());
        assertEquals(5, partialStore.get(ClavaNode.LOCATION).getStart().getLine());
        assertEquals(8, implicitStore.get(ClavaNode.LOCATION).getStart().getLine());
        assertEquals(12, explicitStore.get(ClavaNode.LOCATION).getStart().getLine());
        assertSame(parameter, partialStore.get(VarTemplatePartialSpecializationDecl.TEMPLATE_PARAMETERS).get(0));
        assertEquals(2, partialStore.get(VarTemplatePartialSpecializationDecl.TEMPLATE_ARGUMENTS_AS_WRITTEN).size());
        assertEquals(2, partialStore.get(VarTemplateSpecializationDecl.TEMPLATE_ARGUMENTS).size());
        assertEquals(2, implicitStore.get(VarTemplateSpecializationDecl.TEMPLATE_ARGUMENTS).size());
        assertEquals(2, explicitStore.get(VarTemplateSpecializationDecl.TEMPLATE_ARGUMENTS).size());

        var partialCode = new VarTemplatePartialSpecializationDecl(partialStore, List.of()).getCode();
        var implicitCode = new VarTemplateSpecializationDecl(implicitStore, List.of()).getCode();
        var explicitCode = new VarTemplateSpecializationDecl(explicitStore, List.of()).getCode();
        assertEquals("template<typename T>\nint variable_template<int, 2>", partialCode);
        assertEquals("int variable_template<int, 2>", implicitCode);
        assertEquals("template<>\nint variable_template<int, 2>", explicitCode);
    }

    @Test
    void rejectsMissingRequiredSpecializationAndPartialFields() {
        var specialization = specializationData(8).clearSpecializationKind().build();
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(specialization, "VarTemplateSpecializationDecl"));

        var partial = partialData(5).clearIsMemberSpecialization().build();
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(partial, "VarTemplatePartialSpecializationDecl"));

        var missingPointFile = specializationData(8)
                .setPointOfInstantiation(SourceLocation.newBuilder().setLine(1).setColumn(1))
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(missingPointFile, "VarTemplateSpecializationDecl"));
    }

    private static VarTemplateSpecializationDeclData.Builder specializationData(int line) {
        return VarTemplateSpecializationDeclData.newBuilder()
                .setBase(varDecl(line))
                .setSpecializedTemplate(1)
                .setSpecializedPartialSpecialization(-2)
                .setSpecializationKind(TemplateSpecializationKind.TEMPLATESPECIALIZATIONKIND_EXPLICITSPECIALIZATION)
                .addTemplateArguments(typeArgument(2))
                .addTemplateArguments(integralArgument("2"))
                .addTemplateInstantiationArguments(typeArgument(2))
                .addTemplateInstantiationArguments(integralArgument("2"));
    }

    private static VarTemplatePartialSpecializationDeclData.Builder partialData(int line) {
        return VarTemplatePartialSpecializationDeclData.newBuilder()
                .setBase(specializationData(line))
                .addTemplateParameters(4)
                .setInstantiatedFromMember(-2)
                .addTemplateArgumentsAsWritten(typeArgument(2))
                .addTemplateArgumentsAsWritten(integralArgument("2"))
                .setIsMemberSpecialization(false);
    }

    private static VarDeclData varDecl(int line) {
        var range = Range.newBuilder().setFile(1).setLine(line).setColumn(7)
                .setEndFile(1).setEndLine(line).setEndColumn(22);
        var source = SourceInfo.newBuilder().setExpansion(range).setIsMacro(false).setSystemHeader(false);
        var decl = DeclData.newBuilder().setBase(NodeData.newBuilder().setSource(source))
                .setIsImplicit(false).setIsUsed(false).setIsReferenced(false)
                .setIsInvalidDecl(false).setIsModulePrivate(false);
        var named = NamedDeclData.newBuilder().setBase(decl)
                .setQualifiedPrefix("").setDeclName("variable_template")
                .setNameKind(NameKind.NAMEKIND_IDENTIFIER)
                .setIsCxxClassMember(false).setIsCxxInstanceMember(false)
                .setLinkage(Linkage.LINKAGE_EXTERNAL).setVisibility(Visibility.VISIBILITY_DEFAULT);
        var value = ValueDeclData.newBuilder().setBase(named).setType(2).setIsWeak(false);
        return VarDeclData.newBuilder().setBase(value)
                .setStorageClass(StorageClass.STORAGECLASS_NONE)
                .setTlsKind(TLSKind.TLSKIND_NONE).setIsNrvoVariable(false)
                .setInitStyle(InitializationStyle.INITIALIZATIONSTYLE_CINIT)
                .setIsConstexpr(true).setIsStaticDataMember(false).setIsOutOfLine(false)
                .setHasGlobalStorage(true)
                .build();
    }

    private static pt.up.fe.specs.clang.wire.TemplateArgument typeArgument(long type) {
        return TemplateArgument.newBuilder().setTemplateType(TemplateType.newBuilder().setType(type)).build();
    }

    private static pt.up.fe.specs.clang.wire.TemplateArgument integralArgument(String value) {
        return TemplateArgument.newBuilder().setTemplateIntegral(TemplateIntegral.newBuilder().setIntegral(value)).build();
    }
}
