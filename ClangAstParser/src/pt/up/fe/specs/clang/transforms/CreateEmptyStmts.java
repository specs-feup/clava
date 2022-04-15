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

package pt.up.fe.specs.clang.transforms;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.NullStmt;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.clava.utils.NodeWithScope;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * NullStmt that do not represent absence of statement should be converted to empty statement.
 * 
 * @author João Bispo
 *
 */
public class CreateEmptyStmts implements SimplePostClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {

        // Do nothing if node is not a NullStmt
        if (!(node instanceof NullStmt)) {
            return;
        }

        var parent = node.getParent();

        // If direct child of a statement with scope or if, assume it represents absence of statement
        if (parent instanceof NodeWithScope || parent instanceof IfStmt) {
            return;
        }

        queue.replace(node, node.getFactoryWithNode().emptyStmt());
    }

}
