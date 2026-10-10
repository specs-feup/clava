/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Optional;

import pt.up.fe.specs.clang.wire.WireReader.Tu;
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

/**
 * Compound field values: wire tables that become Clava data objects rather than nodes. Each value is chosen by
 * its table name; unions choose the concrete Clava subclass. Node references are queued like node fields.
 */
final class Compounds {

    private Compounds() {
    }

    static Object read(WireTable v, Tu tu) {
        var nodes = tu.nodes();
        return switch (v.spec.name) {
            case "CXXBaseSpecifier" -> {
                var out = new CXXBaseSpecifier();
                out.set(CXXBaseSpecifier.IS_VIRTUAL, v.bool("is_virtual"));
                out.set(CXXBaseSpecifier.IS_PACK_EXPANSION, v.bool("is_pack_expansion"));
                out.set(CXXBaseSpecifier.ACCESS_SPECIFIER_AS_WRITTEN, access(v, "access_specifier_as_written"));
                out.set(CXXBaseSpecifier.ACCESS_SPECIFIER_SEMANTIC, access(v, "access_specifier_semantic"));
                nodes.queueSetNode(out, CXXBaseSpecifier.TYPE, ref(v, "type", tu));
                yield out;
            }
            case "ExplicitSpecifier" -> {
                var out = new ExplicitSpecifier();
                out.set(ExplicitSpecifier.KIND, ExplicitSpecKind.values()[(int) v.integer("kind")]);
                out.set(ExplicitSpecifier.IS_SPECIFIED, v.bool("is_specified"));
                nodes.queueSetOptionalNode(out, ExplicitSpecifier.EXPR, ref(v, "expr", tu));
                yield out;
            }
            case "TemplateArgument" -> templateArgument(v.union("value"), tu);
            case "TemplateName" -> templateName(v, tu);
            case "CXXCtorInitializer" -> ctorInitializer(v, tu);
            case "ExceptionSpecification" -> exceptionSpecification(v, tu);
            case "OffsetOfComponent" -> offsetOfComponent(v.union("value"), tu);
            case "Designator" -> {
                var value = v.union("value");
                yield switch (value.spec.name) {
                    case "FieldDesignator" -> new FieldDesignator(value.string("field_name"));
                    case "ArrayDesignator" -> new ArrayDesignator((int) value.integer("index"));
                    case "ArrayRangeDesignator" -> new ArrayRangeDesignator((int) value.integer("index"));
                    default -> throw unknown(value);
                };
            }
            case "AsmInput" -> {
                var out = new AsmInput();
                out.set(AsmInput.CONSTRAINT, v.string("constraint"));
                nodes.queueSetNode(out, AsmInput.EXPR, ref(v, "expr", tu));
                yield out;
            }
            case "AsmOutput" -> {
                var out = new AsmOutput();
                out.set(AsmOutput.CONSTRAINT, v.string("constraint"));
                out.set(AsmOutput.IS_PLUS_CONSTRAINT, v.bool("is_plus_constraint"));
                nodes.queueSetNode(out, AsmOutput.EXPR, ref(v, "expr", tu));
                yield out;
            }
            case "NestedNameSpecifier" -> nestedName(v.union("value"), tu);
            default -> throw unknown(v);
        };
    }

    private static TemplateArgument templateArgument(WireTable v, Tu tu) {
        var nodes = tu.nodes();
        switch (v.spec.name) {
            case "TemplateDeclaration": {
                var out = new TemplateArgumentDeclaration();
                nodes.queueSetNode(out, TemplateArgumentDeclaration.DECL, ref(v, "decl", tu));
                return out;
            }
            case "TemplateNullPtr": {
                var out = new TemplateArgumentNullPtr();
                nodes.queueSetNode(out, TemplateArgumentNullPtr.TYPE, ref(v, "type", tu));
                return out;
            }
            case "TemplateType": {
                var out = new TemplateArgumentType();
                nodes.queueSetNode(out, TemplateArgumentType.TYPE, ref(v, "type", tu));
                return out;
            }
            case "TemplateExpression": {
                var out = new TemplateArgumentExpr();
                nodes.queueSetNode(out, TemplateArgumentExpr.EXPR, ref(v, "expr", tu));
                return out;
            }
            case "TemplateStructuralValue": {
                var out = new TemplateArgumentStructuralValue();
                nodes.queueSetNode(out, TemplateArgumentStructuralValue.TYPE, ref(v, "type", tu));
                return out;
            }
            case "TemplatePack": {
                var arguments = new ArrayList<TemplateArgument>();
                for (int i = 0; i < v.length("arguments"); i++) {
                    arguments.add(templateArgument(v.tableAt("arguments", i).union("value"), tu));
                }
                var out = new TemplateArgumentPack();
                out.set(TemplateArgumentPack.PACK, arguments);
                return out;
            }
            case "TemplateIntegral": {
                var out = new TemplateArgumentIntegral();
                out.set(TemplateArgumentIntegral.INTEGRAL, new BigInteger(v.string("integral")).intValueExact());
                return out;
            }
            case "TemplateName":
                return templateName(v, tu);
            case "TemplateExpansion": {
                var out = new TemplateArgumentTemplateExpansion();
                out.set(TemplateArgumentTemplateExpansion.NUM_EXPANSIONS, v.has("num_expansions")
                        ? Optional.of(Math.toIntExact(v.integer("num_expansions")))
                        : Optional.empty());
                out.set(TemplateArgumentTemplateExpansion.TEMPLATE, templateName(v.table("template_name"), tu));
                return out;
            }
            default:
                throw unknown(v);
        }
    }

