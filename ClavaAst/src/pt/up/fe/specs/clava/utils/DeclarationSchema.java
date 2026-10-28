/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.util.*;
import java.util.function.Function;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.decl.data.templates.*;
import pt.up.fe.specs.clava.ast.type.*;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.language.ReferenceQualifier;

/** Overload selection and declaration compatibility are separate, typed questions. */
public final class DeclarationSchema {
    public record FunctionSelector(List<TypeSemantics.Shape> parameters, boolean variadic, boolean member,
            boolean isConst, boolean isVolatile, ReferenceQualifier ref, Object convention,
            List<Object> templateParameters, List<Object> templateArguments) {
        public FunctionSelector {
            parameters = List.copyOf(parameters); templateParameters = List.copyOf(templateParameters);
            templateArguments = List.copyOf(templateArguments);
        }
    }
    public record RecordSelector(String category, List<Object> arguments) { public RecordSelector { arguments = List.copyOf(arguments); } }
    public record TemplateParameter(String kind, Object type, boolean pack) {}
    public record ArgumentShape(String kind, Object value) {}
    public record Profile(DeclarationContext.Symbol symbol, Object selector, TypeSemantics.Evidence type, String unsupported) {
        public boolean supported() { return unsupported.isEmpty(); }
    }
    private final App app;
    private final TypeSemantics types;
    public DeclarationSchema(App app) { this(app, TypeSemantics::declaredNominal); }
    public DeclarationSchema(App app, Function<TagDecl, Object> nominal) { this.app = app; types = new TypeSemantics(nominal); }
    public TypeSemantics types() { return types; }
    public Profile describe(NamedDecl declaration) {
        var symbol = DeclarationContext.symbol(declaration, app);
        try {
            if (declaration instanceof FunctionDecl function) {
                if (!(function.getFunctionType() instanceof FunctionProtoType type)) return unknown(symbol, "function has no prototype");
                var params = new ArrayList<TypeSemantics.Shape>();
                if (function.getParameters().size() != type.getParamTypes().size())
                    return unknown(symbol, "declaration parameters disagree with its function type");
                for (int i = 0; i < function.getParameters().size(); i++) {
                    var parameter = types.parameter(function.getParameters().get(i).getType());
                    var canonical = types.parameter(type.getParamTypes().get(i));
                    if (!parameter.known() || !canonical.known()) return unknown(symbol, !parameter.known() ? parameter.reason() : canonical.reason());
                    if (!parameter.shape().equals(canonical.shape())) return unknown(symbol, "parameter node and signature type disagree");
                    params.add(parameter.shape());
                }
                boolean member = function instanceof CXXMethodDecl;
                var head = function.getParent() instanceof FunctionTemplateDecl template ? template.getTemplateParameters()
                        : function.get(FunctionDecl.TEMPLATE_PARAMETERS);
                var templates = new ArrayList<Object>();
                for (var parameter : head) {
                    if (parameter instanceof TemplateTypeParmDecl value)
                        templates.add(new TemplateParameter("type", "", value.get(TemplateTypeParmDecl.IS_PARAMETER_PACK)));
                    else if (parameter instanceof NonTypeTemplateParmDecl value) {
                        var proof = types.type(value.getType());
                        if (!proof.known()) return unknown(symbol, proof.reason());
                        templates.add(new TemplateParameter("value", proof.shape(), value.get(NonTypeTemplateParmDecl.IS_PARAMETER_PACK)));
                    } else return unknown(symbol, "template-template parameter binding needs compiler evidence");
                }
                var selector = new FunctionSelector(params, type.isVariadic(), member, member && type.get(FunctionType.IS_CONST),
                        member && type.get(FunctionType.IS_VOLATILE), member ? type.get(FunctionProtoType.REFERENCE_QUALIFIER) : ReferenceQualifier.None,
                        type.get(FunctionType.CALLING_CONVENTION), templates, arguments(function.get(FunctionDecl.TEMPLATE_ARGUMENTS)));
                var fullType = types.type(function.getType());
                return fullType.known() ? new Profile(symbol, selector, fullType, "") : unknown(symbol, fullType.reason());
            }
            if (declaration instanceof TagDecl tag) {
                String category = tag instanceof EnumDecl ? "enum" : tag.get(TagDecl.TAG_KIND).toString().equalsIgnoreCase("union") ? "union" : "record";
                List<Object> args = tag instanceof ClassTemplateSpecializationDecl specialization
                        ? arguments(specialization.get(ClassTemplateSpecializationDecl.TEMPLATE_ARGUMENTS)) : List.of();
                return new Profile(symbol, new RecordSelector(category, args), TypeSemantics.Evidence.unknown("nominal declaration"), "");
            }
            if (declaration instanceof VarDecl variable) {
                var proof = types.type(variable.getType());
                return proof.known() ? new Profile(symbol, "object", proof, "") : unknown(symbol, proof.reason());
            }
            return unknown(symbol, "declaration family is not bindable");
        } catch (Unknown unknown) { return unknown(symbol, unknown.getMessage()); }
    }
    private static Profile unknown(DeclarationContext.Symbol symbol, String reason) {
        return new Profile(symbol, null, TypeSemantics.Evidence.unknown(reason), reason);
    }
    private static final class Unknown extends RuntimeException { Unknown(String message) { super(message); } }
    private List<Object> arguments(List<TemplateArgument> arguments) {
        if (arguments == null) return List.of();
        var result = new ArrayList<Object>();
        for (var argument : arguments) {
            if (argument instanceof TemplateArgumentType value) {
                var proof = types.type(value.get(TemplateArgumentType.TYPE));
                if (!proof.known()) throw new Unknown(proof.reason());
                result.add(new ArgumentShape("type", proof.shape()));
            } else if (argument instanceof TemplateArgumentNullPtr value) {
                var proof = types.type(value.get(TemplateArgumentNullPtr.TYPE));
                if (!proof.known()) throw new Unknown(proof.reason());
                result.add(new ArgumentShape("nullptr", proof.shape()));
            } else if (argument instanceof TemplateArgumentPack value)
                result.add(new ArgumentShape("pack", arguments(value.get(TemplateArgumentPack.PACK))));
            else if (argument instanceof TemplateArgumentDeclaration value && value.get(TemplateArgumentDeclaration.DECL) instanceof NamedDecl named) {
                Object entity = named.get(NamedDecl.EDITED_ENTITY).orElseGet(() -> named.hasValue(NamedDecl.USR)
                        ? CompilerEntityIdentity.key(named).orElse(null) : new DeclarationContext.Reference(named));
                if (entity == null) throw new Unknown("unresolved declaration template argument");
                result.add(new ArgumentShape("declaration", entity));
            } else throw new Unknown("template argument has no complete compiler type/value evidence");
        }
        return List.copyOf(result);
    }
    public TypeSemantics.Verdict compatible(NamedDecl left, NamedDecl right) {
        if (left instanceof TagDecl a && right instanceof TagDecl b) {
            var first = describe(a); var second = describe(b);
            if (!first.supported() || !second.supported()) return TypeSemantics.Verdict.UNSUPPORTED;
            if (!first.selector().equals(second.selector())) return TypeSemantics.Verdict.INCOMPATIBLE;
            if (a instanceof EnumDecl x && b instanceof EnumDecl y) {
                if ((x.get(EnumDecl.ENUM_SCOPE_KIND) == EnumDecl.EnumScopeType.NO_SCOPE)
                        != (y.get(EnumDecl.ENUM_SCOPE_KIND) == EnumDecl.EnumScopeType.NO_SCOPE)) return TypeSemantics.Verdict.INCOMPATIBLE;
                if (x.get(EnumDecl.INTEGER_TYPE).isPresent() && y.get(EnumDecl.INTEGER_TYPE).isPresent())
                    return types.compare(x.getIntegerType(), y.getIntegerType(), false);
            }
            return TypeSemantics.Verdict.COMPATIBLE;
        }
        if (!(left instanceof ValueDecl a) || !(right instanceof ValueDecl b)) return TypeSemantics.Verdict.INCOMPATIBLE;
        if (a instanceof VarDecl x && b instanceof VarDecl y && x.get(VarDecl.TLS_KIND) != y.get(VarDecl.TLS_KIND))
            return TypeSemantics.Verdict.INCOMPATIBLE;
        if (a instanceof FunctionDecl x && b instanceof FunctionDecl y
                && x.getFunctionType().get(FunctionType.CALLING_CONVENTION) != y.getFunctionType().get(FunctionType.CALLING_CONVENTION))
            return TypeSemantics.Verdict.INCOMPATIBLE;
        boolean nativePair = left.hasValue(NamedDecl.USR) && right.hasValue(NamedDecl.USR)
                && left.get(NamedDecl.EDITED_ENTITY).isEmpty() && right.get(NamedDecl.EDITED_ENTITY).isEmpty();
        boolean incomplete = a instanceof VarDecl x && b instanceof VarDecl y
                && ((x.get(VarDecl.STORAGE_CLASS) == pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Extern && !x.hasInit())
                    || (y.get(VarDecl.STORAGE_CLASS) == pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Extern && !y.hasInit()));
        if (nativePair && !left.get(NamedDecl.USR_TYPE_GENERATION_FAILED) && !right.get(NamedDecl.USR_TYPE_GENERATION_FAILED)) {
            if (left.get(NamedDecl.USR_TYPE).equals(right.get(NamedDecl.USR_TYPE))) return TypeSemantics.Verdict.COMPATIBLE;
            if (!incomplete) return TypeSemantics.Verdict.INCOMPATIBLE;
        }
        return types.compare(a.getType(), b.getType(), incomplete);
    }
}
