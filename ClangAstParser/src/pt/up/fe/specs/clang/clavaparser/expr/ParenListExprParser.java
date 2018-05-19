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

package pt.up.fe.specs.clang.clavaparser.expr;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.ParenListExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class ParenListExprParser extends AClangNodeParser<ParenListExpr> {

    public ParenListExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected ParenListExpr parse(ClangNode node, StringParser parser) {
        // Example:
        //
        // 'NULL TYPE'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        List<ClavaNode> children = parseChildren(node);
        checkAtLeast(children, 1); // Can be empty?

        List<Expr> expressions = toExpr(children);
        // System.out.println("EXPR:" + expressions);
        return ClavaNodeFactory.parenListExpr(exprData, node.getInfo(), expressions);
    }

}
