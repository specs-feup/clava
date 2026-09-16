/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.LongFunction;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.data.CXXBaseSpecifier;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.data.ExplicitSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.AnyMemberInit;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.BaseInit;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.DelegatingInit;
import pt.up.fe.specs.clava.ast.decl.data.nestedname.NamespaceAliasSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.nestedname.NamespaceSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.nestedname.NestedNameSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.nestedname.SuperSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.nestedname.TypeSpecSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.nestedname.TypeSpecWithTemplateSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentDeclaration;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentExpr;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentIntegral;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentPack;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentStructuralValue;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentTemplate;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentTemplateExpansion;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentType;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentNullPtr;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.DependentTemplate;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.QualifiedTemplate;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.SubstTemplateTemplateParm;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.Template;
import pt.up.fe.specs.clava.ast.decl.data.templates.template.UsingTemplate;
import pt.up.fe.specs.clava.ast.decl.enums.ExplicitSpecKind;
import pt.up.fe.specs.clava.ast.decl.enums.NestedNameSpecifierKind;
import pt.up.fe.specs.clava.ast.expr.data.designator.ArrayDesignator;
import pt.up.fe.specs.clava.ast.expr.data.designator.ArrayRangeDesignator;
import pt.up.fe.specs.clava.ast.expr.data.designator.Designator;
import pt.up.fe.specs.clava.ast.expr.data.designator.FieldDesignator;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfArray;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfBase;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfComponent;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfField;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfIdentifier;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfComponentKind;
import pt.up.fe.specs.clava.ast.expr.enums.DesignatorKind;
import pt.up.fe.specs.clava.ast.stmt.data.AsmInput;
import pt.up.fe.specs.clava.ast.stmt.data.AsmOutput;
import pt.up.fe.specs.clava.ast.type.data.ComputedNoexcept;
import pt.up.fe.specs.clava.ast.type.data.ExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.data.UnevaluatedExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.data.UninstantiatedExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.enums.ExceptionSpecificationType;
import pt.up.fe.specs.clava.ast.type.enums.TemplateNameKind;
import pt.up.fe.specs.clava.ast.attr.AlignedExprAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedTypeAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedAttr;
import pt.up.fe.specs.clava.ast.attr.enums.AlignedAttrKind;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.utils.ClassesService;

/**
 * Converts one generated protobuf node into the existing jOptions data store.
 *
 * <p>The generated message is walked only while the frame callback is active.
 * References are queued in {@link ClavaNodes}, exactly as they are for the
 * historical parser, so this class never builds a parallel protobuf AST.</p>
 */
final class ProtoNodeDataReader {

    private final ClangAstData data;
    private final LongFunction<String> id;
    private final ProtoAstReader.Files files;

    private ProtoNodeDataReader(ClangAstData data, LongFunction<String> id, ProtoAstReader.Files files) {
        this.data = data;
        this.id = id;
        this.files = files;
    }

    static DataStore read(Node wireNode, ClangAstData data, LongFunction<String> id, ProtoAstReader.Files files) {
        if (!wireNode.hasId() || wireNode.getId() <= 0) {
            throw new IllegalArgumentException("Node.id must be a positive dense id");
        }
        if (!wireNode.hasClassName() || wireNode.getClassName().isBlank()) {
            throw new IllegalArgumentException("Node.class_name is required for id " + wireNode.getId());
        }
        if (wireNode.getNodeCase() == Node.NodeCase.NODE_NOT_SET) {
            throw new IllegalArgumentException("Node payload is required for " + wireNode.getClassName());
        }

        Message payload = ProtoGeneratedBindings.payload(wireNode);

        Class<? extends ClavaNode> clavaClass = getClavaClass(wireNode.getClassName(), payload);
        StoreDefinition definition = StoreDefinitions.fromInterface(clavaClass);
        DataStore store = DataStore.newInstance(definition, true);
        String wireId = id.apply(wireNode.getId());
        store.set(ClavaNode.CONTEXT, data.get(ClangAstData.CONTEXT));
        store.set(ClavaNode.ID, wireId);

        ProtoNodeDataReader reader = new ProtoNodeDataReader(data, id, files);
        ProtoGeneratedBindings.validate(payload, wireNode.getClassName());
        reader.setSourceMetadata(ProtoGeneratedBindings.source(payload), store, wireNode.getClassName());
        ProtoGeneratedBindings.visit(payload, reader, store, wireNode.getClassName());
        return store;
    }

