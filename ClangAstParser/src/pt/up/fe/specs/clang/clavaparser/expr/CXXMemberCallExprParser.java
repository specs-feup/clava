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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.CXXMemberCallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXMemberCallExprParser extends AClangNodeParser<CXXMemberCallExpr> {

    public CXXMemberCallExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public CXXMemberCallExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        // 'size_type':'unsigned long long'

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        List<ClavaNode> children = parseChildren(node);

        Preconditions.checkArgument(children.size() > 0, "Expected at least one argument, the name of the function");

        Expr functionExpr = toExpr(children.get(0));
        Preconditions.checkArgument(functionExpr instanceof MemberExpr, "Expected callee to be a MemberExpr");
        MemberExpr function = (MemberExpr) functionExpr;

        List<Expr> args = SpecsCollections.subList(children, 1).stream()
                .map(child -> toExpr(child))
                .collect(Collectors.toList());

        CXXMemberCallExpr expr = ClavaNodeFactory.cxxMemberCallExpr(exprData, info(node), function, args);

        return expr;
    }

}
