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

package pt.up.fe.specs.clang.transforms;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.ast.omp.clang.AClangOMP;
import pt.up.fe.specs.clava.ast.stmt.CapturedStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Removes Clang nodes related with OpenMP.
 * 
 * @author JoaoBispo
 *
 */
public class RemoveClangOmpNodes implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {

        if (!(node instanceof AClangOMP)) {
            return;
        }

        for (ClavaNode child : node.getChildren()) {
            if (isEmptyImplicitBody((AClangOMP) node, child)) {
                continue;
            }

            queue.moveBefore(node, child);
        }

        queue.delete(node);
    }

    private boolean isEmptyImplicitBody(AClangOMP directive, ClavaNode child) {
        SourceLocation directiveStart = directive.getLocation().getStart();
        if (child instanceof CapturedStmt) {
            Stmt capturedStatement = ((CapturedStmt) child).getCapturedStatement();
            return isEmptyImplicitCompound(capturedStatement, directiveStart);
        }

        return isEmptyImplicitCompound(child, directiveStart);
    }

    private boolean isEmptyImplicitCompound(ClavaNode node, SourceLocation directiveStart) {
        if (!(node instanceof CompoundStmt) || node.hasChildren()) {
            return false;
        }

        // Clang's standalone directives carry an empty captured placeholder at the pragma
        // location. Empty source bodies, including macro expansions, have a different anchor.
        SourceLocation start = node.getLocation().getStart();
        SourceLocation end = node.getLocation().getEnd();
        return start.equals(end) && start.equals(directiveStart) && !start.isMacro();
    }

}
