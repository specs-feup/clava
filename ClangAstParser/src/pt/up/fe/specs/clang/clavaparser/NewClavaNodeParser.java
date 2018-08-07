/**
 * Copyright 2017 SPeCS.
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.attr.AlignedExprAttr;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.extra.NullNode;
import pt.up.fe.specs.clava.ast.extra.Undefined;
import pt.up.fe.specs.clava.ast.type.DependentSizedArrayType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.VariableArrayType;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.stringparser.StringParser;

public class NewClavaNodeParser<T extends ClavaNode> extends AClangNodeParser<T> {

    // public static <T extends ClavaNode> Function<ClangConverterTable, ClangNodeParser<?>> newInstanceNoContent(
    // Class<T> nodeClass) {
    //
    // return (converter) -> new NewClavaNodeParser<>(converter, false, nodeClass);
    // }

    public static <T extends ClavaNode> Function<ClangConverterTable, ClangNodeParser<?>> newInstance(
            Class<T> nodeClass) {
        return (converter) -> new NewClavaNodeParser<>(converter, nodeClass);
    }

    private final Class<T> nodeClass;

    // public NewClavaNodeParser(ClangConverterTable converter, boolean hasContent, Class<T> nodeClass) {
    public NewClavaNodeParser(ClangConverterTable converter, Class<T> nodeClass) {
        super(converter, false, true);

        this.nodeClass = nodeClass;
    }

    @Override
    protected T parse(ClangNode node, StringParser parser) {
        // System.out.println("PARSING " + node.getExtendedId());
        // Discard parser contents
        parser.clear();

        // Get already parsed new ClavaNode
        // Using parsed ClavaNodes instead of simply ClavaData to avoid manually applying
        // post-processing and other complications
        ClavaNode clavaNode = getClangRootData().getNewParsedNodes().get(node.getId());
        Preconditions.checkNotNull(clavaNode, "Could not find new ClavaNode for id '" + node.getId() + "'");
        // System.out.println("NODE:" + node.getContent());
        // System.out.println("CLAVA DATA:" + clavaNode.getData().getClass());
        // System.out.println("CLAVA NODE:" + clavaNode);

        boolean isType = clavaNode instanceof Type;
        // Parse children
        // List<ClavaNode> children = parseChildren(node);

        // childrenId.stream().map(getClangRootData().getAllClangNodes()::get)
        // .collect(Collectors.toList());

        List<ClangNode> childrenClangNodes = getChildrenClangNodes(clavaNode, node);

        // List<ClavaNode> children = parseChildren(node.getChildrenStream(), getClass().getSimpleName(), isType);
        List<ClavaNode> children = parseChildren(childrenClangNodes.stream(), getClass().getSimpleName(), isType);

        /*
        if (clavaNode instanceof DeclRefExpr) {
            System.out.println(
                    "VISITED CHILDREN:" + getStdErr().get(ClangParserKeys.VISITED_CHILDREN).get(node.getExtendedId()));
        
            for (String childId : getStdErr().get(ClangParserKeys.VISITED_CHILDREN).get(node.getExtendedId())) {
                ClangNode childNode = getClangRootData().getAllClangNodes().get(childId);
        
                if (childId == null) {
                    throw new RuntimeException("No ClangNode for id " + childNode);
                }
            }
        
        }
        */
        // .map(nodeId -> getParsedNode(nodeId))

        // getStdErr().get(ClangParserKeys.VISITED_CHILDREN).get(node.getExtendedId()).stream()
        // .map(nodeId -> getParsedNode(nodeId))
        // List<ClavaNode> children = parseChildren(
        // getStdErr().get(ClangParserKeys.VISITED_CHILDREN).get(node.getExtendedId()).stream(),
        // getClass().getSimpleName(),
        // isType);

        // if (clavaNode instanceof DeclRefExpr) {
        // System.out.println("DeclRefExpr CHILDREN:" + children);
        // }

        children = pruneChildren(clavaNode, children);

        // getConfig().get(ClavaNode.CONTEXT)
        // .get(ClavaContext.FACTORY)
        // .newNode()
        /*
        if (!clavaNode.getClass().equals(nodeClass)) {
            System.out.println("CLAVA NODE CLASS:" + clavaNode.getClass());
            System.out.println("NEW CLAVA NODE CLASS:" + nodeClass);
        }
        
        clavaNode.setChildren(children);
        
        // Additional processing on the node itself
        processNodeCopy(clavaNode);
        
        return nodeClass.cast(clavaNode);
        */

        T clavaNodeCopy = clavaNode.newInstance(true, nodeClass, children);

        // Additional processing on the node itself
        processNodeCopy(clavaNodeCopy);

        return clavaNodeCopy;

        // return newClavaNode(nodeClass, clavaNode.getDataI(), children);
        /*
        if (clavaNode.hasDataI()) {
            return newClavaNode(nodeClass, clavaNode.getDataI(), children);
        }
        
        return newClavaNode(nodeClass, clavaNode.getData(), children);
        */
        /*
        // Check if children has any dummy type
        if (children.stream().filter(DummyNode.class::isInstance)
                .findFirst().isPresent()) {
            System.out.println("NODE " + clavaNode);
            System.out.println("HAS DUMMY TYPES:" + children);
        }
        */
    }

    private void processNodeCopy(ClavaNode clavaNodeCopy) {

        // In case node has a type, replace with old node type if the type or any of its descendants it a dummy node
        if (clavaNodeCopy instanceof Typable) {
            // if (clavaNodeCopy instanceof DeclRefExpr) {
            // System.out.println("DECL REF EXPR TYPE BEFORE:" + ((Typable) clavaNodeCopy).getType());
            // }

            Typable typable = (Typable) clavaNodeCopy;

            boolean replaceType = checkReplaceType(clavaNodeCopy);

            if (replaceType) {
                Type oldParsingType = getTypesMap().get(clavaNodeCopy.getId());

                typable.setType(oldParsingType);
                // System.out.println("FOUND DUMMY TYPE!");
                // System.out.println("COPY ID:" + clavaNodeCopy.getId());
                // System.out.println("TYPES MAP:" + getTypesMap());
                // System.out.println("ORIGINAL TYPE:" + getOriginalTypes().get(typable.getType().getId()));
                // System.out.println("TYPE:" + getTypesMap().get(clavaNodeCopy.getId()));
                // System.out.println("NODE CLASS:" + clavaNodeCopy.getClass());
            }

            // if (clavaNodeCopy instanceof DeclRefExpr) {
            // System.out.println("DECL REF EXPR TYPE AFTER:" + ((Typable) clavaNodeCopy).getType());
            // }

        }
        // TODO Auto-generated method stub

    }

    private boolean checkReplaceType(ClavaNode node) {
        Typable typable = (Typable) node;

        Type oldParsingType = getTypesMap().get(node.getId());

        // If no old parsing type, do not replace
        if (oldParsingType == null) {
            // SpecsLogs.msgWarn("Old parsing type is null, is this correct?"); // Maybe, if we are visiting nodes with
            // the
            // new nodes that are not visited by the
            // old nodes.

            return false;
        }

        // Verify if the type of the node is according to the expected type from old parsing
        Type nodeType = getOriginalTypes().get(typable.getType().getId());
        if (oldParsingType != null && nodeType != oldParsingType) {
            if (SpecsSystem.isDebug()) {
                SpecsLogs.msgWarn("Current node type different from expected type:\nExpected type:" + oldParsingType
                        + "\nCurrent type:" + nodeType);
            }

            return true;
        }

        boolean hasDummyNodes = typable.getType().getDescendantsAndSelfStream()
                .filter(DummyNode.class::isInstance)
                .findAny()
                .isPresent();

        if (hasDummyNodes) {
            return true;
        }

        return false;
    }

    private List<ClangNode> getChildrenClangNodes(ClavaNode clavaNode, ClangNode node) {
        // In certain cases, use new nodes children
        if (useNewNodesChildren(clavaNode)) {
            // Get ClangNodes of the children visited by the new node
            List<String> childrenId = getStdErr().get(ClangParserData.VISITED_CHILDREN).get(node.getExtendedId());

            List<ClangNode> childrenClangNodes = new ArrayList<>();
            for (String childId : childrenId) {
                ClangNode clangNode = getClangRootData().getAllClangNodes().get(childId);

                // If null, check if system header node
                if (clangNode == null) {
                    // System.out.println("SYSTEM HEADER IDS:"
                    // + getStdErr().get(ClangParserKeys.SYSTEM_HEADERS_CLANG_NODES).keySet());
                    // System.out.println("SYSTEM HEADER ID:" + childId);
                    clangNode = getStdErr().get(ClangParserData.SYSTEM_HEADERS_CLANG_NODES).get(childId);
                    // System.out.println("CLANG NODE:" + clangNode);
                }

                if (clangNode == null) {
                    System.out.println("Null clang node for child id " + childId);
                    continue;
                }

                childrenClangNodes.add(clangNode);

            }

            return childrenClangNodes;
        }

        // AST dumper children
        return node.getChildren();
    }

    private boolean useNewNodesChildren(ClavaNode clavaNode) {
        // For now, only use children from Clang AST dump
        // Decl children replace BareDecl
        // if (clavaNode instanceof DeclRefExpr) {
        // return true;
        // }

        if (clavaNode instanceof FunctionDecl) {
            // SpecsLogs.debug("FUNCTION DECL CHILDREN: " + clavaNode.getChildren());
            return true;
        }

        return false;
    }

    private List<ClavaNode> pruneChildren(ClavaNode clavaNode, List<ClavaNode> children) {
        // Check if it has a filler node
        if (clavaNode instanceof InitListExpr) {
            if (children.size() > 0
                    && (children.get(0) instanceof Undefined)) {

                String content = children.get(0).toContentString();
                Preconditions.checkArgument(content.equals("array - filler"),
                        "Content of node is not 'array - filler:'" + content);

                return children.subList(1, children.size());
            }
        }

        // Check if any of the children is a NullNode
        boolean hasNullNode = children.stream()
                .filter(child -> child instanceof NullNode)
                .findFirst()
                .isPresent();

        if (hasNullNode) {
            processNullNodes(clavaNode, children);
        }

        return children;
    }

    private void processNullNodes(ClavaNode clavaNode, List<ClavaNode> children) {
        if (clavaNode instanceof DependentSizedArrayType) {
            // Only second child can be null
            Preconditions
                    .checkArgument(!(children.get(0) instanceof NullNode) && (children.get(1) instanceof NullNode));

            // Replace with NullExpr
            children.set(1, LegacyToDataStore.getFactory().nullExpr());
            return;
        }

        if (clavaNode instanceof AlignedExprAttr) {
            Preconditions.checkArgument(children.size() == 1);
            children.set(0, LegacyToDataStore.getFactory().nullExpr());
            return;
        }

        if (clavaNode instanceof VariableArrayType) {
            Preconditions.checkArgument(children.size() > 1);
            if (children.get(1) instanceof NullNode) {
                children.set(1, LegacyToDataStore.getFactory().nullExpr());
            }
            return;
        }

        throw new RuntimeException("NullNode not being handled in class " + clavaNode.getClass());
    }

    @Override
    protected boolean isLegacyParser() {
        return false;
    }

}
