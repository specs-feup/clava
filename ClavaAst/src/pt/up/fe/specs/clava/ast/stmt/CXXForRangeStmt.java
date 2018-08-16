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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;

public class CXXForRangeStmt extends LoopStmt {

    public CXXForRangeStmt(ClavaNodeInfo info, DeclStmt range, Stmt beginEnd, Expr cond, Expr inc, DeclStmt loopVar,
            Stmt body) {

        this(info, Arrays.asList(range, ClavaNodes.getNodeOrNullStmt(beginEnd), cond, inc, loopVar, body));
    }

    private CXXForRangeStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXForRangeStmt(getInfo(), Collections.emptyList());
    }

    public DeclStmt getRange() {
        return getChild(DeclStmt.class, 0);
    }

    public Optional<DeclStmt> getBeginEnd() {
        // It can be a NullStmt
        return getChildTry(DeclStmt.class, 1);
    }

    public Expr getCond() {
        return getChild(Expr.class, 2);
    }

    public Expr getInc() {
        return getChild(Expr.class, 3);
    }

    public DeclStmt getLoopVar() {
        return getChild(DeclStmt.class, 4);
    }

    @Override
    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, 5);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("for(");

        VarDecl loopVar = (VarDecl) getLoopVar().getDecls().get(0);

        // String loopVarType = loopVar.getTypeCode().trim();
        String loopVarType = loopVar.getTypeCode().trim();
        if (loopVarType.endsWith(" &")) {
            loopVarType = loopVarType.substring(0, loopVarType.length() - 1) + "auto&";
        }

        code.append(loopVarType).append(" ").append(loopVar.getDeclName());

        code.append(" : ");
        VarDecl initVar = (VarDecl) getRange().getDecls().get(0);
        String expr = initVar.getInit().get().getCode();

        code.append(expr).append(")");
        // code.append(getBody().map(body -> body.getCode()).orElse(";"));
        code.append(getBody().getCode());

        return code.toString();
    }

    @Override
    public Optional<ClavaNode> getStmtCondition() {
        return Optional.of(getCond());
    }
}
