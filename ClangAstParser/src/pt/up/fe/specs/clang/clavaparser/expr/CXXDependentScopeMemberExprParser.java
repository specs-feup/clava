/**
 * Copyright 2017 SPeCS.
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
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clang.streamparser.data.CxxMemberExprInfo;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.CXXDependentScopeMemberExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXDependentScopeMemberExprParser extends AClangNodeParser<CXXDependentScopeMemberExpr> {

    public CXXDependentScopeMemberExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXDependentScopeMemberExpr parse(ClangNode node, StringParser parser) {

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        CxxMemberExprInfo memberExprInfo = getStdErr().get(StreamKeys.CXX_MEMBER_EXPR_INFO).get(node.getExtendedId());
        if (memberExprInfo == null) {
            System.out.println("ID:" + node.getExtendedId());
            System.out.println("MAP:" + getStdErr().get(StreamKeys.CXX_MEMBER_EXPR_INFO));
        }
        List<ClavaNode> children = getConverter().parse(node.getChildren());
        // Not sure if it is always one child
        checkNumChildren(children, 1);
        Expr memberExpr = toExpr(children.get(0));
        // Expr memberExpr = !children.isEmpty() ? toExpr(children.get(0))
        // : ClavaNodeFactory.dummyExpr(ClavaNodeFactory.nullExpr());

        return ClavaNodeFactory.cxxDependentScopeMemberExpr(memberExprInfo.isArrow(), memberExprInfo.getMemberName(),
                exprData, node.getInfo(), memberExpr);
    }

}
