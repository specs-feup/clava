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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.List;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CxxStatement extends AStatement {

    private final Stmt stmt;

    public CxxStatement(Stmt stmt) {
        this.stmt = stmt;
    }

    @Override
    public ClavaNode getNode() {
        return stmt;
    }

    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        // First "transform" node to insert into a statement
        Stmt newStmt = ClavaNodes.toStmt(node.getNode());

        NodeInsertUtils.replace(stmt, newStmt);

        // Return a statement joinpoint
        return CxxJoinpoints.create(newStmt);
    }

    @Override
    public Boolean getIsFirstImpl() {
        // Get parent and check Stmt position on that list
        return stmt.getParent().getChildren(Stmt.class).indexOf(stmt) == 0;

        // return stmt.indexOfSelf() == 1;
        // List<? extends AStatement> statementJps = parent.selectStatements();
        // Preconditions.checkArgument(!statementJps.isEmpty(), "Expected parent to ");
    }

    @Override
    public Boolean getIsLastImpl() {
        // Get parent and check Stmt position on that list
        List<Stmt> siblings = stmt.getParent().getChildren(Stmt.class);
        return siblings.indexOf(stmt) == (siblings.size() - 1);

        // return stmt.indexOfSelf() == stmt.getParentImpl().numChildren();
    }
}
