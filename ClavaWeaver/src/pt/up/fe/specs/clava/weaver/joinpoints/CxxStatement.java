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

import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CxxStatement<Self extends CxxStatement<Self>> extends AStatement<Self> {

    public CxxStatement(Stmt stmt, CxxWeaver weaver) {
        super(stmt, weaver);
    }

    @Override
    public Stmt getNodeImpl() {
        return (Stmt) super.getNodeImpl();
    }

    @Override
    public AJoinpoint<?> replaceWithImpl(AJoinpoint<?> node) {
        // First "transform" node to insert into a statement
        Stmt newStmt = ClavaNodes.toStmt(node.getNodeImpl());

        NodeInsertUtils.replace(this.getNodeImpl(), newStmt);

        // Return a statement joinpoint
        return CxxJoinpoints.create(newStmt, getWeaverEngine());
    }

    @Override
    public boolean getIsFirstImpl() {
        // Get parent and check Stmt position on that list
        return this.getNodeImpl().getParent().getChildren(Stmt.class).indexOf(this.getNodeImpl()) == 0;
    }

    @Override
    public boolean getIsLastImpl() {
        // Get parent and check Stmt position on that list
        List<Stmt> siblings = this.getNodeImpl().getParent().getChildren(Stmt.class);
        return siblings.indexOf(this.getNodeImpl()) == (siblings.size() - 1);
    }
}
