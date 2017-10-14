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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.CXXBindTemporaryExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class CXXBindTemporaryExprParser extends AClangNodeParser<CXXBindTemporaryExpr> {

    public CXXBindTemporaryExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public CXXBindTemporaryExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        // 'std::string':'class std::__cxx11::basic_string<char>' (CXXTemporary 0x4992ff8)

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        String addressString = parser.apply(string -> StringParsers.parseNested(string, '(', ')'));
        String temporaryAddress = parseAddress(addressString);

        List<ClavaNode> children = parseChildren(node);

        Preconditions.checkArgument(children.size() == 1, "Expected a single child");

        Expr subExpr = toExpr(children.get(0));

        // Using R_VALUE as default, for now
        return ClavaNodeFactory.cXXBindTemporaryExpr(temporaryAddress, exprData, info(node),
                subExpr);
    }

    private static String parseAddress(String string) {
        Preconditions.checkArgument(string.startsWith("CXXTemporary "));

        return string.substring("CXXTemporary ".length(), string.length());
    }

}
