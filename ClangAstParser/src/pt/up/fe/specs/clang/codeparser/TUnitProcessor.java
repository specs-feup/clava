/**
 * Copyright 2018 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang.codeparser;

import org.suikasoft.jOptions.Datakey.DataKey;
import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.utils.CompilerEntityIdentity;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.util.SpecsIo;

import java.util.*;
import java.util.stream.Collectors;

public class TUnitProcessor {

    private static final boolean ERROR_ON_AMBIGUOUS_SIGNATURE = false;
    private static final int MAX_COLLISION_SAMPLES = 5;
    private static final int MAX_COLLISION_SAMPLE_LENGTH = 240;

    private final List<ClangAstData> parsingData;
    private final boolean normalize;

    private final Map<NormalizationKey, ClavaNode> keyToNodeMap;
    private final Set<NormalizationKey> ambiguousKeys;
    private final IdentityHashMap<ClavaNode, NormalizationKey> nodeKeys;
    private long processedNodes;
    private long processedFields;
    private long replacedNodes;
    private long ambiguousHits;
    private long collisionCount;
    private final Set<Class<? extends ClavaNode>> ignoredNodesClasses;
    private final List<String> collisionSamples;

    public TUnitProcessor(List<ClangAstData> parsingData, boolean normalize) {
        this.parsingData = parsingData;
        this.normalize = normalize;

        this.keyToNodeMap = new HashMap<>();
        this.ambiguousKeys = new HashSet<>();
        this.nodeKeys = new IdentityHashMap<>();
        this.ignoredNodesClasses = new HashSet<>();
        this.collisionSamples = new ArrayList<>();
    }

    /**
     * Ensures that fields of the AST point to nodes in other TranslationUnits, when that is the case.
     */
    public List<TranslationUnit> getTranslationUnits() {
        List<TranslationUnit> tUnits = parsingData.stream()
                .map(data -> data.get(ClangAstData.TRANSLATION_UNIT))
                .collect(Collectors.toList());

        assignTranslationUnitOrigins(tUnits);
        for (int i = 0; i < tUnits.size(); i++) {
            var included = new ArrayList<NamedDecl>();
            for (var node : parsingData.get(i).get(ClangAstData.CLAVA_NODES).getNodes().values()) {
                if (node instanceof NamedDecl declaration && declaration.hasValue(NamedDecl.USR)) {
                    if (declaration.get(NamedDecl.USR_GENERATION_FAILED) != declaration.get(NamedDecl.USR).isEmpty())
                        throw new IllegalArgumentException("Inconsistent USR generation status for node " + declaration.getId());
                    if (declaration.get(NamedDecl.USR_TYPE_GENERATION_FAILED) != declaration.get(NamedDecl.USR_TYPE).isEmpty())
                        throw new IllegalArgumentException("Inconsistent type USR generation status for node " + declaration.getId());
                    if (!Set.of("none", "declaration", "definition", "tentative")
                            .contains(declaration.get(NamedDecl.USR_DEFINITION_KIND)))
                        throw new IllegalArgumentException("Unknown compiler definition kind for node " + declaration.getId());
                    CompilerEntityIdentity.capture(declaration);
                    if (declaration.getAncestorTry(TranslationUnit.class).isEmpty()) included.add(declaration);
                }
            }
            tUnits.get(i).set(TranslationUnit.INCLUDED_DECLARATIONS, List.copyOf(included));
        }
        // Native references already resolve through the dump-local node registry. Rewriting them
        // by source coordinates would destroy TU ownership, particularly for macro-configured headers.
        // Cross-TU entity lookup belongs exclusively to DeclarationsCache.
        boolean compilerGraph = parsingData.stream()
                .flatMap(data -> data.get(ClangAstData.CLAVA_NODES).getNodes().values().stream())
                .anyMatch(node -> node instanceof NamedDecl declaration && declaration.hasValue(NamedDecl.USR));
        if (!normalize || compilerGraph) return tUnits;

        resetRunState();
        try {
            assignTranslationUnitOrigins(tUnits);
            indexTreeCandidates(tUnits);
            snapshotRegisteredNodesAndQueries();

            // Every key used in this phase has already been captured. Rewriting an earlier reference therefore
            // cannot change the identity observed by a later lookup.
            parsingData.stream()
                    .flatMap(data -> data.get(ClangAstData.CLAVA_NODES).getNodes().values().stream())
                    .forEach(this::replaceFields);

            ClavaLog.debug(() -> "During AST normalization processed " + processedFields + " fields in " + processedNodes
                    + " nodes");
            ClavaLog.debug(() -> "Removed " + replacedNodes + " nodes during normalization and found " + ambiguousHits
                    + " queries to ambiguous signatures, with the following classes: "
                    + ignoredNodesClasses.stream().map(Class::getSimpleName).sorted().collect(Collectors.toList()));

            if (ambiguousHits > 0) {
                ClavaLog.debug(this::formatCollisionReport);
            }

            return tUnits;
        } finally {
            // A TUnitProcessor can outlive this call. Do not retain ASTs through pass-local indexes.
            keyToNodeMap.clear();
            ambiguousKeys.clear();
            nodeKeys.clear();
        }
    }

    private void resetRunState() {
        keyToNodeMap.clear();
        ambiguousKeys.clear();
        nodeKeys.clear();
        ignoredNodesClasses.clear();
        collisionSamples.clear();
        processedNodes = 0;
        processedFields = 0;
        replacedNodes = 0;
        ambiguousHits = 0;
        collisionCount = 0;
    }

    private void indexTreeCandidates(List<TranslationUnit> tUnits) {
        for (TranslationUnit tUnit : tUnits) {
            tUnit.getDescendantsStream().forEach(node -> {
                // Only candidates with a valid source location can be shared.
                if (node.getLocation().isValid()) {
                    addNode(node, snapshotKey(node));
                }
            });
        }
    }

    private void assignTranslationUnitOrigins(List<TranslationUnit> tUnits) {
        for (int i = 0; i < tUnits.size(); i++) {
            TranslationUnit tUnit = tUnits.get(i);
            if (tUnit.getFile() == null) {
                continue;
            }

            String origin = SpecsIo.getCanonicalPath(tUnit.getFile());
            assignOriginIfMissing(tUnit, origin);
            tUnit.getDescendantsStream().forEach(node -> assignOriginIfMissing(node, origin));

            // Registry membership is the authoritative owner for detached parser nodes, including declarations and
            // references parsed from included headers.
            parsingData.get(i).get(ClangAstData.CLAVA_NODES).getNodes().values()
                    .forEach(node -> assignOriginIfMissing(node, origin));
        }
    }

    private void assignOriginIfMissing(ClavaNode node, String origin) {
        if (node.get(ClavaNode.ORIGINATING_TRANSLATION_UNIT).isEmpty()) {
            node.set(ClavaNode.ORIGINATING_TRANSLATION_UNIT, Optional.of(origin));
        }
    }

    private void snapshotRegisteredNodesAndQueries() {
        for (ClangAstData data : parsingData) {
            for (ClavaNode node : data.get(ClangAstData.CLAVA_NODES).getNodes().values()) {
                forEachNodeValue(node, this::snapshotKey);
            }
        }
    }

    private NormalizationKey snapshotKey(ClavaNode node) {
        return nodeKeys.computeIfAbsent(node, NormalizationKey::snapshot);
    }

    private void forEachNodeValue(ClavaNode node, java.util.function.Consumer<ClavaNode> consumer) {
        for (DataKey<?> key : node.getKeysWithNodes()) {
            Object value = node.get(key);

            if (value instanceof ClavaNode child) {
                consumer.accept(child);
                continue;
            }

            if (value instanceof Optional<?> optional) {
                optional.filter(ClavaNode.class::isInstance).map(ClavaNode.class::cast).ifPresent(consumer);
                continue;
            }

            if (value instanceof List<?> list) {
                list.stream().filter(ClavaNode.class::isInstance).map(ClavaNode.class::cast).forEach(consumer);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void replaceFields(ClavaNode node) {
        processedNodes++;

        for (DataKey<?> key : node.getKeysWithNodes()) {
            processedFields++;
            Object value = node.get(key);

            if (value instanceof ClavaNode oldNode) {
                ClavaNode normalizedNode = normalizeNode(oldNode);
                if (normalizedNode != null && normalizedNode != oldNode) {
                    node.set((DataKey<Object>) key, normalizedNode);
                    replacedNodes++;
                }
                continue;
            }

            if (value instanceof Optional<?> optional) {
                if (optional.isEmpty() || !(optional.get() instanceof ClavaNode oldNode)) {
                    continue;
                }

                ClavaNode normalizedNode = normalizeNode(oldNode);
                if (normalizedNode != null && normalizedNode != oldNode) {
                    node.set((DataKey<Object>) key, Optional.of(normalizedNode));
                    replacedNodes++;
                }
                continue;
            }

            if (value instanceof List<?> list) {
                List<Object> updatedNodes = null;
                for (int i = 0; i < list.size(); i++) {
                    Object element = list.get(i);
                    if (!(element instanceof ClavaNode oldNode)) {
                        continue;
                    }

                    ClavaNode normalizedNode = normalizeNode(oldNode);
                    if (normalizedNode == null || normalizedNode == oldNode) {
                        continue;
                    }

                    if (updatedNodes == null) {
                        updatedNodes = new ArrayList<>(list);
                    }

                    updatedNodes.set(i, normalizedNode);
                    replacedNodes++;
                }

                if (updatedNodes != null) {
                    node.set((DataKey<Object>) key, updatedNodes);
                }
                continue;
            }

            ClavaLog.warning("Case not implemented: " + key + " in " + node.getClass());
        }
    }

    private ClavaNode normalizeNode(ClavaNode node) {
        // A compiler-selected declaration target is an occurrence in its original TU. The entity index
        // connects it to other declarations without redirecting it to a different TU's mutable node.
        if (node instanceof NamedDecl declaration && declaration.hasValue(NamedDecl.USR)) return null;
        if (!isNormalizable(node)) {
            return null;
        }

        NormalizationKey key = nodeKeys.get(node);
        if (key == null) {
            throw new IllegalStateException("Missing pre-rewrite normalization snapshot for "
                    + node.getClass().getName() + " with id " + node.getId());
        }

        if (ambiguousKeys.contains(key)) {
            ambiguousHits++;
            ignoredNodesClasses.add(node.getClass());

            if (ERROR_ON_AMBIGUOUS_SIGNATURE) {
                ClavaLog.debug(this::formatCollisionReport);
                throw new RuntimeException("Found ambiguous signature that could not be solved during normalization");
            }

            return null;
        }

        return keyToNodeMap.get(key);
    }

    private boolean isNormalizable(ClavaNode node) {
        return node.getLocation().isValid() && !node.get(ClavaNode.IS_MACRO) && !(node instanceof InitListExpr);
    }

    private void addNode(ClavaNode node, NormalizationKey key) {
        if (ambiguousKeys.contains(key)) {
            return;
        }

        ClavaNode previousNode = keyToNodeMap.putIfAbsent(key, node);
        if (previousNode == null) {
            return;
        }

        keyToNodeMap.remove(key);
        ambiguousKeys.add(key);
        collisionCount++;

        if (collisionSamples.size() < MAX_COLLISION_SAMPLES) {
            String sample = key.compactDescription();
            if (sample.length() > MAX_COLLISION_SAMPLE_LENGTH) {
                sample = sample.substring(0, MAX_COLLISION_SAMPLE_LENGTH - 3) + "...";
            }
            collisionSamples.add(sample);
        }
    }

    private String formatCollisionReport() {
        StringBuilder report = new StringBuilder("Ambiguous normalization keys: ").append(collisionCount);
        for (String sample : collisionSamples) {
            report.append("\n  ").append(sample);
        }
        if (collisionCount > collisionSamples.size()) {
            report.append("\n  ... ").append(collisionCount - collisionSamples.size()).append(" more");
        }
        return report.toString();
    }
}
