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

public class WhileStmt extends LoopStmt {

    public WhileStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<VarDecl> getDeclCondition() {
        return getOptionalChild(VarDecl.class, 0);
    }

    public ExprStmt getCondition() {
        return getChild(ExprStmt.class, 1);
    }

    public CompoundStmt getThen() {
        return (CompoundStmt) ClavaNodes.getChild(this, 2);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        String conditionCode = getStmtCondition().map(ClavaNode::getCode).orElse("");
        if (conditionCode.endsWith(";")) {
            conditionCode = conditionCode.substring(0, conditionCode.length() - 1);
        }

        code.append("while(").append(conditionCode).append(")").append(getThen().getCode());

        return code.toString();
    }

    @Override
    public CompoundStmt getBody() {
        return getThen();
    }

    @Override
    public Optional<ClavaNode> getStmtCondition() {
        var nullableStmtCondition = getDeclCondition().map(ClavaNode.class::cast).orElse(getCondition());
        return Optional.of(nullableStmtCondition);
    }

}
