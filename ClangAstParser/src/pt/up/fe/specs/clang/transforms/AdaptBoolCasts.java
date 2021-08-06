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
import pt.up.fe.specs.clava.ast.expr.CXXFunctionalCastExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Clang always parse bool types as _Bool, even if we are parsing C++ objects.
 * 
 * @author JoaoBispo
 *
 */
public class AdaptBoolCasts implements SimplePostClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof CXXFunctionalCastExpr)) {
            return;
        }

        CXXFunctionalCastExpr castExpr = (CXXFunctionalCastExpr) node;

        // if (!castExpr.getTargetType().equals("_Bool")) {
        if (!castExpr.getTypeCode().equals("_Bool")) {
            return;
        }

        // Get sub-expression
        Expr subExpr = castExpr.getSubExpr();

        // Remove parent to avoid copying of the subtree
        subExpr.detach();

        CXXFunctionalCastExpr newCastExpr = castExpr.getFactory().cxxFunctionalCastExpr(castExpr, subExpr);

        queue.replace(node, newCastExpr);
    }

}