    private static TemplateArgumentTemplate templateName(WireTable name, Tu tu) {
        var nodes = tu.nodes();
        var v = name.union("value");
        var kind = switch (v.spec.name) {
            case "DirectTemplateName" -> TemplateNameKind.Template;
            case "QualifiedTemplateName" -> TemplateNameKind.QualifiedTemplate;
            case "SubstitutedTemplateName" -> TemplateNameKind.SubstTemplateTemplateParm;
            case "UsingTemplateName" -> TemplateNameKind.UsingTemplate;
            case "DependentTemplateName" -> TemplateNameKind.DependentTemplate;
            default -> throw unknown(v);
        };
        var out = TemplateArgumentTemplate.newInstance(kind);
        out.set(TemplateArgumentTemplate.TEMPLATE_NAME_KIND, kind);
        switch (kind) {
            case Template -> nodes.queueSetOptionalNode(out, Template.TEMPLATE_DECL, ref(v, "template_decl", tu));
            case QualifiedTemplate -> {
                out.set(QualifiedTemplate.QUALIFIER, v.string("qualifier"));
                out.set(QualifiedTemplate.HAS_TEMPLATE_KEYWORD, v.bool("has_template_keyword"));
                nodes.queueSetNode(out, QualifiedTemplate.TEMPLATE_DECL, ref(v, "template_decl", tu));
            }
            case SubstTemplateTemplateParm -> {
                nodes.queueSetNode(out, SubstTemplateTemplateParm.PARAMETER, ref(v, "parameter", tu));
                out.set(SubstTemplateTemplateParm.REPLACEMENT, templateName(v.table("replacement"), tu));
            }
            case UsingTemplate -> nodes.queueSetNode(out, UsingTemplate.USING_SHADOW_DECL,
                    ref(v, "using_shadow_decl", tu));
            case DependentTemplate -> {
                out.set(DependentTemplate.QUALIFIER, v.string("qualifier"));
                out.set(DependentTemplate.NAME, v.string("name"));
            }
            default -> throw unknown(v);
        }
        return out;
    }

    private static CXXCtorInitializer ctorInitializer(WireTable v, Tu tu) {
        var nodes = tu.nodes();
        var target = v.union("target");
        CXXCtorInitializer out;
        switch (target.spec.name) {
            case "AnyMemberInitializer" -> {
                out = CXXCtorInitializer.newInstance(CXXCtorInitializerKind.ANY_MEMBER_INITIALIZER);
                nodes.queueSetNode(out, AnyMemberInit.ANY_MEMBER_DECL, ref(target, "any_member_decl", tu));
            }
            case "BaseInitializer" -> {
                out = CXXCtorInitializer.newInstance(CXXCtorInitializerKind.BASE_INITIALIZER);
                nodes.queueSetNode(out, BaseInit.BASE_CLASS, ref(target, "base_class", tu));
            }
            case "DelegatingInitializer" -> {
                out = CXXCtorInitializer.newInstance(CXXCtorInitializerKind.DELEGATING_INITIALIZER);
                nodes.queueSetNode(out, DelegatingInit.DELEGATED_TYPE, ref(target, "delegated_type", tu));
            }
            default -> throw unknown(target);
        }
        nodes.queueSetNode(out, CXXCtorInitializer.INIT_EXPR, ref(v, "init_expr", tu));
        out.set(CXXCtorInitializer.IS_IN_CLASS_MEMBER_INITIALIZER, v.bool("is_in_class_member_initializer"));
        out.set(CXXCtorInitializer.IS_WRITTEN, v.bool("is_written"));
        return out;
    }

