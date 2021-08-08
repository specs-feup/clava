/**
 * Copyright 2018 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang.codeparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;

public class TUnitProcessor {

    private final static boolean ERROR_ON_AMBIGUOUS_SIGNATURE = false;
    // private final static Set<Class<? extends ClavaNode>> IGNORE_CLASSES = new HashSet<>(
    // Arrays.asList(ImplicitCastExpr.class));

    private final List<ClangParserData> parsingData;
    private final boolean normalize;

    private final Map<String, ClavaNode> signatureToNodeMap;
    private final Set<String> ambiguousSignatures;
    private long processedNodes;
    private long processedFields;
    private long replacedNodes;
    private long ambiguousHits;
    private Set<Class<? extends ClavaNode>> ignoredNodesClasses;
    private StringBuilder collisionReport;

    public TUnitProcessor(List<ClangParserData> parsingData, boolean normalize) {
        this.parsingData = parsingData;
        this.normalize = normalize;

        this.signatureToNodeMap = new HashMap<>();
        this.ambiguousSignatures = new HashSet<>();
        this.processedNodes = 0;
        this.processedFields = 0;
        this.replacedNodes = 0;
        this.ambiguousHits = 0;
        this.ignoredNodesClasses = new HashSet<>();
        this.collisionReport = new StringBuilder();
    }

    /**
     * Ensures that fields of the AST point to nodes in other TranslationUnits, when that is the case.
     */
    public List<TranslationUnit> getTranslationUnits() {

        // Create list of Translation Units
        List<TranslationUnit> tUnits = parsingData.stream()
                .map(data -> data.get(ClangParserData.TRANSLATION_UNIT))
                .collect(Collectors.toList());

        // Applies several passes to make the tree resemble more the original code, e.g., remove implicit nodes from
        // original clang tree
        // new TreeTransformer(ClavaParser.getPostParsingRules()).transform(app);
        // TreeTransformer transformer = new TreeTransformer(ClangStreamParser.getPostParsingRules());
        // for (TranslationUnit tUnit : tUnits) {
        // transformer.transform(tUnit);
        // }

        if (!normalize) {
            return tUnits;
        }

        // Build node locations map
        // Map<String, ClavaNode> locationToNodeMap = new HashMap<>();
        // long allNodes = 0;
        // long nodesWithValidLoc = 0;
        for (TranslationUnit tUnit : tUnits) {
            tUnit.getDescendantsStream()
                    // Only consider nodes with complete locations
                    // .filter(node -> node.getLocation().isComplete())
                    // Only consider nodes with valid locations, nodes with invalid locations in the AST at this point
                    // where most likely introduced by Clava (e.g., Includes, Null nodes, etc)
                    .filter(node -> node.getLocation().isValid())
                    // Ignore certain nodes that might have the same location, such as ImplicitCastExpr
                    // .filter(node -> !IGNORE_CLASSES.contains(node.getClass()))
                    .forEach(node -> addNode(node));

            // allNodes += tUnit.getDescendantsStream().count();
            // nodesWithValidLoc += tUnit.getDescendantsStream()
            // .filter(node -> node.getLocation().isValid())
            // .count();
        }

        // System.out.println("ALL NODES: " + allNodes);
        // System.out.println("NODES WITH VALID LOCS: " + nodesWithValidLoc);

        // Iterate over all ClavaNodes and replace fields that have ClavaNodes

        // parsingData.stream()
        // .flatMap(data -> data.get(ClangParserData.CLAVA_NODES).getNodes().values().stream())
        // .forEach(node -> System.out.println("NODE: " + node.getClass()));

        parsingData.stream()
                .flatMap(data -> data.get(ClangParserData.CLAVA_NODES).getNodes().values().stream())
                .forEach(node -> replaceFields(node));

        ClavaLog.debug(() -> "During AST normalization processed " + processedFields + " fields in " + processedNodes
                + " nodes ");
        ClavaLog.debug(() -> "Removed " + replacedNodes + " nodes during normalization and found " + ambiguousHits
                + " queries to ambiguous signatures, with the following classes: "
                + ignoredNodesClasses.stream().map(Class::getSimpleName).sorted().collect(Collectors.toList()));

        if (ambiguousHits > 0) {
            ClavaLog.debug(() -> "Collision report:\n" + collisionReport);
        }

        return tUnits;
    }

    @SuppressWarnings("unchecked")
    private void replaceFields(ClavaNode node) {
        processedNodes++;

        for (DataKey<?> key : node.getKeysWithNodes()) {
            processedFields++;

            Class<?> valueClass = key.getValueClass();

            // Field is a ClavaNode
            if (ClavaNode.class.isAssignableFrom(valueClass)) {
                ClavaNode value = (ClavaNode) node.get(key);

                normalizeNode(value).ifPresent(normalizedNode -> {
                    node.set((DataKey<Object>) key, normalizedNode);
                    replacedNodes++;
                });
                // ClavaNode normalizedNode = locationToNodeMap.get(value.getNodeSignature());
                // if (normalizedNode == null) {
                // continue;
                // }
                //
                // node.set((DataKey<Object>) key, normalizedNode);
                // replacedNodes++;

                // System.out.println("NODE " + normalizedNode.getClass());

                continue;
            }

            // Field is a Optional<ClavaNode>
            if (Optional.class.isAssignableFrom(valueClass)) {
                ClavaNode value = ((Optional<ClavaNode>) node.get(key)).get();

                normalizeNode(value).ifPresent(normalizedNode -> {
                    node.set((DataKey<Object>) key, Optional.of(normalizedNode));
                    replacedNodes++;
                });

                // ClavaNode normalizedNode = locationToNodeMap.get(value.getNodeSignature());
                // if (normalizedNode == null) {
                // continue;
                // }
                //
                // node.set((DataKey<Object>) key, Optional.of(normalizedNode));
                // replacedNodes++;
                // System.out.println("OPTIONAL OF " + normalizedNode.getClass());
                continue;
            }

            // Field is a List<ClavaNode>
            if (List.class.isAssignableFrom(valueClass)) {
                List<ClavaNode> clavaNodes = (List<ClavaNode>) node.get(key);
                List<ClavaNode> newClavaNodes = new ArrayList<>(clavaNodes.size());

                for (int i = 0; i < clavaNodes.size(); i++) {
                    ClavaNode oldNode = clavaNodes.get(i);

                    ClavaNode normalizedNode = normalizeNode(oldNode).orElse(oldNode);
                    newClavaNodes.add(normalizedNode);
                    if (normalizedNode != oldNode) {
                        replacedNodes++;
                    }

                    // ClavaNode normalizedNode = locationToNodeMap.get(oldNode.getNodeSignature());
                    // if (normalizedNode == null) {
                    // newClavaNodes.add(oldNode);
                    // } else {
                    // newClavaNodes.add(normalizedNode);
                    // replacedNodes++;
                    // }
                }

                node.set((DataKey<Object>) key, newClavaNodes);
                continue;
            }

            ClavaLog.warning("Case not implemented: " + key + " in " + node.getClass());
        }

    }

    private Optional<ClavaNode> normalizeNode(ClavaNode node) {
        // If location of node is not valid, cannot be normalized
        if (!node.getLocation().isValid()) {
            return Optional.empty();
        }

        // If node is a macro, cannot be normalized
        if (node.get(ClavaNode.IS_MACRO)) {
            return Optional.empty();
        }

        // Do not normalize InitListExpr, to avoid conflicts with semantic/syntactic forms
        if (node instanceof InitListExpr) {
            return Optional.empty();
        }

        String signature = node.getNodeSignature();

        // If signature is ambiguous, cannot normalize node
        if (ambiguousSignatures.contains(signature)) {
            ClavaLog.debug(() -> "Signature collision: " + signature);
            ClavaLog.debug(() -> "Signature keys:" + node.getSignatureKeys());
            ClavaLog.debug(() -> "Node:" + node);
            // System.out.println("PREVIOUS NODE:" + previousNode);
            ambiguousHits++;
            ignoredNodesClasses.add(node.getClass());

            if (ERROR_ON_AMBIGUOUS_SIGNATURE) {
                ClavaLog.debug(() -> "Collision report:\n" + collisionReport);
                throw new RuntimeException("Found ambiguous signature that could not be solved during normalization");
            }

            return Optional.empty();
        }

        ClavaNode normalizedNode = signatureToNodeMap.get(signature);

        return Optional.ofNullable(normalizedNode);
    }

    private void addNode(ClavaNode node) {
        String id = node.getNodeSignature();

        // If signature is ambiguous, do not add to map
        if (ambiguousSignatures.contains(id)) {
            return;
        }

        ClavaNode previousNode = signatureToNodeMap.put(id, node);

        // If previous node is not null, signature is ambiguous, remove it from the map and add it to the set
        if (previousNode != null) {
            signatureToNodeMap.remove(id);
            ambiguousSignatures.add(id);
            collisionReport.append("Collision: ").append(id).append("\nOLD NODE: " + previousNode)
                    .append("\nNEW NODE: ")
                    .append(node).append("\n");
        }
        /*
        // If previous node is not null, node signature might not have been able to distinguish between nodes
        if (previousNode != null) {
            // System.out.println("SIGNATURE COLLISION: " + id);
            // System.out.println("SIGNATURE KEYS:" + node.getSignatureKeys());
            //
            // System.out.println("PREVIOUS NODE:" + previousNode);
            // System.out.println("CURRENT NODE:" + node);
        
            sameLocationPairs++;
            ignoredNodesClasses.add(node.getClass());
            // SpecsCheck.checkArgument(previousNode.equals(node),
            SpecsCheck.checkArgument(previousNode.getClass().equals(node.getClass()),
                    () -> "Found repeated location '" + id + "' where nodes have different classes.\nOriginal node: "
                            + previousNode
                            + "\nNew node:" + node);
        }
        */

        // SpecsCheck.checkArgument(previousNode == null,
        // () -> "Found repeated location '" + id + "'.\nOriginal node: " + previousNode
        // + "\nNew node:" + node);
    }

    // private String getLocationId(ClavaNode node) {
    // // return node.getClass().getSimpleName() + "_" + node.getCode();
    // return node.getClass().getSimpleName() + "_" + node.getLocation();
    // }

}
