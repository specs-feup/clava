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

package pt.up.fe.specs.clang.clavaparser.type;

import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.CppParsing;
import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clava.parser.DelayedParsingExpr;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.DecltypeType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class DecltypeTypeParser extends AClangNodeParser<DecltypeType> {

    public DecltypeTypeParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected DecltypeType parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // 'decltype(_S_pointer_helper((class std::allocator<int> *)0))' sugar

        TypeData typeData = parser.apply(ClangDataParsers::parseType);

        // First type is an expr, delay parsing
        ClangNode exprNode = node.getChild(0);
        Preconditions.checkArgument(CppParsing.isExprNodeName(exprNode.getName()),
                "Expected node to be an Expr, it is '" + exprNode.getName() + "'");

        List<ClavaNode> children = parseChildren(SpecsCollections.subList(node.getChildren(), 1).stream());
        checkChildrenBetween(children, 0, 1);
        // checkNumChildren(children, 1);
        Expr expr = new DelayedParsingExpr(exprNode);

        Type underlyingType = children.isEmpty() ? ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo())
                : toType(children.get(0));

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.decltypeType(typeData, node.getInfo(), expr, underlyingType);
    }

}
