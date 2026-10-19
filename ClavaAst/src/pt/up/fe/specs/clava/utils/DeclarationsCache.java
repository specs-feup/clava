/**
 * Copyright 2022 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.utils.DeclarationIdentity.FunctionKey;
import pt.up.fe.specs.clava.utils.DeclarationIdentity.TagKey;
import pt.up.fe.specs.clava.utils.DeclarationIdentity.VariableKey;

/** One immutable index generation. App.clearCache() invalidates both declaration and type identities after edits. */
public class DeclarationsCache {
    private final DeclarationIdentity identity;
    private final Map<FunctionKey, List<FunctionDecl>> functionPrototypes = new HashMap<>();
    private final Definitions<FunctionKey, FunctionDecl> functionImplementations = new Definitions<>();
    private final Map<TagKey, List<TagDecl>> tagPrototypes = new HashMap<>();
    private final Definitions<TagKey, TagDecl> tagImplementations = new Definitions<>();
    private final Definitions<VariableKey, VarDecl> globalVarDefinitions = new Definitions<>();

    public DeclarationsCache(App app) {
        identity = new DeclarationIdentity(app);
        app.getDescendantsStream().forEach(this::processNode);
    }

    public List<FunctionDecl> getFunctionPrototypes(FunctionDecl function) {
        return List.copyOf(functionPrototypes.getOrDefault(identity.function(function), List.of()));
    }

    public Optional<FunctionDecl> getFunctionImplementation(FunctionDecl function) {
        var source = (FunctionDecl) ClavaNodes.normalizeDecl(function);
        return functionImplementations.get(identity.function(source))
                .filter(definition -> identity.compatibleFunctions(source, definition));
    }

    public DefinitionResolution<FunctionDecl> getFunctionResolution(FunctionDecl function) {
        var source = (FunctionDecl) ClavaNodes.normalizeDecl(function);
        var result = functionImplementations.resolution(identity.function(source));
        if (result.kind() == DefinitionResolution.Kind.UNIQUE
                && !identity.compatibleFunctions(source, result.candidates().get(0)))
            return new DefinitionResolution<>(DefinitionResolution.Kind.MISSING, List.of());
        return result;
    }

    public List<TagDecl> getTagPrototypes(TagDecl tag) {
        return List.copyOf(tagPrototypes.getOrDefault(identity.tag(tag), List.of()));
    }

    public Optional<TagDecl> getTagImplementation(TagDecl tag) {
        return tagImplementations.get(identity.tag(tag));
    }

    public Optional<VarDecl> getGlobalVarDefinition(VarDecl variable) {
        return globalVarDefinitions.get(identity.variable(variable))
                .filter(definition -> identity.compatibleVariableTypes(variable.getType(), definition.getType()));
    }

    /** Diagnostic compatibility only. Semantic lookups use structured keys. */
    public String getFunctionKey(FunctionDecl function) { return function.getSignature(); }
    /** Diagnostic compatibility only. Semantic lookups use structured keys. */
    public String getTagKey(TagDecl tag) { return identity.tag(tag).name().toString(); }

    private void processNode(ClavaNode node) {
        if (node instanceof FunctionDecl function) {
            var normalized = (FunctionDecl) ClavaNodes.normalizeDecl(function);
            var originalKey = identity.function(function);
            var normalizedKey = identity.function(normalized);

            // Keep the specialization's semantic identity, but also index its primary declaration so
            // calls normalized to the source template can find the generic definition. Repeatedly indexing
            // the primary node is harmless: Definitions and addPrototype deduplicate the same AST node.
            if (normalized.hasBody()) {
                functionImplementations.add(originalKey, normalized);
                if (!originalKey.equals(normalizedKey)) functionImplementations.add(normalizedKey, normalized);
            } else {
                addPrototype(functionPrototypes, originalKey, normalized);
                if (!originalKey.equals(normalizedKey)) addPrototype(functionPrototypes, normalizedKey, normalized);
            }
        } else if (node instanceof TagDecl tag) {
            var key = identity.tag(tag);
            if (tag.isCompleteDefinition()) tagImplementations.add(key, tag);
            else addPrototype(tagPrototypes, key, tag);
        } else if (node instanceof VarDecl variable && DeclarationIdentity.isGlobalDefinition(variable)) {
            globalVarDefinitions.add(identity.variable(variable), variable);
        }
    }

    private static <K, T> void addPrototype(Map<K, List<T>> index, K key, T node) {
        var values = index.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (values.stream().noneMatch(value -> value == node)) values.add(node);
    }

    /** Conflicting definition occurrences never acquire a winner based on traversal order. */
    private static final class Definitions<K, T extends ClavaNode> {
        private final Map<K, T> unique = new HashMap<>();
        private final Map<K, List<T>> ambiguous = new HashMap<>();

        void add(K key, T node) {
            if (ambiguous.containsKey(key)) return;
            var previous = unique.putIfAbsent(key, node);
            if (previous == null || previous == node) return;
            if (previous.getClass() == node.getClass() && previous.getLocation().isValid()
                    && previous.getLocation().equals(node.getLocation())) {
                // Repeated inclusion of one physical definition is not a second definition to edit.
                String first = DeclarationIdentity.origin(previous);
                String second = DeclarationIdentity.origin(node);
                if (first != null && second != null && second.compareTo(first) < 0) unique.put(key, node);
                return;
            }
            unique.remove(key);
            ambiguous.put(key, List.of(previous, node));
        }

        Optional<T> get(K key) { return Optional.ofNullable(unique.get(key)); }

        DefinitionResolution<T> resolution(K key) {
            var conflicts = ambiguous.get(key);
            if (conflicts != null) return new DefinitionResolution<>(DefinitionResolution.Kind.AMBIGUOUS, conflicts);
            var node = unique.get(key);
            return node == null ? new DefinitionResolution<>(DefinitionResolution.Kind.MISSING, List.of())
                    : new DefinitionResolution<>(DefinitionResolution.Kind.UNIQUE, List.of(node));
        }
    }
}
