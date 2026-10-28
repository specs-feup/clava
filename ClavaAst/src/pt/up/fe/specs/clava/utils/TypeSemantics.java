/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.type.*;
import pt.up.fe.specs.clava.ast.type.data.ExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.enums.*;
import pt.up.fe.specs.clava.language.ReferenceQualifier;

/** Typed evidence for binding generated declarations. Unknown evidence never compares equal as a type. */
public final class TypeSemantics {
    public sealed interface Shape permits Builtin, Qualified, Pointer, Reference, Array, FunctionTypeShape,
            MemberPointer, Nominal, TemplateParameter, Expansion, Complex {}
    public record Builtin(BuiltinKind kind) implements Shape {}
    public record AddressSpace(AddressSpaceQualifierV2 kind, long number) {}
    public record Qualified(Shape value, Set<C99Qualifier> qualifiers, AddressSpace addressSpace) implements Shape {
        public Qualified { qualifiers = Set.copyOf(qualifiers); }
    }
    public record Pointer(Shape pointee) implements Shape {}
    public record Reference(boolean rvalue, Shape referred) implements Shape {}
    public record Array(Shape element, Optional<BigInteger> bound, boolean variable) implements Shape {}
    public record MemberPointer(Shape owner, Shape pointee) implements Shape {}
    public record Nominal(Object entity) implements Shape {}
    public record TemplateParameter(int depth, int index, boolean pack) implements Shape {}
    public record Expansion(Shape pattern) implements Shape {}
    public record Complex(Shape element) implements Shape {}
    public enum ExceptionBehavior { THROWING, NON_THROWING }
    public record FunctionTypeShape(Shape returns, List<Shape> parameters, boolean variadic, boolean isConst,
            boolean isVolatile, ReferenceQualifier refQualifier, Object callingConvention, ExceptionBehavior exceptions) implements Shape {
        public FunctionTypeShape { parameters = List.copyOf(parameters); }
    }
    public record Evidence(Shape shape, String reason) {
        public boolean known() { return shape != null; }
        public static Evidence unknown(String reason) { return new Evidence(null, Objects.requireNonNull(reason)); }
        public static Evidence known(Shape shape) { return new Evidence(Objects.requireNonNull(shape), ""); }
    }
    public enum Verdict { COMPATIBLE, INCOMPATIBLE, UNSUPPORTED }
    private final Function<TagDecl, Object> nominal;
    private final IdentityHashMap<Type, Evidence> memo = new IdentityHashMap<>();
    private final Set<Type> active = Collections.newSetFromMap(new IdentityHashMap<>());
    public TypeSemantics() { this(TypeSemantics::declaredNominal); }
    public TypeSemantics(Function<TagDecl, Object> nominal) { this.nominal = nominal; }
    public static Object declaredNominal(TagDecl declaration) {
        return PostEditBindings.identity(declaration);
    }

