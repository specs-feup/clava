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

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CXXBindTemporaryExpr;
import pt.up.fe.specs.clava.ast.expr.ExprWithCleanups;
import pt.up.fe.specs.clava.ast.expr.MaterializeTemporaryExpr;
import pt.up.fe.specs.clava.ast.expr.OpaqueValueExpr;
import pt.up.fe.specs.clava.ast.stmt.CapturedStmt;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Removes a set of nodes that are not needed for code generation.
 * 
 * <p>
 * This transformation needs to be applied with pre-order traversal, otherwise removal of consecutive nodes will fail.
 * 
 * @author JoaoBispo
 *
 */
public class RemoveExtraNodes implements SimplePreClavaRule {

    private static Set<Class<? extends ClavaNode>> CLEANUP_NODES;
    static {
        RemoveExtraNodes.CLEANUP_NODES = new HashSet<>();
        RemoveExtraNodes.CLEANUP_NODES.add(ExprWithCleanups.class);
        RemoveExtraNodes.CLEANUP_NODES.add(MaterializeTemporaryExpr.class);
        RemoveExtraNodes.CLEANUP_NODES.add(CXXBindTemporaryExpr.class);
        RemoveExtraNodes.CLEANUP_NODES.add(OpaqueValueExpr.class);
        RemoveExtraNodes.CLEANUP_NODES.add(CapturedStmt.class);

        // RemoveExtraNodes.CLEANUP_NODES.add(ImplicitCastExpr.class);
    }

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        // if (CXXBindTemporaryExpr.class.isInstance(node)) {
        // System.out.println("PRE-CXXBIND:" + node.getLocation());
        // }

        if (!RemoveExtraNodes.CLEANUP_NODES.contains(node.getClass())) {
            return;
        }

        Preconditions.checkArgument(node.getNumChildren() == 1, "Expected a single child");

        queue.replace(node, node.getChild(0));
    }

}
