/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.decl.data.templates.*;
import pt.up.fe.specs.clava.ast.decl.enums.Linkage;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.type.*;
import pt.up.fe.specs.clava.ast.type.data.ExceptionSpecification;
import pt.up.fe.specs.clava.language.ReferenceQualifier;

/**
 * Semantic declaration identity, independent of display signatures and AST-occurrence normalization.
 * Instances belong to one declaration-cache generation. Unsupported types retain object identity rather than
 * falling back to a spelling that could connect unrelated entities.
 */
public final class DeclarationIdentity {
    public record Name(String scope, String spelling, Object domain) {}
    public record TypeKey(String kind, List<?> components) {
        public TypeKey { components = List.copyOf(components); }
    }
    public record FunctionKey(Name name, List<TypeKey> parameters, boolean variadic,
            boolean isConst, boolean isVolatile, ReferenceQualifier refQualifier,
            Object callingConvention, String languageLinkage, List<?> templateParameters, List<?> templateArguments) {
        public FunctionKey {
            parameters = List.copyOf(parameters);
            templateParameters = List.copyOf(templateParameters);
            templateArguments = List.copyOf(templateArguments);
        }
    }
    public record TagKey(Name name, String kind, List<?> arguments) {
        public TagKey { arguments = List.copyOf(arguments); }
    }
    public record VariableKey(Name name, Object threadStorage) {}

    private static final class ObjectIdentity {
        private final Object object;
        ObjectIdentity(Object object) { this.object = object; }
        @Override public boolean equals(Object other) { return other instanceof ObjectIdentity id && object == id.object; }
        @Override public int hashCode() { return System.identityHashCode(object); }
        @Override public String toString() { return "unresolved " + object.getClass().getSimpleName(); }
    }

    private final IdentityHashMap<Type, TypeKey> types = new IdentityHashMap<>();
    private final Set<Type> activeTypes = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<String, Boolean> cppUnits = new HashMap<>();

    public DeclarationIdentity() {}
    public DeclarationIdentity(App app) {
        for (var tu : app.getTranslationUnits()) {
            cppUnits.put(path(tu.getFile().getPath()), tu.get(TranslationUnit.LANGUAGE).get(Language.C_PLUS_PLUS));
        }
    }

    public FunctionKey function(FunctionDecl function) {
        var type = function.getFunctionType();
        var proto = type instanceof FunctionProtoType p ? p : null;
        boolean member = function instanceof CXXMethodDecl;
        String linkage = member ? "C++" : languageLinkage(function);
        Name name = name(function);
        // C language linkage identifies an external function independently of the namespace of its declaration.
        if (linkage.equals("C") && name.domain().equals("external")) name = new Name("", name.spelling(), name.domain());
        return new FunctionKey(name,
                function.getParameters().stream().map(p -> parameter(p.get(ValueDecl.TYPE))).toList(),
                type.isVariadic(), member && type.get(FunctionType.IS_CONST),
                member && type.get(FunctionType.IS_VOLATILE),
                member && proto != null ? proto.get(FunctionProtoType.REFERENCE_QUALIFIER) : ReferenceQualifier.None,
                type.get(FunctionType.CALLING_CONVENTION), linkage,
                templateParameters(function), arguments(function.get(FunctionDecl.TEMPLATE_ARGUMENTS)));
    }

    public TagKey tag(TagDecl tag) {
        return new TagKey(name(tag), tag instanceof EnumDecl ? "enum"
                : tag.get(TagDecl.TAG_KIND).name().equalsIgnoreCase("union") ? "union" : "record",
                tag instanceof ClassTemplateSpecializationDecl specialization
                        ? arguments(specialization.get(ClassTemplateSpecializationDecl.TEMPLATE_ARGUMENTS)) : List.of());
    }

    public VariableKey variable(VarDecl variable) {
        return new VariableKey(name(variable), variable.get(VarDecl.TLS_KIND));
    }

