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
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Moves the sub-statement of a Label to be directly under it (as a sibling).
 * 
 * @author JoaoBispo
 *
 */
public class NormalizeLabels implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof LabelStmt)) {
            return;
        }

        // If it has a children, move to after the node
        if (!node.hasChildren()) {
            return;
        }

        SpecsCheck.checkSize(node.getChildren(), 1);

        queue.moveAfter(node, node.getChild(0));
    }

}
