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
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.stringparser.StringParser;

public class NewClavaNodeParser<T extends ClavaNode> extends AClangNodeParser<T> {

    public static <T extends ClavaNode> Function<ClangConverterTable, ClangNodeParser<?>> newInstanceNoContent(
            Class<T> nodeClass) {

        return (converter) -> new NewClavaNodeParser<>(converter, false, nodeClass);
    }

    public static <T extends ClavaNode> Function<ClangConverterTable, ClangNodeParser<?>> newInstance(
            Class<T> nodeClass) {
        return (converter) -> new NewClavaNodeParser<>(converter, true, nodeClass);
    }

    private final Class<T> nodeClass;

    public NewClavaNodeParser(ClangConverterTable converter, boolean hasContent, Class<T> nodeClass) {
        super(converter, hasContent);

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

        return newClavaNode(nodeClass, clavaNode.getData(), children);
    }

}