    private static Class<? extends ClavaNode> getClavaClass(String className, Message payload) {
        // Clang has one AlignedAttr kind whose Clava representation is split
        // into two concrete classes. The discriminator is part of the wire
        // payload, so resolve it before creating the store definition.
        if (className.equals("AlignedAttr")) {
            AlignedAttrData aligned = (AlignedAttrData) payload;
            if (!aligned.hasIsExpression() || !aligned.hasAlignment()) {
                throw new IllegalArgumentException("AlignedAttr requires is_expression and alignment");
            }
            return aligned.getIsExpression() ? AlignedExprAttr.class : AlignedTypeAttr.class;
        }
        return ClassesService.getClavaClass(className);
    }

    private void setSourceMetadata(SourceInfo sourceInfo, DataStore store, String className) {
        boolean hasLocation = sourceInfo != null && sourceInfo.hasExpansion();
        boolean isType = className.endsWith("Type") || className.equals("QualType");
        if (!isType && sourceInfo == null) {
            throw new IllegalArgumentException("Missing source information for " + className);
        }
        if (hasLocation) {
            SourceRange range = files.range(sourceInfo.getExpansion());
            if (range.isValid()) {
                store.set(ClavaNode.LOCATION, range);
            }
        }
        store.set(ClavaNode.IS_MACRO, sourceInfo != null && sourceInfo.hasIsMacro() && sourceInfo.getIsMacro());
        store.set(ClavaNode.IS_IN_SYSTEM_HEADER,
                sourceInfo != null && sourceInfo.hasSystemHeader() && sourceInfo.getSystemHeader());

        // Spelling ranges are intentionally walked and checked by Files.range,
        // even though Clava's current data model has no spelling-location key.
        if (sourceInfo != null && sourceInfo.hasSpelling()) {
            files.range(sourceInfo.getSpelling());
        }
    }

    void putScalar(DataStore store, DataKey<?> key, Object value) {
        set(key, store, scalarValue(key, value));
    }

    void putCompound(DataStore store, DataKey<?> key, Message value) {
        Object converted = convertCompound(value, key);
        if (Optional.class.isAssignableFrom(key.getValueClass())) {
            converted = Optional.of(converted);
        }
        set(key, store, converted);
    }

