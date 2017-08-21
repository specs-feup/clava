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

public class WhileStmt extends LoopStmt {

    public WhileStmt(ClavaNodeInfo info, ClavaNode condition, CompoundStmt thenStmt) {
        this(info, Arrays.asList(condition, thenStmt));
    }

    private WhileStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new WhileStmt(getInfo(), Collections.emptyList());
    }

    public ClavaNode getWhileCondition() {
        // return getChild(0);
        return ClavaNodes.getChild(this, 0);
    }

    public CompoundStmt getThen() {
        // return getChild(Stmt.class, 1);
        return (CompoundStmt) ClavaNodes.getChild(this, 1);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        String conditionCode = getWhileCondition().getCode();

        code.append("while(").append(conditionCode).append(")").append(getThen().getCode());

        return code.toString();
    }

    @Override
    public CompoundStmt getBody() {
        return getThen();
    }

    @Override
    public Optional<ClavaNode> getStmtCondition() {
        return Optional.of(getWhileCondition());
    }

}
