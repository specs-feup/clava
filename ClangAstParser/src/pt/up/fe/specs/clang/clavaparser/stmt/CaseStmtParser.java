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

package pt.up.fe.specs.clang.clavaparser.stmt;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.NullNode;
import pt.up.fe.specs.clava.ast.stmt.CaseStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CaseStmtParser extends AClangNodeParser<CaseStmt> {

    public CaseStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    public CaseStmt parse(ClangNode node, StringParser parser) {
        List<ClavaNode> children = parseChildren(node);

        // Always three children
        checkNumChildren(children, 3);

        // 1st child is LHS
        Expr lhs = toExpr(children.get(0));

        // 2nd child is optional and is the RHS
        Expr rhs = children.get(1) instanceof NullNode ? null : toExpr(children.get(1));

        // 3rd child is the body
        Stmt subStmt = toStmt(children.get(2));

        if (rhs == null) {
            return ClavaNodeFactory.caseStmt(info(node), lhs, subStmt);
        }

        return ClavaNodeFactory.caseStmt(info(node), lhs, rhs, subStmt);
    }

}
