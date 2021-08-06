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
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.utils.StmtWithCondition;

public class IfStmt extends Stmt implements StmtWithCondition {

    public IfStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

    }

    public Optional<VarDecl> getDeclCondition() {
        return getOptionalChild(VarDecl.class, 0);
    }

    public Expr getCondition() {
        return getChild(Expr.class, 1);

    }

    public void setCondition(Expr condition) {
        setChild(1, condition);
    }

    /**
     * The 'then' block.
     * 
     * <p>
     * In C++ the IfStmt can have no then (e.g., "if(true);").
     * 
     * @return
     */
    public Optional<CompoundStmt> getThen() {
        return getOptionalChild(CompoundStmt.class, 2);
    }

    public void setThen(Stmt thenBody) {
        var thenNode = thenBody != null ? ClavaNodes.toCompoundStmt(thenBody) : getFactory().nullStmt();
        setChild(2, thenNode);
    }

    public Optional<CompoundStmt> getElse() {
        return getOptionalChild(CompoundStmt.class, 3);
    }

    public void setElse(Stmt elseBody) {
        var elseNode = elseBody != null ? ClavaNodes.toCompoundStmt(elseBody) : getFactory().nullStmt();
        setChild(3, elseNode);
    }

    @Override
    public String getCode() {

        StringBuilder code = new StringBuilder();

        String conditionCode = getDeclCondition().map(VarDecl::getCode).orElse(getCondition().getCode());

        String thenCode = getThen().map(this::getThenCode).orElse(";\n");

        // If then does not end with newline, add one
        thenCode = thenCode.endsWith(ln()) ? thenCode : thenCode + ln();

        code.append("if(").append(conditionCode).append(")").append(thenCode);

        // CompoundStmt takes care of prefixing a space before the statement code
        getElse().ifPresent(elseStmt -> code.append(getElseCode(elseStmt)));

        return code.toString();
    }

    private String getThenCode(CompoundStmt thenStmt) {
        boolean isNaked = thenStmt.isNaked();
        var code = thenStmt.getCode(isNaked);

        if (isNaked) {
            code = " " + code.stripLeading();
        }

        return code;
    }

    private String getElseCode(CompoundStmt elseStmt) {
        var elseCode = elseStmt.getCode();

        // If else stmt is a naked block with a single if, remove whitespace in the beginning?

        if (elseStmt.isNaked()) {
            elseCode = " " + elseCode.stripLeading();
        }

        return "else" + elseCode;
    }

    @Override
    public Optional<ClavaNode> getStmtCondition() {
        return getDeclCondition().map(ClavaNode.class::cast);
    }

}
