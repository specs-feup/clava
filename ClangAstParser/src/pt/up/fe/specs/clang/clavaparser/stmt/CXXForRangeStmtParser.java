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
import pt.up.fe.specs.clava.ast.stmt.CXXForRangeStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXForRangeStmtParser extends AClangNodeParser<CXXForRangeStmt> {

    public CXXForRangeStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    protected CXXForRangeStmt parse(ClangNode node, StringParser parser) {

        List<ClavaNode> children = parseChildren(node);

        checkNumChildren(children, 6);
        // checkNumChildren(children, 7);

        /*
        DeclStmt *   Range,
        DeclStmt *      BeginEnd,
        Expr *  Cond,
        Expr *  Inc,
        DeclStmt *      LoopVar,
        Stmt *  Body,
        */

        DeclStmt range = (DeclStmt) toStmt(children.get(0));
        // Begin end is optional, it can either return a DeclStmt or a NullStmt
        Stmt beginEnd = toStmt(children.get(1));
        Expr cond = toExpr(children.get(2));
        Expr inc = toExpr(children.get(3));
        DeclStmt loopVar = (DeclStmt) toStmt(children.get(4));
        Stmt body = toStmt(children.get(5));

        return ClavaNodeFactory.cxxForRangeStmt(node.getInfo(), range, beginEnd, cond, inc, loopVar, body);
    }

}
