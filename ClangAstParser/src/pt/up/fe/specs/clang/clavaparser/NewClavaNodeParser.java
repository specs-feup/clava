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

import java.util.List;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.attr.AlignedExprAttr;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.extra.NullNode;
import pt.up.fe.specs.clava.ast.extra.Undefined;
import pt.up.fe.specs.clava.ast.type.DependentSizedArrayType;
import pt.up.fe.specs.clava.ast.type.Type;
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
        List<ClavaNode> children = parseChildren(node.getChildrenStream(), getClass().getSimpleName(), isType);

        children = pruneChildren(clavaNode, children);

        // getConfig().get(ClavaNode.CONTEXT)
        // .get(ClavaContext.FACTORY)
        // .newNode()
        return clavaNode.newInstance(true, nodeClass, children);
        // return newClavaNode(nodeClass, clavaNode.getDataI(), children);
        /*
        if (clavaNode.hasDataI()) {
            return newClavaNode(nodeClass, clavaNode.getDataI(), children);
        }
        
        return newClavaNode(nodeClass, clavaNode.getData(), children);
        */
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

        throw new RuntimeException("NullNode not being handled in class " + clavaNode.getClass());
    }

    @Override
    protected boolean isLegacyParser() {
        return false;
    }

}
