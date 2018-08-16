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
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.expr.CXXThisExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXThisExprParser extends AClangNodeParser<CXXThisExpr> {

    public CXXThisExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXThisExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'class Routing::CSVReader *' this
        // 'const class Routing::MCSimulation *' this

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        parser.apply(string -> ClangGenericParsers.checkWord(string, "this"));

        checkNoChildren(node);
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.cxxThisExpr(exprData, node.getInfo());
    }

}
