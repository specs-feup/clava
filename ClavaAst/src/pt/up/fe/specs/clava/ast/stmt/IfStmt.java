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
import pt.up.fe.specs.clava.utils.StmtWithCondition;

public class IfStmt extends Stmt implements StmtWithCondition {

    public IfStmt(ClavaNodeInfo info, ClavaNode condition, CompoundStmt thenStmt) {
        this(info, Arrays.asList(condition, thenStmt));
    }

    public IfStmt(ClavaNodeInfo info, ClavaNode condition, CompoundStmt thenStmt, CompoundStmt elseStmt) {
        this(info, Arrays.asList(condition, thenStmt, elseStmt));
    }

    private IfStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new IfStmt(getInfo(), Collections.emptyList());
    }

    public ClavaNode getCondition() {
        // return getChild(0);
        return ClavaNodes.getChild(this, 0);

    }

    public CompoundStmt getThen() {
        return (CompoundStmt) ClavaNodes.getChild(this, 1);
    }

    public Optional<CompoundStmt> getElse() {
        return ClavaNodes.getChildTry(this, 2).map(child -> (CompoundStmt) child);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        String conditionCode = getCondition().getCode();

        String thenCode = getThen().getCode();
        // If then does not end with newline, add one
        thenCode = thenCode.endsWith(ln()) ? thenCode : thenCode + ln();

        code.append("if(").append(conditionCode).append(")").append(thenCode);
        // CompoundStmt takes care of prefixing a space before the statement code
        getElse().ifPresent(elseStmt -> code.append("else" + elseStmt.getCode()));

        return code.toString();
    }

    /*
    private String getThenCode(CompoundStmt stmt) {
        if (stmt instanceof CompoundStmt) {
            return stmt.getCode();
        }
    
        // Not a compound statement, manually create braces
        return "{" + ln() + getTab() + stmt.getCode() + ln() + "}" + ln();
    }
    */

    @Override
    public Optional<ClavaNode> getStmtCondition() {
        return Optional.of(getCondition());
    }

    /*
    @Override
    public List<Stmt> toStatements() {
    
        List<Stmt> then = getThen().toStatements();
    
        if (!getElse().isPresent()) {
            return then;
        }
    
        return SpecsCollections.concat(then, getElse().get().toStatements());
    }
    */

}
