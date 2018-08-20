/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.clavaparser.comment;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clava.ast.comment.FullComment;
import pt.up.fe.specs.util.stringparser.StringParser;

public class FullCommentParser extends AClangNodeParser<FullComment> {

    public FullCommentParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    protected FullComment parse(ClangNode node, StringParser parser) {
        // Comments are processed separately

        // List<ClavaNode> children = parseChildren(node);
        // checkAtLeast(children, 1);
        // checkChildrenBetween(children, 0, 1);
        // List<BlockContentComment> blocks = CollectionUtils.cast(BlockContentComment.class, children);

        // return ClavaNodeFactory.fullComment(info(node), blocks);
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.fullComment(info(node), Collections.emptyList());
    }

}