    private static ExceptionSpecification exceptionSpecification(WireTable v, Tu tu) {
        var nodes = tu.nodes();
        var out = ExceptionSpecificationType.values()[(int) v.integer("kind")].newInstance();
        var types = new ArrayList<String>();
        for (int i = 0; i < v.length("exception_types"); i++) {
            types.add(tu.id(v.integerAt("exception_types", i)));
        }
        nodes.queueSetNodeList(out, ExceptionSpecification.EXCEPTION_TYPES, types);
        var details = v.union("details");
        switch (details.spec.name) {
            case "NoExceptionDetails" -> {
            }
            case "ComputedExceptionDetails" -> nodes.queueSetNode(out, ComputedNoexcept.NOEXCEPT_EXPR,
                    ref(details, "noexcept_expr", tu));
            case "UnevaluatedExceptionDetails" -> nodes.queueSetNode(out,
                    UnevaluatedExceptionSpecification.SOURCE_DECL, ref(details, "source_decl", tu));
            case "UninstantiatedExceptionDetails" -> {
                nodes.queueSetNode(out, UninstantiatedExceptionSpecification.SOURCE_DECL,
                        ref(details, "source_decl", tu));
                nodes.queueSetNode(out, UninstantiatedExceptionSpecification.SOURCE_TEMPLATE,
                        ref(details, "source_template", tu));
            }
            default -> throw unknown(details);
        }
        return out;
    }

    private static OffsetOfComponent offsetOfComponent(WireTable v, Tu tu) {
        var nodes = tu.nodes();
        switch (v.spec.name) {
            case "OffsetArray": {
                var out = OffsetOfComponent.newInstance(OffsetOfComponentKind.ARRAY);
                nodes.queueSetNode(out, OffsetOfArray.EXPR, ref(v, "expr", tu));
                return out;
            }
            case "OffsetBase": {
                var out = OffsetOfComponent.newInstance(OffsetOfComponentKind.BASE);
                nodes.queueSetNode(out, OffsetOfBase.TYPE, ref(v, "type", tu));
                return out;
            }
            case "OffsetField": {
                var out = OffsetOfComponent.newInstance(OffsetOfComponentKind.FIELD);
                out.set(OffsetOfField.FIELD_NAME, v.string("field_name"));
                return out;
            }
            case "OffsetIdentifier": {
                var out = OffsetOfComponent.newInstance(OffsetOfComponentKind.IDENTIFIER);
                out.set(OffsetOfIdentifier.FIELD_NAME, v.string("field_name"));
                return out;
            }
            default:
                throw unknown(v);
        }
    }

    private static NestedNameSpecifier nestedName(WireTable v, Tu tu) {
        var nodes = tu.nodes();
        var kind = switch (v.spec.name) {
            case "NamespaceSpecifier" -> NestedNameSpecifierKind.Namespace;
            case "NamespaceAliasSpecifier" -> NestedNameSpecifierKind.NamespaceAlias;
            case "TypeSpecifier" -> NestedNameSpecifierKind.TypeSpec;
            case "TypeWithTemplateSpecifier" -> NestedNameSpecifierKind.TypeSpecWithTemplate;
            case "GlobalSpecifier" -> NestedNameSpecifierKind.Global;
            case "SuperSpecifier" -> NestedNameSpecifierKind.Super;
            default -> throw unknown(v);
        };
        var out = NestedNameSpecifier.newInstance(kind);
        switch (kind) {
            case Namespace -> nodes.queueSetNode(out, NamespaceSpecifier.NAMESPACE, ref(v, "namespace_decl", tu));
            case NamespaceAlias -> nodes.queueSetNode(out, NamespaceAliasSpecifier.NAMESPACE_ALIAS,
                    ref(v, "namespace_alias", tu));
            case TypeSpec -> nodes.queueSetNode(out, TypeSpecSpecifier.TYPE, ref(v, "type", tu));
            case TypeSpecWithTemplate -> nodes.queueSetNode(out, TypeSpecWithTemplateSpecifier.TYPE,
                    ref(v, "type", tu));
            case Super -> nodes.queueSetNode(out, SuperSpecifier.SUPER, ref(v, "super_decl", tu));
            case Global -> {
            }
            default -> throw unknown(v);
        }
        return out;
    }

    private static AccessSpecifier access(WireTable v, String field) {
        return AccessSpecifier.values()[(int) v.integer(field)];
    }

    private static String ref(WireTable v, String field, Tu tu) {
        return tu.id(v.integer(field));
    }

    private static IllegalArgumentException unknown(WireTable v) {
        return new IllegalArgumentException("Unsupported compound wire table " + v.spec);
    }
}