    public boolean compatibleVariableTypes(Type first, Type second) {
        var left = type(first);
        var right = type(second);
        if (left.equals(right)) return true;
        // An extern array declaration may leave the first bound unspecified until its definition.
        if ((left.kind().equals("incomplete-array") && right.kind().equals("array"))
                || (left.kind().equals("array") && right.kind().equals("incomplete-array")))
            return left.components().get(0).equals(right.components().get(0));
        return false;
    }

    public Name name(NamedDecl decl) {
        String scope = decl.get(NamedDecl.QUALIFIED_PREFIX);
        if (scope.isEmpty() && decl instanceof CXXMethodDecl method) {
            scope = method.getRecordDecl().map(record -> qualified(record)).orElse("");
        }
        if (scope.isEmpty()) {
            var ancestors = new ArrayList<String>();
            for (ClavaNode node = decl.getParent(); node != null; node = node.getParent()) {
                if (node instanceof NamespaceDecl || node instanceof TagDecl) ancestors.add(0, ((NamedDecl) node).getDeclName());
            }
            scope = String.join("::", ancestors);
        }
        Object domain;
        Linkage linkage = decl.hasValue(NamedDecl.LINKAGE) ? decl.get(NamedDecl.LINKAGE) : Linkage.Invalid;
        boolean staticFunction = decl instanceof FunctionDecl f && !(f instanceof CXXMethodDecl)
                && f.get(FunctionDecl.STORAGE_CLASS) == StorageClass.Static;
        if (linkage == Linkage.Invalid && decl instanceof CXXMethodDecl method && method.getRecordDecl().isPresent()) {
            // Factory-created member declarations inherit the identity domain of their explicit owning record.
            domain = name(method.getRecordDecl().get()).domain();
        } else if (linkage == Linkage.External && !staticFunction && !decl.getDeclName().isEmpty()) {
            domain = "external";
        } else if ((linkage == Linkage.Internal || linkage == Linkage.UniqueExternal || staticFunction)
                && origin(decl) != null) {
            domain = List.of("translation-unit", origin(decl));
        } else {
            // No linkage, module ownership, or incomplete provenance cannot establish cross-TU equivalence.
            domain = new ObjectIdentity(decl);
        }
        return new Name(scope, decl.getDeclName(), domain);
    }

    private static String qualified(NamedDecl decl) {
        String prefix = decl.get(NamedDecl.QUALIFIED_PREFIX);
        return prefix.isEmpty() ? decl.getDeclName() : prefix + "::" + decl.getDeclName();
    }

    public static String origin(ClavaNode node) {
        var attached = node.getAncestorTry(TranslationUnit.class);
        if (attached.isPresent()) return path(attached.get().getFile().getPath());
        return node.get(ClavaNode.ORIGINATING_TRANSLATION_UNIT).map(DeclarationIdentity::path).orElse(null);
    }

    private String languageLinkage(NamedDecl decl) {
        var explicit = decl.getAncestorTry(LinkageSpecDecl.class);
        if (explicit.isPresent()) return explicit.get().get(LinkageSpecDecl.LINKAGE_TYPE).toString().equals("C") ? "C" : "C++";
        var tu = decl.getAncestorTry(TranslationUnit.class);
        boolean cpp = tu.map(unit -> unit.get(TranslationUnit.LANGUAGE).get(Language.C_PLUS_PLUS))
                .orElseGet(() -> cppUnits.getOrDefault(origin(decl), true));
        return cpp ? "C++" : "C";
    }

    private static String path(String value) { return Path.of(value).toAbsolutePath().normalize().toString(); }
    private static TypeKey key(String kind, Object... components) { return new TypeKey(kind, List.of(components)); }

    public TypeKey parameter(Type original) {
        Type adjusted = unwrap(original);
        // Parameter top-level cv-qualification is not part of a function's type. Pointee qualification remains.
        while (adjusted instanceof QualType qualifiers) {
            if (qualifiers.get(QualType.ADDRESS_SPACE_QUALIFIER) != pt.up.fe.specs.clava.ast.type.enums.AddressSpaceQualifierV2.NONE)
                return key("parameter-address-space", parameter(qualifiers.getUnqualifiedType()),
                        qualifiers.get(QualType.ADDRESS_SPACE_QUALIFIER), qualifiers.get(QualType.ADDRESS_SPACE));
            adjusted = unwrap(qualifiers.getUnqualifiedType());
        }
        if (adjusted instanceof ArrayType array) return key("pointer", type(array.getElementType()));
        if (adjusted instanceof FunctionType) return key("pointer", type(adjusted));
        return type(adjusted);
    }

