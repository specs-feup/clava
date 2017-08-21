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
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.CXXTemporaryObjectExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.CXXConstructExprData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXTemporaryObjectExprParser extends AClangNodeParser<CXXTemporaryObjectExpr> {

    public CXXTemporaryObjectExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public CXXTemporaryObjectExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        // 'std::string':'class std::__cxx11::basic_string<char>' 'void (const char *, size_type, const class
        // std::allocator<char> &)'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        CXXConstructExprData constructorData = parser.apply(ClangDataParsers::parseCXXConstructExpr);

        List<ClavaNode> children = parseChildren(node);
        List<Expr> args = children.stream().map(child -> toExpr(child)).collect(Collectors.toList());

        // Using R_VALUE as default, for now
        return ClavaNodeFactory.cxxTemporaryObjectExpr(constructorData, exprData, info(node), args);

    }

}
