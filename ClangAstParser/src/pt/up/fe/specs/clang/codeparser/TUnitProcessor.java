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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;

import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.util.SpecsCheck;

public class TUnitProcessor {

    // private final static Set<Class<? extends ClavaNode>> IGNORE_CLASSES = new HashSet<>(
    // Arrays.asList(ImplicitCastExpr.class));

    private final List<ClangParserData> parsingData;
    private final boolean normalize;

    private long processedNodes;
    private long processedFields;
    private long replacedNodes;
    private long sameLocationPairs;

    public TUnitProcessor(List<ClangParserData> parsingData, boolean normalize) {
        this.parsingData = parsingData;
        this.normalize = normalize;
        this.processedNodes = 0;
        this.processedFields = 0;
        this.replacedNodes = 0;
        this.sameLocationPairs = 0;
    }

    /**
     * Ensures that fields of the AST point to nodes in other TranslationUnits, when that is the case.
     */
    public List<TranslationUnit> getTranslationUnits() {

        // Create list of Translation Units
        List<TranslationUnit> tUnits = parsingData.stream()
                .map(data -> data.get(ClangParserData.TRANSLATION_UNIT))
                .collect(Collectors.toList());

        if (!normalize) {
            return tUnits;
        }

        // Build node locations map
        Map<String, ClavaNode> locationToNodeMap = new HashMap<>();
        for (TranslationUnit tUnit : tUnits) {
            tUnit.getDescendantsAndSelfStream()
                    // Only consider nodes with valid locations
                    .filter(node -> node.getLocation().isValid())
                    // Ignore certain nodes that might have the same location, such as ImplicitCastExpr
                    // .filter(node -> !IGNORE_CLASSES.contains(node.getClass()))
                    .forEach(node -> addNode(node, locationToNodeMap));
        }

        // Iterate over all ClavaNodes and replace fields that have ClavaNodes

        // parsingData.stream()
        // .flatMap(data -> data.get(ClangParserData.CLAVA_NODES).getNodes().values().stream())
        // .forEach(node -> System.out.println("NODE: " + node.getClass()));

        parsingData.stream()
                .flatMap(data -> data.get(ClangParserData.CLAVA_NODES).getNodes().values().stream())
                .forEach(node -> replaceFields(node, locationToNodeMap));

        ClavaLog.debug(
                "During AST normalization processed " + processedFields + " fields in " + processedNodes + " nodes ");
        ClavaLog.debug("Removed " + replacedNodes + " nodes during normalization and found " + sameLocationPairs
                + " pairs with the same location");

        return tUnits;
    }

    @SuppressWarnings("unchecked")
    private void replaceFields(ClavaNode node, Map<String, ClavaNode> locationToNodeMap) {
        processedNodes++;

        for (DataKey<?> key : node.getKeysWithNodes()) {
            processedFields++;

            Class<?> valueClass = key.getValueClass();

            // Field is a ClavaNode
            if (ClavaNode.class.isAssignableFrom(valueClass)) {
                ClavaNode value = (ClavaNode) node.get(key);
                ClavaNode normalizedNode = locationToNodeMap.get(getLocationId(value));
                if (normalizedNode == null) {
                    continue;
                }

                node.set((DataKey<Object>) key, normalizedNode);
                replacedNodes++;

                // System.out.println("NODE " + normalizedNode.getClass());

                continue;
            }

            // Field is a Optional<ClavaNode>
            if (Optional.class.isAssignableFrom(valueClass)) {
                ClavaNode value = ((Optional<ClavaNode>) node.get(key)).get();
                ClavaNode normalizedNode = locationToNodeMap.get(getLocationId(value));
                if (normalizedNode == null) {
                    continue;
                }

                node.set((DataKey<Object>) key, Optional.of(normalizedNode));
                replacedNodes++;
                // System.out.println("OPTIONAL OF " + normalizedNode.getClass());
                continue;
            }

            // Field is a List<ClavaNode>
            if (List.class.isAssignableFrom(valueClass)) {
                List<ClavaNode> clavaNodes = (List<ClavaNode>) node.get(key);
                List<ClavaNode> newClavaNodes = new ArrayList<>(clavaNodes.size());

                for (int i = 0; i < clavaNodes.size(); i++) {
                    ClavaNode oldNode = clavaNodes.get(i);
                    ClavaNode normalizedNode = locationToNodeMap.get(getLocationId(oldNode));
                    if (normalizedNode == null) {
                        newClavaNodes.add(oldNode);
                    } else {
                        newClavaNodes.add(normalizedNode);
                        replacedNodes++;
                    }
                }

                node.set((DataKey<Object>) key, newClavaNodes);
                continue;
            }

            ClavaLog.warning("Case not implemented: " + key + " in " + node.getClass());
        }

    }

    private void addNode(ClavaNode node, Map<String, ClavaNode> locationToNodeMap) {
        String id = getLocationId(node);
        ClavaNode previousNode = locationToNodeMap.put(id, node);

        // If previous node is not null, even then they should be equivalent
        if (previousNode != null) {
            sameLocationPairs++;
            // SpecsCheck.checkArgument(previousNode.equals(node),
            SpecsCheck.checkArgument(previousNode.getClass().equals(node.getClass()),
                    () -> "Found repeated location '" + id + "' where nodes have different classes.\nOriginal node: "
                            + previousNode
                            + "\nNew node:" + node);
        }

        // SpecsCheck.checkArgument(previousNode == null,
        // () -> "Found repeated location '" + id + "'.\nOriginal node: " + previousNode
        // + "\nNew node:" + node);
    }

    private String getLocationId(ClavaNode node) {
        return node.getClass().getSimpleName() + "_" + node.getLocation();
    }

}