    void putRepeated(DataStore store, DataKey<?> key, List<?> values, boolean stringBytes) {
        List<Object> converted = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof Message message) {
                converted.add(convertCompound(message, key));
            } else if (stringBytes) {
                // Protobuf uint32 is represented as Integer, while Clava
                // retains string literal bytes as signed Byte.
                converted.add(((Number) value).byteValue());
            } else {
                converted.add(scalarValue(key, value));
            }
        }
        set(key, store, converted);
    }

    void putRepeatedEnums(DataStore store, DataKey<?> key, List<?> values, Class<?> enumClass) {
        List<Object> converted = new ArrayList<>(values.size());
        for (Object value : values) {
            converted.add(enumValue(enumClass, value));
        }
        set(key, store, converted);
    }

    void putRepeatedReferences(DataStore store, DataKey<?> key, List<Long> values) {
        List<String> ids = new ArrayList<>(values.size());
        for (long value : values) {
            ids.add(id.apply(value));
        }
        data.getClavaNodes().queueSetNodeList(store, rawKey(key), ids);
    }

    void putReference(DataStore store, DataKey<?> key, long value, boolean nullable, boolean record) {
        String reference = id.apply(value);
        // The text parser retained this legacy scalar alongside the resolved
        // RECORD pointer. Keep both values in sync for CXXMethodDecl.
        if (record) {
            set(CXXMethodDecl.RECORD_ID, store, reference);
        }
        queueReference(store, key, reference, nullable);
    }

    void applyAlignment(DataStore store, Boolean alignedExpression, long alignment) {
        if (alignedExpression == null) {
            throw new IllegalArgumentException("AlignedAttr.alignment requires is_expression");
        }
        if (!(store instanceof org.suikasoft.jOptions.DataStore.DataClass<?>)) {
            throw new IllegalArgumentException("AlignedAttr payload has no data class");
        }
        org.suikasoft.jOptions.DataStore.DataClass<?> dataClass =
                (org.suikasoft.jOptions.DataStore.DataClass<?>) store;
        if (alignedExpression) {
            set(AlignedAttr.ALIGNED_ATTR_KIND, store, AlignedAttrKind.EXPR);
            queueOptional(dataClass, alignment, AlignedExprAttr.EXPR);
        } else {
            set(AlignedAttr.ALIGNED_ATTR_KIND, store, AlignedAttrKind.TYPE);
            queue(dataClass, "alignment", alignment, AlignedTypeAttr.TYPE);
        }
    }

    private void queueReference(DataStore store, DataKey<?> key, String value, boolean nullable) {
        if (Optional.class.isAssignableFrom(key.getValueClass())) {
            data.getClavaNodes().queueSetOptionalNode(store, rawKey(key), value);
        } else if (ClavaNode.class.isAssignableFrom(key.getValueClass())) {
            if (ClavaNodes.isNullId(value) && nullable) {
                data.getClavaNodes().queueSetNullableNode(store, rawKey(key), value);
            } else {
                data.getClavaNodes().queueSetNode(store, rawKey(key), value);
            }
        } else {
            throw new IllegalArgumentException("Reference field '" + key.getName() + "' has unsupported type "
                    + key.getValueClass().getName());
        }
    }

    private Object scalarValue(DataKey<?> key, Object value) {
        Class<?> target = key.getValueClass();
        if (Optional.class.isAssignableFrom(target)) {
            return Optional.ofNullable(value);
        }
        if (target.isEnum()) {
            return enumValue(target, value);
        }
        if (target == BigInteger.class) {
            return new BigInteger(value.toString());
        }
        if (target == Integer.class || target == int.class) {
            return Math.toIntExact(((Number) value).longValue());
        }
        if (target == Long.class || target == long.class) {
            return ((Number) value).longValue();
        }
        if (target == Double.class || target == double.class) {
            return ((Number) value).doubleValue();
        }
        if (target == Float.class || target == float.class) {
            return ((Number) value).floatValue();
        }
        if (target == Boolean.class || target == boolean.class) {
            return value;
        }
        if (target == String.class) {
            return value.toString();
        }
        return value;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void set(DataKey<?> key, DataStore store, Object value) {
        store.set((DataKey) key, value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static DataKey rawKey(DataKey<?> key) {
        return key;
    }

    private Object convertCompound(Message value, DataKey<?> key) {
        if (value instanceof pt.up.fe.specs.clang.wire.TemplateArgument argument) {
            return templateArgument(argument);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.TemplateName name) {
            return templateName(name);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.ExceptionSpecification specification) {
            return exceptionSpecification(specification);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.CXXBaseSpecifier base) {
            return baseSpecifier(base);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.ExplicitSpecifier explicit) {
            return explicitSpecifier(explicit);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.CXXCtorInitializer initializer) {
            return ctorInitializer(initializer);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.OffsetOfComponent component) {
            return offsetComponent(component);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.Designator designator) {
            return designator(designator);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.NestedNameSpecifier nested) {
            return nestedName(nested);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.AsmInput input) {
            return asmInput(input);
        }
        if (value instanceof pt.up.fe.specs.clang.wire.AsmOutput output) {
            return asmOutput(output);
        }
        throw new IllegalArgumentException("Unsupported protobuf compound " + value.getClass().getSimpleName()
                + " for key " + key.getName());
    }

    private TemplateArgument templateArgument(pt.up.fe.specs.clang.wire.TemplateArgument value) {
        return switch (value.getTemplateCase()) {
            case TEMPLATE_DECLARATION -> {
                var result = new TemplateArgumentDeclaration();
                queue(result, "decl", value.getTemplateDeclaration().getDecl(), TemplateArgumentDeclaration.DECL);
                yield result;
            }
            case TEMPLATE_NULL_PTR -> {
                var result = new TemplateArgumentNullPtr();
                queue(result, "type", value.getTemplateNullPtr().getType(), TemplateArgumentNullPtr.TYPE);
                yield result;
            }
            case TEMPLATE_TYPE -> {
                var result = new TemplateArgumentType();
                queue(result, "type", value.getTemplateType().getType(), TemplateArgumentType.TYPE);
                yield result;
            }
            case TEMPLATE_EXPRESSION -> {
                var result = new TemplateArgumentExpr();
                queue(result, "expr", value.getTemplateExpression().getExpr(), TemplateArgumentExpr.EXPR);
                yield result;
            }
            case TEMPLATE_PACK -> {
                List<TemplateArgument> arguments = new ArrayList<>();
                for (pt.up.fe.specs.clang.wire.TemplateArgument argument : value.getTemplatePack().getArgumentsList()) {
                    arguments.add(templateArgument(argument));
                }
                yield new TemplateArgumentPack(arguments);
            }
            case TEMPLATE_INTEGRAL -> {
                var result = new TemplateArgumentIntegral();
                result.set(TemplateArgumentIntegral.INTEGRAL,
                        Integer.parseInt(value.getTemplateIntegral().getIntegral()));
                yield result;
            }
            case TEMPLATE_NAME -> templateName(value.getTemplateName());
            case TEMPLATE_EXPANSION -> {
                var result = new TemplateArgumentTemplateExpansion();
                var expansion = value.getTemplateExpansion();
                result.set(TemplateArgumentTemplateExpansion.NUM_EXPANSIONS,
                        expansion.hasNumExpansions() ? Optional.of(Math.toIntExact(expansion.getNumExpansions()))
                                : Optional.empty());
                result.set(TemplateArgumentTemplateExpansion.TEMPLATE, templateName(expansion.getTemplateName()));
                yield result;
            }
            case TEMPLATE_STRUCTURAL_VALUE -> {
                var result = new TemplateArgumentStructuralValue();
                queue(result, "type", value.getTemplateStructuralValue().getType(), TemplateArgumentStructuralValue.TYPE);
                yield result;
            }
            case TEMPLATE_NOT_SET -> throw new IllegalArgumentException("TemplateArgument alternative is required");
        };
    }

    private TemplateArgumentTemplate templateName(pt.up.fe.specs.clang.wire.TemplateName value) {
        return switch (value.getTemplatenameCase()) {
            case DIRECT_TEMPLATE_NAME -> {
                var result = new Template();
                result.set(TemplateArgumentTemplate.TEMPLATE_NAME_KIND, TemplateNameKind.Template);
                queueOptional(result, value.getDirectTemplateName().getTemplateDecl(), Template.TEMPLATE_DECL);
                yield result;
            }
            case QUALIFIED_TEMPLATE_NAME -> {
                var result = new QualifiedTemplate();
                result.set(TemplateArgumentTemplate.TEMPLATE_NAME_KIND, TemplateNameKind.QualifiedTemplate);
                result.set(QualifiedTemplate.QUALIFIER, value.getQualifiedTemplateName().getQualifier());
                result.set(QualifiedTemplate.HAS_TEMPLATE_KEYWORD, value.getQualifiedTemplateName().getHasTemplateKeyword());
                queue(result, "template_decl", value.getQualifiedTemplateName().getTemplateDecl(), QualifiedTemplate.TEMPLATE_DECL);
                yield result;
            }
            case SUBSTITUTED_TEMPLATE_NAME -> {
                var result = new SubstTemplateTemplateParm();
                result.set(TemplateArgumentTemplate.TEMPLATE_NAME_KIND, TemplateNameKind.SubstTemplateTemplateParm);
                queue(result, "parameter", value.getSubstitutedTemplateName().getParameter(), SubstTemplateTemplateParm.PARAMETER);
                result.set(SubstTemplateTemplateParm.REPLACEMENT,
                        templateName(value.getSubstitutedTemplateName().getReplacement()));
                yield result;
            }
            case USING_TEMPLATE_NAME -> {
                var result = new UsingTemplate();
                result.set(TemplateArgumentTemplate.TEMPLATE_NAME_KIND, TemplateNameKind.UsingTemplate);
                queue(result, "using_shadow_decl", value.getUsingTemplateName().getUsingShadowDecl(), UsingTemplate.USING_SHADOW_DECL);
                yield result;
            }
            case DEPENDENT_TEMPLATE_NAME -> {
                var result = new DependentTemplate();
                result.set(TemplateArgumentTemplate.TEMPLATE_NAME_KIND, TemplateNameKind.DependentTemplate);
                result.set(DependentTemplate.QUALIFIER, value.getDependentTemplateName().getQualifier());
                result.set(DependentTemplate.NAME, value.getDependentTemplateName().getName());
                yield result;
            }
            case TEMPLATENAME_NOT_SET -> throw new IllegalArgumentException("TemplateName alternative is required");
        };
    }

    private pt.up.fe.specs.clava.ast.type.data.ExceptionSpecification exceptionSpecification(
            pt.up.fe.specs.clang.wire.ExceptionSpecification value) {
        ExceptionSpecificationType kind = enumValue(ExceptionSpecificationType.class, value.getKind());
        var result = kind.newInstance();
        List<String> types = new ArrayList<>();
        for (long exceptionType : value.getExceptionTypesList()) {
            types.add(id.apply(exceptionType));
        }
        data.getClavaNodes().queueSetNodeList(result, ExceptionSpecification.EXCEPTION_TYPES, types);
        switch (value.getExceptionCase()) {
            case COMPUTED_EXCEPTION_DETAILS -> queue(result, "noexcept_expr",
                    value.getComputedExceptionDetails().getNoexceptExpr(), ComputedNoexcept.NOEXCEPT_EXPR);
            case UNEVALUATED_EXCEPTION_DETAILS -> queue(result, "source_decl",
                    value.getUnevaluatedExceptionDetails().getSourceDecl(), UnevaluatedExceptionSpecification.SOURCE_DECL);
            case UNINSTANTIATED_EXCEPTION_DETAILS -> {
                queue(result, "source_decl", value.getUninstantiatedExceptionDetails().getSourceDecl(),
                        UninstantiatedExceptionSpecification.SOURCE_DECL);
                queue(result, "source_template", value.getUninstantiatedExceptionDetails().getSourceTemplate(),
                        UninstantiatedExceptionSpecification.SOURCE_TEMPLATE);
            }
            case NO_EXCEPTION_DETAILS, EXCEPTION_NOT_SET -> {
                // No additional reference for the ordinary exception kinds.
            }
        }
        return result;
    }

    private CXXBaseSpecifier baseSpecifier(pt.up.fe.specs.clang.wire.CXXBaseSpecifier value) {
        var result = new CXXBaseSpecifier();
        result.set(CXXBaseSpecifier.IS_VIRTUAL, value.getIsVirtual());
        result.set(CXXBaseSpecifier.IS_PACK_EXPANSION, value.getIsPackExpansion());
        result.set(CXXBaseSpecifier.ACCESS_SPECIFIER_AS_WRITTEN,
                enumValue(pt.up.fe.specs.clava.language.AccessSpecifier.class, value.getAccessSpecifierAsWritten()));
        result.set(CXXBaseSpecifier.ACCESS_SPECIFIER_SEMANTIC,
                enumValue(pt.up.fe.specs.clava.language.AccessSpecifier.class, value.getAccessSpecifierSemantic()));
        queue(result, "type", value.getType(), CXXBaseSpecifier.TYPE);
        return result;
    }

    private ExplicitSpecifier explicitSpecifier(pt.up.fe.specs.clang.wire.ExplicitSpecifier value) {
        var result = new ExplicitSpecifier();
        result.set(ExplicitSpecifier.KIND, enumValue(ExplicitSpecKind.class, value.getKind()));
        queueOptional(result, value.getExpr(), ExplicitSpecifier.EXPR);
        result.set(ExplicitSpecifier.IS_SPECIFIED, value.getIsSpecified());
        return result;
    }

    private CXXCtorInitializer ctorInitializer(pt.up.fe.specs.clang.wire.CXXCtorInitializer value) {
        CXXCtorInitializer result;
        switch (value.getInitializerCase()) {
            case ANY_MEMBER_INITIALIZER -> {
                result = CXXCtorInitializer.newInstance(
                        pt.up.fe.specs.clava.language.CXXCtorInitializerKind.ANY_MEMBER_INITIALIZER);
                queue(result, "any_member_decl", value.getAnyMemberInitializer().getAnyMemberDecl(), AnyMemberInit.ANY_MEMBER_DECL);
            }
            case BASE_INITIALIZER -> {
                result = CXXCtorInitializer.newInstance(
                        pt.up.fe.specs.clava.language.CXXCtorInitializerKind.BASE_INITIALIZER);
                queue(result, "base_class", value.getBaseInitializer().getBaseClass(), BaseInit.BASE_CLASS);
            }
            case DELEGATING_INITIALIZER -> {
                result = CXXCtorInitializer.newInstance(
                        pt.up.fe.specs.clava.language.CXXCtorInitializerKind.DELEGATING_INITIALIZER);
                queue(result, "delegated_type", value.getDelegatingInitializer().getDelegatedType(), DelegatingInit.DELEGATED_TYPE);
            }
            case INITIALIZER_NOT_SET -> throw new IllegalArgumentException("CXXCtorInitializer alternative is required");
            default -> throw new IllegalArgumentException("Unknown CXXCtorInitializer alternative");
        }
        queue(result, "init_expr", value.getInitExpr(), CXXCtorInitializer.INIT_EXPR);
        result.set(CXXCtorInitializer.IS_IN_CLASS_MEMBER_INITIALIZER, value.getIsInClassMemberInitializer());
        result.set(CXXCtorInitializer.IS_WRITTEN, value.getIsWritten());
        return result;
    }

    private OffsetOfComponent offsetComponent(pt.up.fe.specs.clang.wire.OffsetOfComponent value) {
        return switch (value.getOffsetCase()) {
            case OFFSET_ARRAY -> {
                var result = OffsetOfComponent.newInstance(OffsetOfComponentKind.ARRAY);
                queue(result, "expr", value.getOffsetArray().getExpr(), OffsetOfArray.EXPR);
                yield result;
            }
            case OFFSET_FIELD -> {
                var result = OffsetOfComponent.newInstance(OffsetOfComponentKind.FIELD);
                result.set(OffsetOfField.FIELD_NAME, value.getOffsetField().getFieldName());
                yield result;
            }
            case OFFSET_IDENTIFIER -> {
                var result = OffsetOfComponent.newInstance(OffsetOfComponentKind.IDENTIFIER);
                result.set(OffsetOfIdentifier.FIELD_NAME, value.getOffsetIdentifier().getFieldName());
                yield result;
            }
            case OFFSET_BASE -> {
                var result = OffsetOfComponent.newInstance(OffsetOfComponentKind.BASE);
                queue(result, "type", value.getOffsetBase().getType(), OffsetOfBase.TYPE);
                yield result;
            }
            case OFFSET_NOT_SET -> throw new IllegalArgumentException("OffsetOfComponent alternative is required");
        };
    }

    private Designator designator(pt.up.fe.specs.clang.wire.Designator value) {
        return switch (value.getDesignatorCase()) {
            case FIELD_DESIGNATOR -> new FieldDesignator(value.getFieldDesignator().getFieldName());
            case ARRAY_DESIGNATOR -> new ArrayDesignator(value.getArrayDesignator().getIndex());
            case ARRAY_RANGE_DESIGNATOR -> new ArrayRangeDesignator(value.getArrayRangeDesignator().getIndex());
            case DESIGNATOR_NOT_SET -> throw new IllegalArgumentException("Designator alternative is required");
        };
    }

    private NestedNameSpecifier nestedName(pt.up.fe.specs.clang.wire.NestedNameSpecifier value) {
        return switch (value.getNestednameCase()) {
            case NAMESPACE_SPECIFIER -> {
                var result = NestedNameSpecifier.newInstance(NestedNameSpecifierKind.Namespace);
                queue(result, "namespace_decl", value.getNamespaceSpecifier().getNamespaceDecl(), NamespaceSpecifier.NAMESPACE);
                yield result;
            }
            case NAMESPACE_ALIAS_SPECIFIER -> {
                var result = NestedNameSpecifier.newInstance(NestedNameSpecifierKind.NamespaceAlias);
                queue(result, "namespace_alias", value.getNamespaceAliasSpecifier().getNamespaceAlias(), NamespaceAliasSpecifier.NAMESPACE_ALIAS);
                yield result;
            }
            case TYPE_SPECIFIER -> {
                var result = NestedNameSpecifier.newInstance(NestedNameSpecifierKind.TypeSpec);
                queue(result, "type", value.getTypeSpecifier().getType(), TypeSpecSpecifier.TYPE);
                yield result;
            }
            case TYPE_WITH_TEMPLATE_SPECIFIER -> {
                var result = NestedNameSpecifier.newInstance(NestedNameSpecifierKind.TypeSpecWithTemplate);
                queue(result, "type", value.getTypeWithTemplateSpecifier().getType(), TypeSpecWithTemplateSpecifier.TYPE);
                yield result;
            }
            case GLOBAL_SPECIFIER -> NestedNameSpecifier.newInstance(NestedNameSpecifierKind.Global);
            case SUPER_SPECIFIER -> {
                var result = NestedNameSpecifier.newInstance(NestedNameSpecifierKind.Super);
                queue(result, "super_decl", value.getSuperSpecifier().getSuperDecl(), SuperSpecifier.SUPER);
                yield result;
            }
            case NESTEDNAME_NOT_SET -> throw new IllegalArgumentException("NestedNameSpecifier alternative is required");
        };
    }

    private AsmInput asmInput(pt.up.fe.specs.clang.wire.AsmInput value) {
        var result = new AsmInput();
        queue(result, "expr", value.getExpr(), AsmInput.EXPR);
        result.set(AsmInput.CONSTRAINT, value.getConstraint());
        return result;
    }

    private AsmOutput asmOutput(pt.up.fe.specs.clang.wire.AsmOutput value) {
        var result = new AsmOutput();
        queue(result, "expr", value.getExpr(), AsmOutput.EXPR);
        result.set(AsmOutput.CONSTRAINT, value.getConstraint());
        result.set(AsmOutput.IS_PLUS_CONSTRAINT, value.getIsPlusConstraint());
        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void queue(org.suikasoft.jOptions.DataStore.DataClass<?> target, String field, long value,
            DataKey key) {
        data.getClavaNodes().queueSetNode(target, key, id.apply(value));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void queueOptional(org.suikasoft.jOptions.DataStore.DataClass<?> target, long value, DataKey key) {
        data.getClavaNodes().queueSetOptionalNode(target, key, id.apply(value));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T extends Enum<T>> T enumValue(Class<?> target, Object protoValue) {
        if (!(target.isEnum())) {
            throw new IllegalArgumentException("Expected enum target, got " + target);
        }
        String protoName = protoValue instanceof ProtocolMessageEnum enumValue
                ? enumValue.getValueDescriptor().getName()
                : protoValue.toString();
        Object best = null;
        int bestLength = -1;
        for (Object constant : target.getEnumConstants()) {
            String name = ((Enum<?>) constant).name();
            String normalizedProto = protoName.replace("_", "").toLowerCase(Locale.ROOT);
            String normalizedTarget = name.replace("_", "").toLowerCase(Locale.ROOT);
            if (protoName.equals(name) || protoName.endsWith("_" + name)
                    || normalizedProto.endsWith(normalizedTarget)) {
                if (normalizedTarget.length() > bestLength) {
                    best = constant;
                    bestLength = normalizedTarget.length();
                }
            }
        }
        if (best != null) {
            return (T) best;
        }
        throw new IllegalArgumentException("Unsupported " + target.getSimpleName() + " value '" + protoName + "'");
    }
}
