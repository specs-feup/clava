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

package pt.up.fe.specs.clang.clavaparser;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class DummyReplacerParser extends AClangNodeParser<ClavaNode> {

    private int numReplacedNodes;
    private int numIgnoredNodes;

    public DummyReplacerParser(ClangConverterTable converter) {
        super(converter, false, true);

        numReplacedNodes = 0;
        numIgnoredNodes = 0;

        // System.out.println("ALL CLANG NODES:" + getClangRootData().getAllClangNodes().keySet());
        // System.out.println("NUM:" + getClangRootData().getAllClangNodes().size());
    }

    public int getNumReplacedNodes() {
        return numReplacedNodes;
    }

    public int getNumIgnoredNodes() {
        return numIgnoredNodes;
    }

    @Override
    protected ClavaNode parse(ClangNode node, StringParser parser) {
        throw new RuntimeException("Should not be called");
    }

    public static void replaceDummyNodes(App app, ClangConverterTable converter) {

        int numTotalNodes = converter.getClangRootData().getNewParsedNodes().values().size();

        // Collect all nodes that are not dummy nodes
        List<ClavaNode> parsedNodes = converter.getClangRootData().getNewParsedNodes().values().stream()
                .filter(node -> !(node instanceof DummyNode))
                .collect(Collectors.toList());

        // System.out.println("CORRECTLY PARSED NODES:" + parsedNodes.size());
        double correctlyParsedRatio = (double) parsedNodes.size() / (double) numTotalNodes;
        System.out.println("Correctly parsed new nodes ratio: " + SpecsStrings.toPercentage(correctlyParsedRatio)
                + " (from a total of " + numTotalNodes + " new nodes)");

        DummyReplacerParser replacer = new DummyReplacerParser(converter);

        // Apply the dummy replacer over each non-dummy node
        parsedNodes.stream().forEach(node -> replacer.replaceDummies(node));

        int totalDummyNodes = numTotalNodes - parsedNodes.size();

        double replacedRatio = (double) replacer.getNumReplacedNodes() / (double) (totalDummyNodes);
        System.out.println("Replaced " + replacer.getNumReplacedNodes() + " dumy nodes ("
                + SpecsStrings.toPercentage(replacedRatio) + ")");

        double ignoredRatio = (double) replacer.getNumIgnoredNodes() / (double) (totalDummyNodes);
        System.out.println("Ignored " + replacer.getNumIgnoredNodes() + " dumy nodes ("
                + SpecsStrings.toPercentage(ignoredRatio) + ")");

        int untouchedNodes = totalDummyNodes - replacer.getNumIgnoredNodes() - replacer.getNumReplacedNodes();
        double untouchedRatio = (double) (untouchedNodes) / (double) (totalDummyNodes);
        System.out.println(
                "Untouched " + untouchedNodes + " dumy nodes (" + SpecsStrings.toPercentage(untouchedRatio) + ")");

        // Collect all dummy nodes
        // List<ClavaNode> dummyNodes = converter.getClangRootData().getNewParsedNodes().values().stream()
        // .filter(DummyNode.class::isInstance)
        // .collect(Collectors.toList());
        //
        // System.out.println("DUMMY NODES:" + dummyNodes.size());
    }

    private ClavaNode getOldClavaNode(ClavaNode newNode) {
        return getOldClavaNode(newNode, true);
    }

    private ClavaNode getOldClavaNode(ClavaNode newNode, boolean checkNull) {
        String id = newNode.get(ClavaNode.ID);
        ClangNode clangNode = getClangRootData().getAllClangNodes().get(id);

        if (checkNull) {
            SpecsCheck.checkNotNull(clangNode, () -> "Could not find old Clava node for node '" + newNode + "'");
        } else {
            if (clangNode == null) {
                return null;
            }
        }

        try {
            ClavaNode oldNode = parseChild(clangNode, newNode instanceof Type);
            return oldNode;
        } catch (Exception e) {
            return null;
        }

        // Put children in a new list, so that they can be detached
        // List<ClavaNode> newChildren = newNode.getChildrenStream()
        // .collect(Collectors.toList());

        // Replace children
        // newChildren.stream().forEach(ClavaNode::detach);
        // oldNode.setChildren(newChildren);

        // If new node has parent, replace it
        // if (newNode.hasParent()) {
        // NodeInsertUtils.replace(newNode, oldNode);
        // }

        // return oldNode;
    }

    private void replaceDummyNode(ClavaNode dummyNodeWithParent) {
        Preconditions.checkArgument(dummyNodeWithParent.hasParent());
        Preconditions.checkArgument(dummyNodeWithParent instanceof DummyNode);

        ClavaNode oldClavaNode = getOldClavaNode(dummyNodeWithParent, false);

        if (oldClavaNode == null) {
            numIgnoredNodes++;
            return;
        }
        oldClavaNode.removeChildren();
        NodeInsertUtils.set(dummyNodeWithParent, oldClavaNode);
        numReplacedNodes++;
    }

    // private ClavaNode getOldWithCorrectChildren(ClavaNode dummyNodeWithoutParent) {
    // Preconditions.checkArgument(!dummyNodeWithoutParent.hasParent());
    // Preconditions.checkArgument(dummyNodeWithoutParent instanceof DummyNode);
    //
    // ClavaNode oldClavaNode = getOldClavaNode(dummyNodeWithParent, false);
    //
    // if (oldClavaNode == null) {
    // numIgnoredNodes++;
    // return;
    // }
    // oldClavaNode.removeChildren();
    // NodeInsertUtils.set(dummyNodeWithParent, oldClavaNode);
    // numReplacedNodes++;
    // }

    private void replaceDummies(ClavaNode node) {

        // Children
        replaceDummyChildren(node);

        // Types
        // replaceType(node);
    }

    /**
     * In case node has a type, replace with old node type.
     * 
     * @param node
     */
    private void replaceType(ClavaNode node) {
        if (!(node instanceof Typable)) {
            return;
        }

        Typable typable = (Typable) node;
        try {

            Type type = typable.getType();

            if (!(type instanceof DummyNode)) {
                return;
            }

            Type oldParsingType = getTypesMap().get(node.getId());

            if (oldParsingType == null) {
                oldParsingType = (Type) getOldClavaNode(type, false);
            }

            if (oldParsingType != null) {
                oldParsingType.setChildren(type.getChildren());
                typable.setType(oldParsingType);

                numReplacedNodes++;
                return;
            }

            System.out.println("IGNORED!!!!");
            numIgnoredNodes++;
            // typable.setType(oldParsingType);
        } catch (Exception e) {
            System.out.println("NODE WITHOUT TYPE:" + typable);
        }
    }

    private void replaceDummyChildren(ClavaNode node) {
        // Get dummy children
        List<ClavaNode> dummyNodes = node.getChildren().stream()
                .filter(DummyNode.class::isInstance)
                .collect(Collectors.toList());

        dummyNodes.stream().forEach(this::replaceDummyNode);

    }
}
