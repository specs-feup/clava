/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.codeparser;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.enums.Linkage;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * Immutable identity snapshot used only while normalizing one parser pass.
 *
 * <p>This describes an AST occurrence, not the semantic entity declared by that occurrence. Keep its fields aligned
 * with {@link ClavaNode#getSignatureKeys()} and {@link ClavaNode#getSignatureCustomStrings()}.</p>
 */
record NormalizationKey(Class<?> nodeClass, String translationUnitDomain, boolean hasDeclarationLinkage,
        Linkage declarationLinkage,
        List<FieldValue> fields, List<String> customStrings, String compactLocation) {

    static NormalizationKey snapshot(ClavaNode node) {
        List<FieldValue> fields = new ArrayList<>();
        for (DataKey<?> key : node.getSignatureKeys()) {
            fields.add(new FieldValue(key, snapshotValue(node.get(key))));
        }

        List<String> customStrings = new ArrayList<>();
        for (String customString : node.getSignatureCustomStrings()) {
            customStrings.add(customString);
        }

        var location = SourceRangeValue.snapshot(node.getLocation());
        boolean hasDeclarationLinkage = node instanceof NamedDecl declaration && declaration.hasValue(NamedDecl.LINKAGE);
        Linkage linkage = hasDeclarationLinkage ? ((NamedDecl) node).get(NamedDecl.LINKAGE) : null;
        boolean hasExternalLinkage = hasDeclarationLinkage && linkage == Linkage.External;
        String translationUnitDomain = !(node instanceof NamedDecl) || !hasExternalLinkage
                ? node.get(ClavaNode.ORIGINATING_TRANSLATION_UNIT).orElse(null) : null;

        return new NormalizationKey(node.getClass(), translationUnitDomain, hasDeclarationLinkage, linkage,
                List.copyOf(fields), Collections.unmodifiableList(customStrings), location.compact());
    }

    String compactDescription() {
        return nodeClass.getSimpleName() + "@" + compactLocation;
    }

    private static Object snapshotValue(Object value) {
        if (value == null) {
            return NullValue.INSTANCE;
        }

        if (value instanceof SourceRange range) {
            return SourceRangeValue.snapshot(range);
        }

        if (value instanceof ClavaNode node) {
            // getNodeSignature() uses StringProvider.getString() for nodes. Capture its identity components directly
            // so a later reference rewrite cannot change this field's value.
            return new NodeValue(node.getClass(), node.getId());
        }

        if (value instanceof Optional<?> optional) {
            return optional.isPresent()
                    ? new OptionalValue(true, snapshotValue(optional.get()))
                    : new OptionalValue(false, NullValue.INSTANCE);
        }

        if (value instanceof Collection<?> collection) {
            List<Object> elements = new ArrayList<>(collection.size());
            collection.forEach(element -> elements.add(snapshotValue(element)));
            return new CollectionValue(List.copyOf(elements));
        }

        if (value instanceof StringProvider provider) {
            return new ProviderValue(value.getClass(), provider.getString());
        }

        if (value instanceof String || value instanceof Boolean || value instanceof Character || value instanceof Byte
                || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof Float
                || value instanceof Double || value instanceof BigInteger || value instanceof BigDecimal
                || value instanceof Enum<?>) {
            return new ScalarValue(value.getClass(), value);
        }

        throw new IllegalArgumentException("Unsupported mutable signature value " + value.getClass().getName()
                + "; add an explicit immutable snapshot to NormalizationKey");
    }

    private record FieldValue(DataKey<?> key, Object value) {
    }

    private record NodeValue(Class<?> nodeClass, String nodeId) {
    }

    private record OptionalValue(boolean present, Object value) {
    }

    private record CollectionValue(List<Object> values) {
    }

    private record ProviderValue(Class<?> providerClass, String value) {
    }

    private record ScalarValue(Class<?> valueClass, Object value) {
    }

    private enum NullValue {
        INSTANCE
    }

    private record SourceRangeValue(SourcePoint start, SourcePoint end, String invalidDescription) {
        static SourceRangeValue snapshot(SourceRange range) {
            if (range == null) {
                return new SourceRangeValue(null, null, "<null-location>");
            }

            String invalidDescription = range.isValid() ? null : range.toString();
            return new SourceRangeValue(SourcePoint.snapshot(range.getStart()), SourcePoint.snapshot(range.getEnd()),
                    invalidDescription);
        }

        String compact() {
            if (start == null) {
                return "<null-location>";
            }

            if (invalidDescription != null) {
                return invalidDescription;
            }

            String file = start.filepath() == null ? "<no-file>" : new File(start.filepath()).getName();
            return file + ":" + start.line() + ":" + start.column();
        }
    }

    private record SourcePoint(String filepath, int line, int column, boolean macro) {
        static SourcePoint snapshot(SourceLocation location) {
            if (location == null) {
                return null;
            }

            return new SourcePoint(location.getFilepath(), location.getLine(), location.getColumn(), location.isMacro());
        }
    }
}