    public Evidence type(Type type) {
        if (memo.containsKey(type)) return memo.get(type);
        if (!active.add(type)) return Evidence.unknown("cyclic type sugar");
        Evidence result;
        try { result = read(type); }
        finally { active.remove(type); }
        memo.put(type, result);
        return result;
    }
    private Shape require(Type value) {
        var proof = type(value);
        if (!proof.known()) throw new Unknown(proof.reason());
        return proof.shape();
    }
    private static final class Unknown extends RuntimeException { Unknown(String reason) { super(reason); } }
    private Evidence read(Type value) {
        try {
            if (value instanceof BuiltinType builtin) {
                return switch (builtin.get(BuiltinType.KIND)) {
                    case Dependent, Overload, BoundMember, UnknownAny, BuiltinFn -> Evidence.unknown("unresolved builtin");
                    default -> Evidence.known(new Builtin(builtin.get(BuiltinType.KIND)));
                };
            }
            if (value instanceof QualType qualified) {
                var shape = require(qualified.getUnqualifiedType());
                var qualifiers = new HashSet<>(qualified.get(QualType.C99_QUALIFIERS));
                var space = new AddressSpace(qualified.get(QualType.ADDRESS_SPACE_QUALIFIER), qualified.get(QualType.ADDRESS_SPACE));
                if (space.kind() == AddressSpaceQualifierV2.NONE && space.number() <= 0) space = new AddressSpace(space.kind(), 0);
                if (shape instanceof Qualified inner) {
                    qualifiers.addAll(inner.qualifiers());
                    if (space.number() == 0 && space.kind() == AddressSpaceQualifierV2.NONE) space = inner.addressSpace();
                    else if (!space.equals(inner.addressSpace()) && (inner.addressSpace().number() != 0
                            || inner.addressSpace().kind() != AddressSpaceQualifierV2.NONE)) throw new Unknown("conflicting address spaces");
                    shape = inner.value();
                }
                return Evidence.known(qualify(shape, qualifiers, space));
            }
            if (value instanceof TypedefType alias) return type(alias.desugar());
            if (value instanceof DecayedType decay) return type(decay.get(DecayedType.DECAYED_TYPE));
            if (value instanceof AdjustedType adjusted) return type(adjusted.getAdjustedType());
            if (value instanceof ElaboratedType elaborated) return type(elaborated.getNamedType());
            if (value instanceof ParenType paren) return type(paren.getInnerType());
            if (value instanceof AttributedType attributed) return type(attributed.getEquivalentType());
            if (value instanceof AutoType automatic) return automatic.getDeducedType().map(this::type).orElseGet(() -> Evidence.unknown("undeduced auto"));
            if (value instanceof SubstTemplateTypeParmType substitution) return type(substitution.get(SubstTemplateTypeParmType.REPLACEMENT_TYPE));
            if (value instanceof UnaryTransformType transform) return transform.getUnderlyingType().map(this::type)
                    .orElseGet(() -> Evidence.unknown("unresolved unary type transform"));
            if (value instanceof PointerType pointer) return Evidence.known(new Pointer(require(pointer.getPointeeType())));
            if (value instanceof ReferenceType reference) return Evidence.known(new Reference(value instanceof RValueReferenceType, require(reference.getReferencee())));
            if (value instanceof MemberPointerType pointer) return Evidence.known(new MemberPointer(require(pointer.getClassType()), require(pointer.getPointeeType())));
            if (value instanceof ConstantArrayType array) return Evidence.known(new Array(require(array.getElementType()), Optional.of(array.get(ConstantArrayType.ARRAY_SIZE)), false));
            if (value instanceof IncompleteArrayType array) return Evidence.known(new Array(require(array.getElementType()), Optional.empty(), false));
            if (value instanceof VariableArrayType array) return Evidence.known(new Array(require(array.getElementType()), Optional.empty(), true));
            if (value instanceof TagType tag) {
                Object entity = nominal.apply(tag.getDecl());
                return entity == null ? Evidence.unknown("unbound nominal declaration") : Evidence.known(new Nominal(entity));
            }
            if (value instanceof TemplateTypeParmType parameter) return Evidence.known(new TemplateParameter(parameter.get(TemplateTypeParmType.DEPTH),
                    parameter.get(TemplateTypeParmType.INDEX), parameter.get(TemplateTypeParmType.IS_PACKED)));
            if (value instanceof PackExpansionType expansion) return Evidence.known(new Expansion(require(expansion.get(PackExpansionType.PATTERN))));
            if (value instanceof ComplexType complex) return Evidence.known(new Complex(require(complex.get(ComplexType.ELEMENT_TYPE))));
            if (value instanceof FunctionProtoType function) {
                var params = new ArrayList<Shape>();
                for (var parameter : function.getParamTypes()) {
                    var proof = parameter(parameter); if (!proof.known()) throw new Unknown(proof.reason()); params.add(proof.shape());
                }
                var exceptions = exceptions(function);
                if (exceptions == null) return Evidence.unknown("unevaluated exception specification");
                return Evidence.known(new FunctionTypeShape(require(function.getReturnType()), params, function.isVariadic(),
                        function.get(FunctionType.IS_CONST), function.get(FunctionType.IS_VOLATILE),
                        function.get(FunctionProtoType.REFERENCE_QUALIFIER), function.get(FunctionType.CALLING_CONVENTION), exceptions));
            }
            // Only sugar that the producer resolved may establish a template specialization's nominal type.
            if ((value instanceof TemplateSpecializationType || value instanceof DecltypeType || value instanceof UsingType)
                    && value.get(Type.UNQUALIFIED_DESUGARED_TYPE).filter(type -> type != value).isPresent())
                return type(value.get(Type.UNQUALIFIED_DESUGARED_TYPE).get());
            return Evidence.unknown("unsupported type: " + value.getClass().getSimpleName());
        } catch (Unknown unknown) { return Evidence.unknown(unknown.getMessage()); }
    }
    private static Shape qualify(Shape value, Set<C99Qualifier> qualifiers, AddressSpace space) {
        return qualifiers.isEmpty() && space.kind() == AddressSpaceQualifierV2.NONE && space.number() == 0
                ? value : new Qualified(value, qualifiers, space);
    }
    public Evidence parameter(Type value) {
        var evidence = type(value);
        if (!evidence.known()) return evidence;
        Shape shape = evidence.shape();
        if (shape instanceof Qualified qualified) {
            if (qualified.value() instanceof Array array) shape = new Array(qualify(array.element(), qualified.qualifiers(), qualified.addressSpace()), array.bound(), array.variable());
            else shape = qualify(qualified.value(), Set.of(), qualified.addressSpace());
        }
        if (shape instanceof Array array) shape = new Pointer(array.element());
        else if (shape instanceof FunctionTypeShape) shape = new Pointer(shape);
        return Evidence.known(shape);
    }
    public static ExceptionBehavior exceptions(FunctionProtoType function) {
        return switch (function.get(FunctionProtoType.EXCEPTION_SPECIFICATION).get(ExceptionSpecification.EXCEPTION_SPECIFICATION_TYPE)) {
            case None, NoexceptFalse, MSAny -> ExceptionBehavior.THROWING;
            case DynamicNone, BasicNoexcept, NoexceptTrue, NoThrow -> ExceptionBehavior.NON_THROWING;
            default -> null;
        };
    }
    public Verdict compare(Type left, Type right, boolean incompleteExternalArray) {
        var a = type(left); var b = type(right);
        if (!a.known() || !b.known()) return Verdict.UNSUPPORTED;
        if (a.shape().equals(b.shape())) return Verdict.COMPATIBLE;
        if (incompleteExternalArray && a.shape() instanceof Array first && b.shape() instanceof Array second
                && !first.variable() && !second.variable() && first.element().equals(second.element())
                && (first.bound().isEmpty() || second.bound().isEmpty())) return Verdict.COMPATIBLE;
        return Verdict.INCOMPATIBLE;
    }
}