    private Type unwrap(Type type) {
        var seen = java.util.Collections.newSetFromMap(new IdentityHashMap<Type, Boolean>());
        while (seen.add(type)) {
            Type next = type;
            if (type instanceof TypedefType alias) next = alias.desugar();
            else if (type instanceof DecayedType decay) next = decay.get(DecayedType.DECAYED_TYPE);
            else if (type instanceof AdjustedType adjusted) next = adjusted.getAdjustedType();
            else if (type instanceof ElaboratedType elaborated) next = elaborated.getNamedType();
            else if (type instanceof ParenType paren) next = paren.getInnerType();
            else if (type instanceof AttributedType attributed) next = attributed.getEquivalentType();
            else if (type instanceof AutoType auto && auto.getDeducedType().isPresent()) next = auto.getDeducedType().get();
            if (next == type) return type;
            type = next;
        }
        return type;
    }

    public TypeKey type(Type original) {
        var cached = types.get(original);
        if (cached != null) return cached;
        if (!activeTypes.add(original)) return key("recursive", new ObjectIdentity(original));
        TypeKey result;
        try { result = typeUncached(original); }
        finally { activeTypes.remove(original); }
        types.put(original, result);
        return result;
    }

    private TypeKey typeUncached(Type original) {
        Type value = unwrap(original);
        if (value != original) return type(value);
        if (value instanceof BuiltinType builtin) {
            var kind = builtin.get(BuiltinType.KIND);
            return switch (kind) {
                case Dependent, Overload, BoundMember, UnknownAny, BuiltinFn -> key("unresolved-builtin", new ObjectIdentity(value));
                default -> key("builtin", kind);
            };
        }
        if (value instanceof QualType qualifiers) return key("qualified", type(qualifiers.getUnqualifiedType()),
                qualifiers.get(QualType.C99_QUALIFIERS).stream().distinct().sorted().toList(),
                qualifiers.get(QualType.ADDRESS_SPACE_QUALIFIER), qualifiers.get(QualType.ADDRESS_SPACE));
        if (value instanceof PointerType pointer) return key("pointer", type(pointer.getPointeeType()));
        if (value instanceof ReferenceType reference) return key(value.getClass().getSimpleName(), type(reference.getReferencee()));
        if (value instanceof MemberPointerType pointer) return key("member-pointer", type(pointer.getClassType()), type(pointer.getPointeeType()));
        if (value instanceof ConstantArrayType array) return key("array", type(array.getElementType()), array.get(ConstantArrayType.ARRAY_SIZE));
        if (value instanceof IncompleteArrayType array) return key("incomplete-array", type(array.getElementType()));
        // C function parameter compatibility does not depend on the spelling of a runtime array bound.
        if (value instanceof VariableArrayType array) return key("variable-array", type(array.getElementType()));
        if (value instanceof TagType tag) return key("tag", tag(tag.getDecl()));
        if (value instanceof TemplateTypeParmType parameter) return key("template-parameter", parameter.get(TemplateTypeParmType.DEPTH),
                parameter.get(TemplateTypeParmType.INDEX), parameter.get(TemplateTypeParmType.IS_PACKED));
        if (value instanceof PackExpansionType pack) return key("pack-expansion", type(pack.get(PackExpansionType.PATTERN)));
        if (value instanceof ComplexType complex) return key("complex", type(complex.get(ComplexType.ELEMENT_TYPE)));
        if (value instanceof FunctionProtoType function) return key("function", type(function.getReturnType()),
                function.getParamTypes().stream().map(this::parameter).toList(), function.isVariadic(),
                function.get(FunctionType.IS_CONST), function.get(FunctionType.IS_VOLATILE),
                function.get(FunctionProtoType.REFERENCE_QUALIFIER), function.get(FunctionType.CALLING_CONVENTION),
                exceptionSpecification(function));
        if (value instanceof TemplateSpecializationType specialization) {
            var template = specialization.get(TemplateSpecializationType.TEMPLATE_DECL);
            if (template.isPresent()) return key("template", name(template.get()), arguments(specialization.getTemplateArguments()));
        }
        return key("unsupported", new ObjectIdentity(value));
    }

