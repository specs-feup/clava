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
import pt.up.fe.specs.clava.ast.stmt.LabelStmt;
import pt.up.fe.specs.clava.ast.stmt.SwitchCase;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.classmap.ClassSet;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Moves the sub-statement of a certain nodes to be directly under it (as a sibling).
 * 
 * @author JoaoBispo
 *
 */
public class FlattenSubStmtNodes implements SimplePreClavaRule {

    private static final ClassSet<ClavaNode> SUB_STMT_NODES = ClassSet.newInstance(LabelStmt.class, SwitchCase.class);

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!SUB_STMT_NODES.contains(node)) {
            return;
        }

        // If it has no children, continue
        if (!node.hasChildren()) {
            return;
        }

        // Last child becomes a sibling
        int lastChildIndex = node.getNumChildren() - 1;
        queue.moveAfter(node, node.getChild(lastChildIndex));
    }

}
