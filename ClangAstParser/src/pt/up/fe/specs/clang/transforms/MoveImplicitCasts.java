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
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.ImplicitCastExpr;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Moves implicit cast nodes to inside the corresponding sub-expression.
 * 
 * 
 * @author JoaoBispo
 *
 */
public class MoveImplicitCasts implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof ImplicitCastExpr)) {
            return;
        }

        ImplicitCastExpr implicitCastExpr = (ImplicitCastExpr) node;
        Expr subExpression = implicitCastExpr.getSubExpr();

        // Replace sub-expression with cast
        queue.replace(implicitCastExpr, subExpression);

        // Set cast as field of sub-expression
        subExpression.setImplicitCast(implicitCastExpr);

        // Set sub-expression as field of implicit cast
        implicitCastExpr.set(ImplicitCastExpr.SUB_EXPR, subExpression);

        // Add sub expression as dummy expr in implicit cast node
        // queue.addChild(implicitCastExpr, subExpression.getFactoryWithNode()
        // .dummyExpr("Implicit cast of node " + subExpression.get(ClavaNode.ID)));
    }

}