    /** Return types do not select overloads, but incompatible declarations cannot supply definitions. */
    public boolean compatibleFunctions(FunctionDecl declaration, FunctionDecl definition) {
        if (!type(declaration.getFunctionType().getReturnType()).equals(type(definition.getFunctionType().getReturnType()))) return false;
        if (declaration.getFunctionType() instanceof FunctionProtoType first
                && definition.getFunctionType() instanceof FunctionProtoType second)
            return exceptionSpecification(first).equals(exceptionSpecification(second));
        return true;
    }

    private Object exceptionSpecification(FunctionProtoType type) {
        var specification = type.get(FunctionProtoType.EXCEPTION_SPECIFICATION);
        return switch (specification.get(ExceptionSpecification.EXCEPTION_SPECIFICATION_TYPE)) {
            case None, NoexceptFalse, MSAny -> "potentially-throwing";
            case DynamicNone, BasicNoexcept, NoexceptTrue, NoThrow -> "non-throwing";
            // Dependent and unresolved specifications cannot be equated without evaluating their context.
            default -> new ObjectIdentity(specification);
        };
    }

    private List<?> templateParameters(FunctionDecl function) {
        List<NamedDecl> parameters = function.getParent() instanceof FunctionTemplateDecl template
                ? template.getTemplateParameters() : function.get(FunctionDecl.TEMPLATE_PARAMETERS);
        return parameters.stream().map(parameter -> {
            if (parameter instanceof TemplateTypeParmDecl type) return key("type-parameter", type.get(TemplateTypeParmDecl.IS_PARAMETER_PACK));
            if (parameter instanceof NonTypeTemplateParmDecl value) return key("value-parameter", type(value.getType()), value.get(NonTypeTemplateParmDecl.IS_PARAMETER_PACK));
            return key("unsupported-parameter", new ObjectIdentity(parameter));
        }).toList();
    }

    private List<?> arguments(List<TemplateArgument> arguments) {
        if (arguments == null) return List.of(); // Factory-created non-template functions have no wire payload.
        return arguments.stream().map(argument -> {
            if (argument instanceof TemplateArgumentType type) return key("type", type(type.get(TemplateArgumentType.TYPE)));
            // The wire representation has only a Java int, without original type or width.
            // Equal values cannot prove identity (e.g. template<auto>: 1 versus 1L).
            if (argument instanceof TemplateArgumentIntegral) return key("unresolved-integral", new ObjectIdentity(argument));
            if (argument instanceof TemplateArgumentNullPtr pointer) return key("nullptr", type(pointer.get(TemplateArgumentNullPtr.TYPE)));
            if (argument instanceof TemplateArgumentPack pack) return key("pack", arguments(pack.get(TemplateArgumentPack.PACK)));
            if (argument instanceof TemplateArgumentDeclaration declaration && declaration.get(TemplateArgumentDeclaration.DECL) instanceof NamedDecl named)
                return key("declaration", named instanceof FunctionDecl function ? function(function)
                        : named instanceof VarDecl variable ? variable(variable) : name(named));
            return key("unsupported-argument", new ObjectIdentity(argument));
        }).toList();
    }

    /** Global storage duration includes block statics; it is not itself proof of a global definition. */
    public static boolean isGlobalDefinition(VarDecl variable) {
        if (!variable.get(VarDecl.HAS_GLOBAL_STORAGE) || variable.getAncestorTry(FunctionDecl.class).isPresent()) return false;
        if (variable.get(VarDecl.IS_STATIC_DATA_MEMBER) && variable.getAncestorTry(CXXRecordDecl.class).isPresent()) return false;
        return variable.hasInit() || variable.get(VarDecl.STORAGE_CLASS) != StorageClass.Extern;
    }
}
