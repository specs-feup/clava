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

package pt.up.fe.specs.clang.clavaparser.expr;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.PredefinedExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.enums.PredefinedIdType;
import pt.up.fe.specs.util.stringparser.StringParser;

public class PredefinedExprParser extends AClangNodeParser<PredefinedExpr> {

    public PredefinedExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected PredefinedExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // 'const char [38]' lvalue __PRETTY_FUNCTION__

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        PredefinedIdType identType = parser.apply(ClangGenericParsers::parseEnum, PredefinedIdType.getEnumHelper());

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 1);

        Expr subExpr = toExpr(children.get(0));

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.predefinedExpr(identType, exprData, info(node), subExpr);
    }

}
