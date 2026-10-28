/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.util.*;
import org.suikasoft.jOptions.DataStore.DataClass;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.type.Type;

/** Immutable mutation baselines. Opaque transport is observed as data, never accepted as binding evidence. */
final class DeclarationState {
    private record FunctionState(Object symbol, Object selector, Object type, boolean body, Object storage, boolean specialization) {}
    private record ObjectState(Object symbol, Object type, boolean initializer, Object storage, Object tls) {}
    private record TagState(Object symbol, Object selector, boolean complete) {}
    private record Unsupported(Object data) {}
    private record NominalState(Object identity, Object name) {}
    private final DeclarationSchema schema = new DeclarationSchema(null,
            tag -> new NominalState(PostEditBindings.identity(tag), DeclarationContext.symbol(tag, null)));
    Object capture(NamedDecl declaration) {
        var profile = schema.describe(declaration);
        Object selector = profile.supported() ? profile.selector() : new Unsupported(new DataSnapshot().read(templateState(declaration, true)));
        Object result;
        if (declaration instanceof FunctionDecl function)
            result = new FunctionState(profile.symbol(), selector, type(function.getType()), function.hasBody(),
                    function.get(FunctionDecl.STORAGE_CLASS), function.get(FunctionDecl.IS_EXPLICIT_TEMPLATE_SPECIALIZATION));
        else if (declaration instanceof VarDecl variable)
            result = new ObjectState(profile.symbol(), type(variable.getType()), variable.hasInit(), variable.get(VarDecl.STORAGE_CLASS), variable.get(VarDecl.TLS_KIND));
        else if (declaration instanceof TagDecl tag) result = new TagState(profile.symbol(), selector, tag.isCompleteDefinition());
        else result = List.of(declaration.getDeclName(), declaration.get(NamedDecl.QUALIFIED_PREFIX), declaration.get(NamedDecl.LINKAGE));
        // Language mutation changes the compilation context even if native language linkage is historical.
        Object language = declaration.getAncestorTry(TranslationUnit.class)
                .<Object>map(unit -> unit.get(TranslationUnit.LANGUAGE).get(Language.C_PLUS_PLUS)).orElse("detached");
        return List.of(result, language, new DataSnapshot().read(templateState(declaration, !profile.supported())));
    }
    private Object type(Type type) {
        var evidence = schema.types().type(type);
        return evidence.known() ? evidence.shape() : new Unsupported(new DataSnapshot().read(type));
    }
    private Object templateState(NamedDecl declaration, boolean includeParameterTypes) {
        if (declaration instanceof FunctionDecl function) return Arrays.asList(function.get(FunctionDecl.TEMPLATE_PARAMETERS),
                function.get(FunctionDecl.TEMPLATE_ARGUMENTS), includeParameterTypes ? function.getParameters().stream().map(ValueDecl::getType).toList() : List.of(),
                function.getParent() instanceof TemplateDecl template ? template.getTemplateParameters() : List.of());
        if (declaration instanceof ClassTemplateSpecializationDecl specialization)
            return specialization.get(ClassTemplateSpecializationDecl.TEMPLATE_ARGUMENTS);
        return declaration.getClass().getName();
    }
    private record BackReference(int number) {}
    private record Data(String kind, Map<String, Object> fields, List<Object> children) {}
    private static final class DataSnapshot {
        private final IdentityHashMap<Object, Integer> visited = new IdentityHashMap<>();
        Object read(Object value) {
            if (value == null) return "<absent>";
            if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) return value;
            if (value instanceof Optional<?> optional) return optional.map(this::read).orElse("<absent>");
            if (visited.containsKey(value)) return new BackReference(visited.get(value));
            visited.put(value, visited.size());
            if (value instanceof List<?> list) return list.stream().map(this::read).toList();
            if (value instanceof NamedDecl declaration)
                return Arrays.asList(declaration.getClass().getName(), declaration.getDeclName(), declaration.get(NamedDecl.QUALIFIED_PREFIX),
                        declaration.hasValue(NamedDecl.USR) ? declaration.get(NamedDecl.USR) : new DeclarationContext.Reference(declaration));
            if (value instanceof DataClass<?> data) {
                var fields = new TreeMap<String, Object>();
                for (var key : data.getDataKeysWithValues()) {
                    String name = key.getName();
                    if (Set.of("context", "origin", "id", "previousId", "location", "usrSourceState", "editedEntityState").contains(name)) continue;
                    fields.put(name, read(data.get(key)));
                }
                var children = value instanceof ClavaNode node ? node.getChildren().stream().map(this::read).toList() : List.<Object>of();
                return new Data(value.getClass().getName(), Map.copyOf(fields), children);
            }
            return new DeclarationContext.Reference(value);
        }
    }
}
