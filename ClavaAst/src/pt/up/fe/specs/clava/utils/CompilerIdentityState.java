/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.util.*;
import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * A semantic baseline with an allocation-free unchanged-state check. Node setters/tree operations have stamps;
 * exposed lists and mutable data classes are observed separately so their in-place edits are not missed.
 * A changed dependency still goes through the original semantic comparison, including equivalent edits.
 */
final class CompilerIdentityState {
    private final NamedDecl owner;
    private final Object baseline;
    private Observation[] observations;

    CompilerIdentityState(NamedDecl owner, Object baseline) {
        this(owner, baseline, new IdentityHashMap<>());
    }

    CompilerIdentityState(NamedDecl owner, Object baseline, Map<Type, Observation[]> sharedTypes) {
        this.owner = owner;
        this.baseline = baseline;
        observations = observe(owner, sharedTypes);
    }

    boolean isCurrent(NamedDecl declaration) {
        // A raw AST copy can carry the source snapshot. Never validate that copy using the source's stamps.
        if (owner != declaration) return baseline.equals(CompilerEntityIdentity.sourceState(declaration));
        for (var observation : observations) {
            if (!observation.unchanged()) {
                if (!baseline.equals(CompilerEntityIdentity.sourceState(declaration))) return false;
                observations = observe(declaration);
                break;
            }
        }
        return true;
    }

    interface Observation { boolean unchanged(); }

    private record NodeStamp(ClavaNode node, long version) implements Observation {
        @Override public boolean unchanged() { return node.getIdentityMutationVersion() == version; }
    }

    // TU child/diagnostic changes do not alter a declaration's identity. Watch only the inputs read by
    // origin/languageLinkage; unrelated parser bookkeeping must not invalidate every declaration in the TU.
    private record UnitStamp(TranslationUnit unit, java.io.File file, Object language) implements Observation {
        @Override public boolean unchanged() {
            return unit.get(TranslationUnit.SOURCE_FILE) == file && unit.get(TranslationUnit.LANGUAGE) == language;
        }
    }

    private record ListStamp(List<?> list, Object[] elements) implements Observation {
        @Override public boolean unchanged() {
            if (list.size() != elements.length) return false;
            for (int i = 0; i < elements.length; i++) if (list.get(i) != elements[i]) return false;
            return true;
        }
    }

    private record DataStamp(DataClass<?> data, DataKey<?>[] keys, Object[] values) implements Observation {
        @Override public boolean unchanged() {
            for (int i = 0; i < keys.length; i++)
                if (!data.hasValue(keys[i]) || data.get(keys[i]) != values[i]) return false;
            return true;
        }
    }

    private static Observation[] observe(NamedDecl declaration) {
        return observe(declaration, new IdentityHashMap<>());
    }

    private static Observation[] observe(NamedDecl declaration, Map<Type, Observation[]> sharedTypes) {
        var collector = new Collector(sharedTypes);
        collector.declaration(declaration);
        return collector.observations.toArray(Observation[]::new);
    }

    private record Group(Observation[] dependencies) implements Observation {
        @Override public boolean unchanged() {
            for (var dependency : dependencies) if (!dependency.unchanged()) return false;
            return true;
        }
    }

    private static final class Collector {
        final Map<Type, Observation[]> sharedTypes;
        Collector(Map<Type, Observation[]> sharedTypes) { this.sharedTypes = sharedTypes; }

        final List<Observation> observations = new ArrayList<>();
        final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<ClavaNode> stamped = Collections.newSetFromMap(new IdentityHashMap<>());

        void stamp(ClavaNode node) {
            if (stamped.add(node)) observations.add(new NodeStamp(node, node.getIdentityMutationVersion()));
        }

        void declaration(NamedDecl declaration) {
            if (!visited.add(declaration)) return;
            stamp(declaration);
            for (var parent = declaration.getParent(); parent != null; parent = parent.getParent()) {
                if (parent instanceof TranslationUnit unit) {
                    observations.add(new UnitStamp(unit, unit.get(TranslationUnit.SOURCE_FILE),
                            unit.get(TranslationUnit.LANGUAGE)));
                    var language = unit.get(TranslationUnit.LANGUAGE);
                    var key = pt.up.fe.specs.clava.ast.extra.data.Language.C_PLUS_PLUS;
                    observations.add(new DataStamp(language, new DataKey<?>[]{key}, new Object[]{language.get(key)}));
                    break;
                }
                stamp(parent);
                if (parent instanceof TemplateDecl template) value(template.getTemplateParameters());
            }
            if (declaration instanceof ValueDecl value) value(value.getType());
            if (declaration instanceof VarDecl variable) value(variable.getChildrenMutable());
            if (declaration instanceof FunctionDecl function) {
                // Includes child-list bypasses of the normal tree mutators, and function body presence.
                value(function.getChildrenMutable());
                value(function.get(FunctionDecl.TEMPLATE_ARGUMENTS));
                value(function.get(FunctionDecl.TEMPLATE_PARAMETERS));
            }
            if (declaration instanceof CXXMethodDecl method) method.getRecordDecl().ifPresent(this::declaration);
            if (declaration instanceof ClassTemplateSpecializationDecl specialization)
                value(specialization.get(ClassTemplateSpecializationDecl.TEMPLATE_ARGUMENTS));
            if (declaration instanceof TemplateDecl template) value(template.getTemplateParameters());
        }

        void type(Type type) {
            if (!visited.add(type)) return;
            stamp(type);
            value(type.getChildrenMutable());
            // Only raw data needed by the semantic snapshot has been materialized before observation.
            // Conservatively follow the type's other stored dependencies too, without walking statements.
            for (var key : type.getDataKeysWithValues()) {
                if (key == ClavaNode.CONTEXT || key == ClavaNode.ORIGIN) continue;
                value(type.get(key));
            }
        }

        void value(Object value) {
            if (value instanceof Type type) {
                if (sharedTypes == null) { type(type); return; }
                if (!visited.add(type)) return;
                var dependencies = sharedTypes.get(type);
                if (dependencies == null) {
                    // Build a complete graph independently of this declaration's visited set. Reuse it
                    // only during a read-only capture batch; live edits always collect fresh observations.
                    var typeCollector = new Collector(null);
                    typeCollector.type(type);
                    dependencies = typeCollector.observations.toArray(Observation[]::new);
                    sharedTypes.put(type, dependencies);
                }
                observations.add(new Group(dependencies));
                return;
            }
            if (value instanceof NamedDecl declaration) { declaration(declaration); return; }
            if (value instanceof Optional<?> optional) { optional.ifPresent(this::value); return; }
            if (value instanceof List<?> list) {
                if (!visited.add(list)) return;
                observations.add(new ListStamp(list, list.toArray()));
                list.forEach(this::value);
                return;
            }
            if (value instanceof DataClass<?> data) {
                if (!visited.add(data)) return;
                var keys = data.getDataKeysWithValues().toArray(DataKey<?>[]::new);
                var values = new Object[keys.length];
                for (int i = 0; i < keys.length; i++) {
                    values[i] = data.get(keys[i]);
                    value(values[i]);
                }
                observations.add(new DataStamp(data, keys, values));
            }
        }
    }
}
