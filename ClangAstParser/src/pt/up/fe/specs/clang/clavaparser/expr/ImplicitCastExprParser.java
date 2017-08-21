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
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.ImplicitCastExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.extra.NullNode;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.util.stringparser.StringParser;

public class ImplicitCastExprParser extends AClangNodeParser<ImplicitCastExpr> {

    public ImplicitCastExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public ImplicitCastExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        // 'footype':'unsigned int' <IntegralCast>
        // 'class std::basic_ostream<char>' lvalue <UncheckedDerivedToBase (basic_ostream)>
        // 'const key_type':'const int' lvalue <NoOp>
        // 'basic_istream<char>':'class std::basic_istream<char>' lvalue <DerivedToBase (basic_iostream ->
        // basic_istream)>'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());
        CastKind castKind = parser.apply(ClangGenericParsers::parseCastKind);

        List<ClavaNode> children = getConverter().parse(node.getChildren());

        if (children.size() == 1) {
            Expr subExpr = toExpr(children.get(0));
            return ClavaNodeFactory.exprImplicitCast(castKind, exprData, info(node), subExpr);
        }

        Preconditions.checkArgument(children.size() == 2, "Expected 2 children at this point");

        Expr subExpr = toExpr(children.get(0));
        if (!(children.get(1) instanceof NullNode)) {
            throw new RuntimeException("Do not know what to do when non-NullNode:" + info(node));
        }

        return ClavaNodeFactory.exprImplicitCast(castKind, exprData, info(node), subExpr);
    }

}
