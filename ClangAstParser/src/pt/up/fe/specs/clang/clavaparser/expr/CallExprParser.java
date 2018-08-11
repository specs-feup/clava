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
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CallExprParser extends AClangNodeParser<CallExpr> {

    public CallExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public CallExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        // 'double'
        // 'const std::string':'const class std::__cxx11::basic_string<char>'
        // 'typename std::remove_reference<int &>::type':'int' xvalue

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        List<ClavaNode> children = parseChildren(node);

        Preconditions.checkArgument(children.size() > 0, "Expected at least one argument, the name of the function");

        Expr function = toExpr(children.get(0));
        List<Expr> args = toExpr(SpecsCollections.subList(children, 1));

        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.callExpr(exprData, info(node), function, args);
    }

}
