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

package pt.up.fe.specs.clang.clavaparser.attr;

import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.AlignedAttr;
import pt.up.fe.specs.util.stringparser.StringParser;

public class AlignedAttrParser extends AClangNodeParser<AlignedAttr> {

    public AlignedAttrParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected AlignedAttr parse(ClangNode node, StringParser parser) {

        // Discard parser contents
        parser.clear();

        // Get already parsed new ClavaNode
        // Using parsed ClavaNodes instead of simply ClavaData to avoid manually applying
        // post-processing and other complications
        ClavaNode clavaNode = getClangRootData().getNewParsedNodes().get(node.getId());
        Preconditions.checkNotNull(clavaNode, "Could not find new ClavaNode for id '" + node.getId() + "'");

        // Parse children
        List<ClavaNode> children = parseChildren(node);

        return newClavaNode(AlignedAttr.class, clavaNode.getData(), children);
        // AttrData attrData = parser.apply(ClangDataParsers::parseAttr);
        // parser.apply(ClangGenericParsers::ensureStringStarts, "final");

        // checkNoChildren(node);

        // return ClavaNodeFactory.finalAttr(attrData, node.getInfo());
    }

}
