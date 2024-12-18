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

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;

/**
 * This represents C++0x [stmt.ranged]'s ranged for statement, represented as 'for (range-declarator :
 * range-expression)' or 'for (init-statement range-declarator : range-expression)'.
 * 
 * @author JBispo
 *
 */
public class CXXForRangeStmt extends LoopStmt {

    public CXXForRangeStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public DeclStmt getRange() {
        return getChild(DeclStmt.class, 0);
    }

    public Optional<DeclStmt> getBegin() {
        // It can be a NullStmt
        return getChildTry(DeclStmt.class, 1);
    }

    public Optional<DeclStmt> getEnd() {
        // It can be a NullStmt
        return getChildTry(DeclStmt.class, 2);
    }

    public Expr getCond() {
        return getChild(Expr.class, 3);
    }

    public Expr getInc() {
        return getChild(Expr.class, 4);
    }

    public DeclStmt getLoopVar() {
        return getChild(DeclStmt.class, 5);
    }

    @Override
    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, 6);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("for(");

        VarDecl loopVar = (VarDecl) getLoopVar().getDecls().get(0);

        String loopVarType = loopVar.getTypeCode().trim();
        if (loopVarType.endsWith(" &")) {
            loopVarType = loopVarType.substring(0, loopVarType.length() - 1) + "auto&";
        }

        code.append(loopVarType).append(" ").append(loopVar.getDeclName());

        code.append(" : ");
        VarDecl initVar = (VarDecl) getRange().getDecls().get(0);
        String expr = initVar.getInit().get().getCode();

        code.append(expr).append(")");
        code.append(getBody().getCode());

        return code.toString();
    }

    @Override
    public Optional<ClavaNode> getStmtCondition() {
        return Optional.of(getCond());
    }
}
