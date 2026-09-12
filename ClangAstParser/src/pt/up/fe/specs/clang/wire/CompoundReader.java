package pt.up.fe.specs.clang.wire;

import java.util.*;
import pt.up.fe.specs.clang.wire.SchemaRuntime.ImportContext;
import pt.up.fe.specs.clava.ast.decl.data.*;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.*;
import pt.up.fe.specs.clava.ast.decl.data.nestedname.*;
import pt.up.fe.specs.clava.ast.decl.data.templates.*;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.*;
import pt.up.fe.specs.clava.ast.decl.enums.*;
import pt.up.fe.specs.clava.ast.expr.data.designator.*;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.*;
import pt.up.fe.specs.clava.ast.stmt.data.*;
import pt.up.fe.specs.clava.ast.type.data.*;
import pt.up.fe.specs.clava.ast.type.enums.*;
import pt.up.fe.specs.clava.language.*;
import static pt.up.fe.specs.clang.wire.SchemaRuntime.list;

/** Converts generated compound records, resolving every embedded reference eagerly. */
public final class CompoundReader {
    public static CXXBaseSpecifier readCXXBaseSpecifier(astwire.v2.CXXBaseSpecifier v, ImportContext c) {
        var out=new CXXBaseSpecifier();
        out.set(CXXBaseSpecifier.IS_VIRTUAL,v.isVirtual());out.set(CXXBaseSpecifier.IS_PACK_EXPANSION,v.isPackExpansion());
        out.set(CXXBaseSpecifier.ACCESS_SPECIFIER_AS_WRITTEN,AccessSpecifier.values()[v.accessSpecifierAsWritten()]);
        out.set(CXXBaseSpecifier.ACCESS_SPECIFIER_SEMANTIC,AccessSpecifier.values()[v.accessSpecifierSemantic()]);
        c.data().getClavaNodes().queueSetNode(out,CXXBaseSpecifier.TYPE,c.id(v.type()));return out;
    }
    public static ExplicitSpecifier readExplicitSpecifier(astwire.v2.ExplicitSpecifier v, ImportContext c) {
        var out=new ExplicitSpecifier();out.set(ExplicitSpecifier.KIND,ExplicitSpecKind.values()[v.kind()]);
        out.set(ExplicitSpecifier.IS_SPECIFIED,v.isSpecified());
        c.data().getClavaNodes().queueSetOptionalNode(out,ExplicitSpecifier.EXPR,c.id(v.expr()));return out;
    }
    public static TemplateArgument readTemplateArgument(astwire.v2.TemplateArgument v, ImportContext c) {
        var nodes=c.data().getClavaNodes();
        switch(v.valueType()) {
        case astwire.v2.TemplateValue.TemplateDeclaration: {
            var x=(astwire.v2.TemplateDeclaration)v.value(new astwire.v2.TemplateDeclaration());
            var out=new TemplateArgumentDeclaration();nodes.queueSetNode(out,TemplateArgumentDeclaration.DECL,c.id(x.decl()));return out;
        }
        case astwire.v2.TemplateValue.TemplateNullPtr: {
            var x=(astwire.v2.TemplateNullPtr)v.value(new astwire.v2.TemplateNullPtr());
            var out=new TemplateArgumentNullPtr();nodes.queueSetNode(out,TemplateArgumentNullPtr.TYPE,c.id(x.type()));return out;
        }
        case astwire.v2.TemplateValue.TemplateType: {
            var x=(astwire.v2.TemplateType)v.value(new astwire.v2.TemplateType());
            var out=new TemplateArgumentType();nodes.queueSetNode(out,TemplateArgumentType.TYPE,c.id(x.type()));return out;
        }
        case astwire.v2.TemplateValue.TemplateExpression: {
            var x=(astwire.v2.TemplateExpression)v.value(new astwire.v2.TemplateExpression());
            var out=new TemplateArgumentExpr();nodes.queueSetNode(out,TemplateArgumentExpr.EXPR,c.id(x.expr()));return out;
        }
        case astwire.v2.TemplateValue.TemplateStructuralValue: {
            var x=(astwire.v2.TemplateStructuralValue)v.value(new astwire.v2.TemplateStructuralValue());
            var out=new TemplateArgumentStructuralValue();nodes.queueSetNode(out,TemplateArgumentStructuralValue.TYPE,c.id(x.type()));return out;
        }
        case astwire.v2.TemplateValue.TemplatePack: {
            var x=(astwire.v2.TemplatePack)v.value(new astwire.v2.TemplatePack());
            var out=new TemplateArgumentPack();out.set(TemplateArgumentPack.PACK,list(x.argumentsLength(),i->readTemplateArgument(x.arguments(i),c)));return out;
        }
        case astwire.v2.TemplateValue.TemplateIntegral: {
            var x=(astwire.v2.TemplateIntegral)v.value(new astwire.v2.TemplateIntegral());
            var out=new TemplateArgumentIntegral();out.set(TemplateArgumentIntegral.INTEGRAL,new java.math.BigInteger(x.integral()).intValueExact());return out;
        }
        case astwire.v2.TemplateValue.TemplateName:return readTemplateName((astwire.v2.TemplateName)v.value(new astwire.v2.TemplateName()),c);
        case astwire.v2.TemplateValue.TemplateExpansion: {
            var x=(astwire.v2.TemplateExpansion)v.value(new astwire.v2.TemplateExpansion());
            var out=new TemplateArgumentTemplateExpansion();
            out.set(TemplateArgumentTemplateExpansion.NUM_EXPANSIONS,x.hasNumExpansions()?Optional.of(Math.toIntExact(x.numExpansions())):Optional.empty());
            out.set(TemplateArgumentTemplateExpansion.TEMPLATE,readTemplateName(x.templateName(),c));return out;
        }
        default:throw new IllegalArgumentException("Unknown template argument "+v.valueType());
        }
    }
    public static TemplateArgumentTemplate readTemplateName(astwire.v2.TemplateName v, ImportContext c) {
        var nodes=c.data().getClavaNodes();
        TemplateNameKind kind=switch(v.valueType()) {
            case astwire.v2.TemplateNameValue.DirectTemplateName -> TemplateNameKind.Template;
            case astwire.v2.TemplateNameValue.QualifiedTemplateName -> TemplateNameKind.QualifiedTemplate;
            case astwire.v2.TemplateNameValue.SubstitutedTemplateName -> TemplateNameKind.SubstTemplateTemplateParm;
            case astwire.v2.TemplateNameValue.UsingTemplateName -> TemplateNameKind.UsingTemplate;
            case astwire.v2.TemplateNameValue.DependentTemplateName -> TemplateNameKind.DependentTemplate;
            default -> throw new IllegalArgumentException("Unknown template name "+v.valueType());
        };
        var out=TemplateArgumentTemplate.newInstance(kind);out.set(TemplateArgumentTemplate.TEMPLATE_NAME_KIND,kind);
        switch(kind) {
        case Template: {
            var x=(astwire.v2.DirectTemplateName)v.value(new astwire.v2.DirectTemplateName());
            nodes.queueSetOptionalNode(out,Template.TEMPLATE_DECL,c.id(x.templateDecl()));break;
        }
        case QualifiedTemplate: {
            var x=(astwire.v2.QualifiedTemplateName)v.value(new astwire.v2.QualifiedTemplateName());
            out.set(QualifiedTemplate.QUALIFIER,x.qualifier());out.set(QualifiedTemplate.HAS_TEMPLATE_KEYWORD,x.hasTemplateKeyword());
            nodes.queueSetNode(out,QualifiedTemplate.TEMPLATE_DECL,c.id(x.templateDecl()));break;
        }
        case SubstTemplateTemplateParm: {
            var x=(astwire.v2.SubstitutedTemplateName)v.value(new astwire.v2.SubstitutedTemplateName());
            nodes.queueSetNode(out,SubstTemplateTemplateParm.PARAMETER,c.id(x.parameter()));out.set(SubstTemplateTemplateParm.REPLACEMENT,readTemplateName(x.replacement(),c));break;
        }
        case UsingTemplate: {
            var x=(astwire.v2.UsingTemplateName)v.value(new astwire.v2.UsingTemplateName());
            nodes.queueSetNode(out,UsingTemplate.USING_SHADOW_DECL,c.id(x.usingShadowDecl()));break;
        }
        case DependentTemplate: {
            var x=(astwire.v2.DependentTemplateName)v.value(new astwire.v2.DependentTemplateName());
            out.set(DependentTemplate.QUALIFIER,x.qualifier());out.set(DependentTemplate.NAME,x.name());break;
        }
        default:throw new IllegalArgumentException("Unsupported template kind "+kind);
        }
        return out;
    }
    public static CXXCtorInitializer readCXXCtorInitializer(astwire.v2.CXXCtorInitializer v, ImportContext c) {
        var nodes=c.data().getClavaNodes();
        CXXCtorInitializer out;
        switch(v.targetType()) {
        case astwire.v2.InitializerTarget.AnyMemberInitializer: {
            var x=(astwire.v2.AnyMemberInitializer)v.target(new astwire.v2.AnyMemberInitializer());
            out=CXXCtorInitializer.newInstance(CXXCtorInitializerKind.ANY_MEMBER_INITIALIZER);
            nodes.queueSetNode(out,AnyMemberInit.ANY_MEMBER_DECL,c.id(x.anyMemberDecl()));break;
        }
        case astwire.v2.InitializerTarget.BaseInitializer: {
            var x=(astwire.v2.BaseInitializer)v.target(new astwire.v2.BaseInitializer());
            out=CXXCtorInitializer.newInstance(CXXCtorInitializerKind.BASE_INITIALIZER);
            nodes.queueSetNode(out,BaseInit.BASE_CLASS,c.id(x.baseClass()));break;
        }
        case astwire.v2.InitializerTarget.DelegatingInitializer: {
            var x=(astwire.v2.DelegatingInitializer)v.target(new astwire.v2.DelegatingInitializer());
            out=CXXCtorInitializer.newInstance(CXXCtorInitializerKind.DELEGATING_INITIALIZER);
            nodes.queueSetNode(out,DelegatingInit.DELEGATED_TYPE,c.id(x.delegatedType()));break;
        }
        default:throw new IllegalArgumentException("Unknown initializer target "+v.targetType());
        }
        nodes.queueSetNode(out,CXXCtorInitializer.INIT_EXPR,c.id(v.initExpr()));
        out.set(CXXCtorInitializer.IS_IN_CLASS_MEMBER_INITIALIZER,v.isInClassMemberInitializer());out.set(CXXCtorInitializer.IS_WRITTEN,v.isWritten());return out;
    }
    public static ExceptionSpecification readExceptionSpecification(astwire.v2.ExceptionSpecification v, ImportContext c) {
        var kind=ExceptionSpecificationType.values()[v.kind()];var out=kind.newInstance();var nodes=c.data().getClavaNodes();
        nodes.queueSetNodeList(out,ExceptionSpecification.EXCEPTION_TYPES,list(v.exceptionTypesLength(),i->c.id(v.exceptionTypes(i))));
        switch(v.detailsType()) {
        case astwire.v2.ExceptionDetails.NoExceptionDetails:break;
        case astwire.v2.ExceptionDetails.ComputedExceptionDetails: {
            var x=(astwire.v2.ComputedExceptionDetails)v.details(new astwire.v2.ComputedExceptionDetails());
            nodes.queueSetNode(out,ComputedNoexcept.NOEXCEPT_EXPR,c.id(x.noexceptExpr()));break;
        }
        case astwire.v2.ExceptionDetails.UnevaluatedExceptionDetails: {
            var x=(astwire.v2.UnevaluatedExceptionDetails)v.details(new astwire.v2.UnevaluatedExceptionDetails());
            nodes.queueSetNode(out,UnevaluatedExceptionSpecification.SOURCE_DECL,c.id(x.sourceDecl()));break;
        }
        case astwire.v2.ExceptionDetails.UninstantiatedExceptionDetails: {
            var x=(astwire.v2.UninstantiatedExceptionDetails)v.details(new astwire.v2.UninstantiatedExceptionDetails());
            nodes.queueSetNode(out,UninstantiatedExceptionSpecification.SOURCE_DECL,c.id(x.sourceDecl()));
            nodes.queueSetNode(out,UninstantiatedExceptionSpecification.SOURCE_TEMPLATE,c.id(x.sourceTemplate()));break;
        }
        default:throw new IllegalArgumentException("Unknown exception details "+v.detailsType());
        }
        return out;
    }
    public static OffsetOfComponent readOffsetOfComponent(astwire.v2.OffsetOfComponent v, ImportContext c) {
        var nodes=c.data().getClavaNodes();
        switch(v.valueType()) {
        case astwire.v2.OffsetValue.OffsetArray: {
            var x=(astwire.v2.OffsetArray)v.value(new astwire.v2.OffsetArray());var out=OffsetOfComponent.newInstance(OffsetOfComponentKind.ARRAY);
            nodes.queueSetNode(out,OffsetOfArray.EXPR,c.id(x.expr()));return out;
        }
        case astwire.v2.OffsetValue.OffsetBase: {
            var x=(astwire.v2.OffsetBase)v.value(new astwire.v2.OffsetBase());var out=OffsetOfComponent.newInstance(OffsetOfComponentKind.BASE);
            nodes.queueSetNode(out,OffsetOfBase.TYPE,c.id(x.type()));return out;
        }
        case astwire.v2.OffsetValue.OffsetField: {
            var x=(astwire.v2.OffsetField)v.value(new astwire.v2.OffsetField());var out=OffsetOfComponent.newInstance(OffsetOfComponentKind.FIELD);
            out.set(OffsetOfField.FIELD_NAME,x.fieldName());return out;
        }
        case astwire.v2.OffsetValue.OffsetIdentifier: {
            var x=(astwire.v2.OffsetIdentifier)v.value(new astwire.v2.OffsetIdentifier());var out=OffsetOfComponent.newInstance(OffsetOfComponentKind.IDENTIFIER);
            out.set(OffsetOfIdentifier.FIELD_NAME,x.fieldName());return out;
        }
        default:throw new IllegalArgumentException("Unknown offset component "+v.valueType());
        }
    }
    public static Designator readDesignator(astwire.v2.Designator v, ImportContext c) {
        return switch(v.valueType()) {
        case astwire.v2.DesignatorValue.FieldDesignator -> new FieldDesignator(((astwire.v2.FieldDesignator)v.value(new astwire.v2.FieldDesignator())).fieldName());
        case astwire.v2.DesignatorValue.ArrayDesignator -> new ArrayDesignator(((astwire.v2.ArrayDesignator)v.value(new astwire.v2.ArrayDesignator())).index());
        case astwire.v2.DesignatorValue.ArrayRangeDesignator -> new ArrayRangeDesignator(((astwire.v2.ArrayRangeDesignator)v.value(new astwire.v2.ArrayRangeDesignator())).index());
        default -> throw new IllegalArgumentException("Unknown designator "+v.valueType());
        };
    }
    public static AsmInput readAsmInput(astwire.v2.AsmInput v, ImportContext c) {
        var out=new AsmInput();out.set(AsmInput.CONSTRAINT,v.constraint());c.data().getClavaNodes().queueSetNode(out,AsmInput.EXPR,c.id(v.expr()));return out;
    }
    public static AsmOutput readAsmOutput(astwire.v2.AsmOutput v, ImportContext c) {
        var out=new AsmOutput();out.set(AsmOutput.CONSTRAINT,v.constraint());out.set(AsmOutput.IS_PLUS_CONSTRAINT,v.isPlusConstraint());
        c.data().getClavaNodes().queueSetNode(out,AsmOutput.EXPR,c.id(v.expr()));return out;
    }
    public static NestedNameSpecifier readNestedNameSpecifier(astwire.v2.NestedNameSpecifier v, ImportContext c) {
        var kind=switch(v.valueType()) {
            case astwire.v2.NestedNameValue.NamespaceSpecifier -> NestedNameSpecifierKind.Namespace;
            case astwire.v2.NestedNameValue.NamespaceAliasSpecifier -> NestedNameSpecifierKind.NamespaceAlias;
            case astwire.v2.NestedNameValue.TypeSpecifier -> NestedNameSpecifierKind.TypeSpec;
            case astwire.v2.NestedNameValue.TypeWithTemplateSpecifier -> NestedNameSpecifierKind.TypeSpecWithTemplate;
            case astwire.v2.NestedNameValue.GlobalSpecifier -> NestedNameSpecifierKind.Global;
            case astwire.v2.NestedNameValue.SuperSpecifier -> NestedNameSpecifierKind.Super;
            default -> throw new IllegalArgumentException("Unknown nested name "+v.valueType());
        };
        var out=NestedNameSpecifier.newInstance(kind);var nodes=c.data().getClavaNodes();
        switch(kind) {
        case Namespace:nodes.queueSetNode(out,NamespaceSpecifier.NAMESPACE,c.id(((astwire.v2.NamespaceSpecifier)v.value(new astwire.v2.NamespaceSpecifier())).namespaceDecl()));break;
        case NamespaceAlias:nodes.queueSetNode(out,NamespaceAliasSpecifier.NAMESPACE_ALIAS,c.id(((astwire.v2.NamespaceAliasSpecifier)v.value(new astwire.v2.NamespaceAliasSpecifier())).namespaceAlias()));break;
        case TypeSpec:nodes.queueSetNode(out,TypeSpecSpecifier.TYPE,c.id(((astwire.v2.TypeSpecifier)v.value(new astwire.v2.TypeSpecifier())).type()));break;
        case TypeSpecWithTemplate:nodes.queueSetNode(out,TypeSpecWithTemplateSpecifier.TYPE,c.id(((astwire.v2.TypeWithTemplateSpecifier)v.value(new astwire.v2.TypeWithTemplateSpecifier())).type()));break;
        case Super:nodes.queueSetNode(out,SuperSpecifier.SUPER,c.id(((astwire.v2.SuperSpecifier)v.value(new astwire.v2.SuperSpecifier())).superDecl()));break;
        case Global:break;
        default:throw new IllegalArgumentException("Unsupported nested name "+kind);
        }
        return out;
    }
}
