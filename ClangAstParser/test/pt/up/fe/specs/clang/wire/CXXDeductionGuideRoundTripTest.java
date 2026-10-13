package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXDeductionGuideDecl;
import pt.up.fe.specs.clava.ast.decl.ClassTemplateDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateDecl;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.data.ExplicitSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentType;
import pt.up.fe.specs.clava.ast.expr.CXXBoolLiteralExpr;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.TemplateSpecializationType;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.TemplateTypeParmKind;

class CXXDeductionGuideRoundTripTest {

    @Test
    void readsTypedGuideIdentityExplicitSpecifierConstructorSourceAndCode() {
        var context = new ClavaContext();
        var data = new ClangAstData();
        data.set(ClangAstData.CONTEXT, context);
        var clavaNodes = new ClavaNodes(context.get(ClavaContext.FACTORY));
        data.set(ClangAstData.CLAVA_NODES, clavaNodes);
        var files = new ProtoAstReader.Files("unit");
        files.addPath("/tmp/deduction-guide.cpp");

        var parameterStore = DataStore.newInstance(StoreDefinitions.fromInterface(TemplateTypeParmDecl.class), true);
        parameterStore.set(TemplateTypeParmDecl.KIND, TemplateTypeParmKind.TYPENAME);
        parameterStore.set(TemplateTypeParmDecl.IS_PARAMETER_PACK, false);
        parameterStore.set(TemplateTypeParmDecl.DEFAULT_ARGUMENT, Optional.empty());
        parameterStore.set(NamedDecl.DECL_NAME, "T");
        var templateParameter = new TemplateTypeParmDecl(parameterStore, List.of());

        var templateStore = DataStore.newInstance(StoreDefinitions.fromInterface(ClassTemplateDecl.class), true);
        templateStore.set(NamedDecl.DECL_NAME, "GuideBox");
        templateStore.set(TemplateDecl.TEMPLATE_PARAMETERS, new ArrayList<>(List.of(templateParameter)));
        var deducedTemplate = new ClassTemplateDecl(templateStore, List.of());
        var constructor = new CXXConstructorDecl(
                DataStore.newInstance(StoreDefinitions.fromInterface(CXXConstructorDecl.class), true), List.of());

        var intStore = DataStore.newInstance(StoreDefinitions.fromInterface(BuiltinType.class), true);
        intStore.set(BuiltinType.KIND, BuiltinKind.Int);
        intStore.set(BuiltinType.KIND_LITERAL, "int");
        var intType = new BuiltinType(intStore, List.of());

        var returnTypeStore = DataStore.newInstance(
                StoreDefinitions.fromInterface(TemplateSpecializationType.class), true);
        returnTypeStore.set(TemplateSpecializationType.IS_TYPE_ALIAS, false);
        returnTypeStore.set(TemplateSpecializationType.ALIASED_TYPE, Optional.empty());
        returnTypeStore.set(TemplateSpecializationType.TEMPLATE_NAME, "GuideBox");
        returnTypeStore.set(TemplateSpecializationType.TEMPLATE_DECL, Optional.of(deducedTemplate));
        returnTypeStore.set(TemplateSpecializationType.TEMPLATE_ARGUMENTS,
                new ArrayList<>(List.of(new TemplateArgumentType(intType))));
        var returnType = new TemplateSpecializationType(returnTypeStore, List.of());
        FunctionProtoType functionType = context.get(ClavaContext.FACTORY)
                .functionProtoType(returnType, List.of(intType));
        var parameter = context.get(ClavaContext.FACTORY).parmVarDecl("value", intType);

        var expressionStore = DataStore.newInstance(StoreDefinitions.fromInterface(CXXBoolLiteralExpr.class), true);
        expressionStore.set(CXXBoolLiteralExpr.VALUE, false);
        var explicitExpression = new CXXBoolLiteralExpr(expressionStore, List.of());
        var requiresExpressionStore = DataStore.newInstance(
                StoreDefinitions.fromInterface(CXXBoolLiteralExpr.class), true);
        requiresExpressionStore.set(CXXBoolLiteralExpr.VALUE, true);
        var requiresExpression = new CXXBoolLiteralExpr(requiresExpressionStore, List.of());

        clavaNodes.getNodes().put(files.id(1), deducedTemplate);
        clavaNodes.getNodes().put(files.id(2), constructor);
        clavaNodes.getNodes().put(files.id(3), functionType);
        clavaNodes.getNodes().put(files.id(4), templateParameter);
        clavaNodes.getNodes().put(files.id(6), explicitExpression);
        clavaNodes.getNodes().put(files.id(7), requiresExpression);

        var wire = Node.newBuilder()
                .setId(5)
                .setClassName("CXXDeductionGuideDecl")
                .setCXXDeductionGuideDeclData(guideData(7, 3, 4, 5, 1, 2, 6))
                .build();
        var guideStore = ProtoNodeDataReader.read(wire, data, files::id, files);
        var guide = new CXXDeductionGuideDecl(guideStore, List.of(parameter));
        clavaNodes.getNodes().put(files.id(5), guide);
        clavaNodes.runQueuedActions();

        assertSame(deducedTemplate, guide.get(CXXDeductionGuideDecl.DEDUCED_TEMPLATE));
        assertSame(constructor, guide.get(CXXDeductionGuideDecl.CORRESPONDING_CONSTRUCTOR).orElseThrow());
        assertSame(explicitExpression, guide.get(CXXDeductionGuideDecl.EXPLICIT_SPECIFIER)
                .get(ExplicitSpecifier.EXPR).orElseThrow());
        assertSame(requiresExpression, guide.get(CXXDeductionGuideDecl.TEMPLATE_REQUIRES_CLAUSE).orElseThrow());
        assertFalse(guide.get(CXXDeductionGuideDecl.IS_EXPLICIT));
        assertEquals(pt.up.fe.specs.clava.ast.decl.enums.ExplicitSpecKind.ResolvedFalse,
                guide.get(CXXDeductionGuideDecl.EXPLICIT_SPECIFIER).get(ExplicitSpecifier.KIND));
        assertEquals(pt.up.fe.specs.clava.ast.decl.enums.DeductionCandidateKind.Copy,
                guide.get(CXXDeductionGuideDecl.DEDUCTION_CANDIDATE_KIND));
        assertEquals(7, guide.get(ClavaNode.LOCATION).getStart().getLine());
        assertEquals("template<typename T>\nrequires true\nexplicit(false) GuideBox(int value) -> GuideBox<int>;",
                guide.getCode());
    }

