/**
 * Copyright 2022 SPeCS.
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
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CXXMemberCallExpr;
import pt.up.fe.specs.clava.ast.expr.ParenExpr;
import pt.up.fe.specs.clava.ast.expr.PointerToMemberExpr;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Replaces callees of CXXMemberCallExpr that are pointer-to-member binary operations with PointerToMemberExpr nodes.
 * 
 * @author Joao Bispo
 *
 */
public class CreatePointerToMemberExpr implements SimplePostClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {

        // Only member calls
        if (!(node instanceof CXXMemberCallExpr)) {
            return;
        }
        // Check if callee is a ParenExpr
        var callee = node.getChild(0);

        if (!(callee instanceof ParenExpr)) {
            return;
        }

        var subExpr = ((ParenExpr) callee).getSubExpr();

        if (!(subExpr instanceof BinaryOperator)) {
            throw new RuntimeException("Expected BinaryOperator, got " + subExpr);
        }

        var binOp = (BinaryOperator) subExpr;

        if (binOp.getOp() != BinaryOperatorKind.PtrMemD && binOp.getOp() != BinaryOperatorKind.PtrMemI) {
            throw new RuntimeException(
                    "Expected BinaryOperator to be either a PtrMemD or PtrMemI, got " + binOp.getOp());
        }

        var pointerToMemberExpr = PointerToMemberExpr.newInstance(binOp);

        queue.replace(callee, pointerToMemberExpr);
    }

}
