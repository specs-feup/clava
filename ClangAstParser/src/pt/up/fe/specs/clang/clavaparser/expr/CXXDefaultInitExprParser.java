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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.CXXDefaultInitExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXDefaultInitExprParser extends AClangNodeParser<CXXDefaultInitExpr> {

    public CXXDefaultInitExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXDefaultInitExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        //
        // 'float'
        // 'int'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        checkNoChildren(node);

        return ClavaNodeFactory.cxxDefaultInitExpr(exprData, node.getInfo());
    }

}
