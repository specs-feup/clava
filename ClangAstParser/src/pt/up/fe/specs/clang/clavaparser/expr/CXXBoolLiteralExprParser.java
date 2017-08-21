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
import pt.up.fe.specs.clava.ast.expr.CXXBoolLiteralExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class CXXBoolLiteralExprParser extends AClangNodeParser<CXXBoolLiteralExpr> {

    public CXXBoolLiteralExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXBoolLiteralExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        // '_Bool' false

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        // Boolean value
        boolean value = Boolean.parseBoolean(parser.apply(StringParsers::parseWord));

        return ClavaNodeFactory.cxxBoolLiteralExpr(value, exprData, node.getInfo());
    }

}
