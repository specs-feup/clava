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
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CXXConstructExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.CXXConstructExprData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXConstructExprParser extends AClangNodeParser<CXXConstructExpr> {

    public CXXConstructExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public CXXConstructExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        // 'const std::string':'const class std::__cxx11::basic_string<char>' 'void (class
        // std::__cxx11::basic_string<char> &&) noexcept' elidable
        // 'std::string':'class std::__cxx11::basic_string<char>' 'void (const char *, const class std::allocator<char>
        // &)'
        // 'class Molecule' 'void (void)'
        // 'pos_type':'class std::fpos<int>' 'void (streamoff)'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        CXXConstructExprData constructorData = parser.apply(ClangDataParsers::parseCXXConstructExpr);

        List<ClavaNode> children = parseChildren(node);

        List<Expr> args = toExpr(children);

        throw new RuntimeException("deprecated");
        // Using R_VALUE as default, for now
        // return ClavaNodeFactory.cxxConstructExpr(constructorData, exprData, info(node), args);
    }

}