    @Test
    void rejectsMissingRequiredGuideFields() {
        var complete = guideData(7, 3, 4, 5, 1, 2, 6);

        IllegalArgumentException missingTemplate = assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(complete.clone().clearDeducedTemplate().build(),
                        "CXXDeductionGuideDecl"));
        assertTrue(missingTemplate.getMessage().contains("CXXDeductionGuideDeclData.deduced_template"),
                missingTemplate.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(complete.clone().clearIsExplicit().build(),
                        "CXXDeductionGuideDecl"));
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(complete.clone().clearExplicitSpecifier().build(),
                        "CXXDeductionGuideDecl"));
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(complete.clone().clearDeductionCandidateKind().build(),
                        "CXXDeductionGuideDecl"));
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(complete.clone().clearCorrespondingConstructor().build(),
                        "CXXDeductionGuideDecl"));
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(complete.clone().clearTemplateRequiresClause().build(),
                        "CXXDeductionGuideDecl"));
    }

    @Test
    void readsAbsentOptionalExplicitExpressionAsEmpty() {
        var context = new ClavaContext();
        var data = new ClangAstData();
        data.set(ClangAstData.CONTEXT, context);
        data.set(ClangAstData.CLAVA_NODES, new ClavaNodes(context.get(ClavaContext.FACTORY)));
        var files = new ProtoAstReader.Files("optional-explicit-expression");
        files.addPath("/tmp/deduction-guide.cpp");

        var payload = guideData(7, 3, 4, 5, 1, 2, 6)
                .setExplicitSpecifier(pt.up.fe.specs.clang.wire.ExplicitSpecifier.newBuilder()
                        .setKind(ExplicitSpecKind.EXPLICITSPECKIND_RESOLVEDFALSE)
                        .setIsSpecified(false))
                .build();
        var wire = Node.newBuilder()
                .setId(5)
                .setClassName("CXXDeductionGuideDecl")
                .setCXXDeductionGuideDeclData(payload)
                .build();

        var store = ProtoNodeDataReader.read(wire, data, files::id, files);
        var explicitSpecifier = store.get(CXXDeductionGuideDecl.EXPLICIT_SPECIFIER);

        assertTrue(explicitSpecifier.get(ExplicitSpecifier.EXPR).isEmpty());
    }

    private static CXXDeductionGuideDeclData.Builder guideData(int line, long functionTypeId,
            long templateParameterId, long canonicalId, long deducedTemplateId,
            long constructorId, long explicitExpressionId) {
        var range = Range.newBuilder().setFile(1).setLine(line).setColumn(1)
                .setEndFile(1).setEndLine(line).setEndColumn(60);
        var source = SourceInfo.newBuilder().setExpansion(range).setIsMacro(false).setSystemHeader(false);
        var decl = DeclData.newBuilder().setBase(NodeData.newBuilder().setSource(source))
                .setIsImplicit(false).setIsUsed(false).setIsReferenced(false)
                .setIsInvalidDecl(false).setIsModulePrivate(false);
        var named = NamedDeclData.newBuilder().setBase(decl)
                .setQualifiedPrefix("").setDeclName("<deduction guide for GuideBox>")
                .setNameKind(NameKind.NAMEKIND_CXX_DEDUCTION_GUIDE_NAME)
                .setIsCxxClassMember(false).setIsCxxInstanceMember(false)
                .setLinkage(Linkage.LINKAGE_EXTERNAL).setVisibility(Visibility.VISIBILITY_DEFAULT);
        var value = ValueDeclData.newBuilder().setBase(named).setType(functionTypeId).setIsWeak(false);
        var function = FunctionDeclData.newBuilder()
                .setBase(DeclaratorDeclData.newBuilder().setBase(value))
                .setIsConstexpr(false)
                .setTemplateKind(TemplateKind.TEMPLATEKIND_FUNCTION_TEMPLATE)
                .setStorageClass(StorageClass.STORAGECLASS_NONE)
                .setIsInlineSpecified(false).setIsVirtualAsWritten(false)
                .setIsPure(false).setIsDeleted(false).setIsExplicitlyDefaulted(false)
                .setPreviousDecl(-2)
                .setCanonicalDecl(canonicalId)
                .setPrimaryTemplateDecl(-2)
                .addTemplateParameters(templateParameterId)
                .addTemplateParameterListSizes(1);

        return CXXDeductionGuideDeclData.newBuilder()
                .setBase(function)
                .setDeducedTemplate(deducedTemplateId)
                .setCorrespondingConstructor(constructorId)
                .setTemplateRequiresClause(7)
                .setIsExplicit(false)
                .setExplicitSpecifier(pt.up.fe.specs.clang.wire.ExplicitSpecifier.newBuilder()
                        .setKind(ExplicitSpecKind.EXPLICITSPECKIND_RESOLVEDFALSE)
                        .setExpr(explicitExpressionId)
                        .setIsSpecified(true))
                .setDeductionCandidateKind(DeductionCandidateKind.DEDUCTIONCANDIDATEKIND_COPY);
    }
}
