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

package pt.up.fe.specs.clang.clavaparser.extra;

import java.util.Collections;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.comment.FullComment;
import pt.up.fe.specs.util.stringparser.StringParser;

/**
 * @deprecated Check this class, maybe it should be something similar to FinalAttr?
 * 
 * @author JoaoBispo
 *
 */
@Deprecated
public class AlwaysInlineAttrParser extends AClangNodeParser<FullComment> {

    public AlwaysInlineAttrParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected FullComment parse(ClangNode node, StringParser parser) {
        // Ignoring node
        parser.apply(ClangGenericParsers::clear);

        checkNoChildren(node);

        // Returning FullComment, these nodes are removed from the tree
        return ClavaNodeFactory.fullComment(node.getInfo(), Collections.emptyList());
    }

}
