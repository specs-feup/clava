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
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.CXXFunctionalCastExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXFunctionalCastExprParser extends AClangNodeParser<CXXFunctionalCastExpr> {

    public CXXFunctionalCastExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public CXXFunctionalCastExpr parse(ClangNode node, StringParser parser) {
        // Examples:
        // 'std::string':'class std::__cxx11::basic_string<char>' functional cast to std::string <ConstructorConversion>
        // 'float' functional cast to float <NoOp>
        // 'class KeyValuePair' functional cast to class KeyValuePair <ConstructorConversion>
        // System.out.println("PARSER:" + parser);

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        // Drop string
        boolean check = parser.apply(string -> ClangGenericParsers.checkStringStarts(string, "functional cast to "));
        Preconditions.checkArgument(check);

        // String typeAsWritten = parser.apply(StringParsers::parseWord);

        // CastKind castKind = parser.apply(ClangGenericParsers::parseCastKind);
        String castKindString = parser.apply(string -> ClangGenericParsers.reverseNested(string, '<', '>'));
        String typeAsWritten = parser.apply(ClangGenericParsers::parseRemaining);
        CastKind castKind = CastKind.getHelper().valueOf(castKindString);

        List<ClavaNode> children = parseChildren(node);

        Preconditions.checkArgument(children.size() < 2, "Expected a single child:" + info(node));

        Expr subExpr = toExpr(children.get(0));

        return ClavaNodeFactory.cxxFunctionalCastExpr(typeAsWritten, castKind, exprData, info(node), subExpr);
    }

}
