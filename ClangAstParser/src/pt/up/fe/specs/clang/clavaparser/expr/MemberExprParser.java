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
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.stringparser.StringParser;

public class MemberExprParser extends AClangNodeParser<MemberExpr> {

    public MemberExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    public MemberExpr parse(ClangNode node, StringParser parser) {
        // Examples
        // '<bound member function type>' .size 0x43b49b0
        // 'const class Atom':'const class Atom' lvalue ->second 0x43d2078
        // '<bound member function type>' .operator bool 0x46ddbc8
        // '<bound member function type>' .operator void * 0x38a9968

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        boolean isArrow = parser.apply(ClangGenericParsers::checkArrow);

        // Member address
        parser.apply(ClangGenericParsers::reverseHex);

        String memberName = parser.apply(ClangGenericParsers::parseRemaining);

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 1);

        Expr base = toExpr(children.get(0));

        return ClavaNodeFactory.memberExpr(memberName, isArrow, exprData, info(node),
                base);
    }

}
