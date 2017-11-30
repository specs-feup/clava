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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CXXMemberCallExpr;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.clava.utils.StmtWithCondition;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Clang always parse bool types as _Bool, even if we are parsing C++ objects.
 * 
 * @author JoaoBispo
 *
 */
public class RemoveBoolOperatorCalls implements SimplePostClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        // Find MemberCalls
        if (!(node instanceof CXXMemberCallExpr)) {
            return;
        }

        CXXMemberCallExpr memberCall = (CXXMemberCallExpr) node;

        MemberExpr memberExpr = memberCall.getCallee();

        // Find operator bool
        if (!memberExpr.getMemberName().equals("operator bool")) {
            return;
        }

        Preconditions.checkArgument(memberCall.getArgs().isEmpty(), "Expected operator to have no arguments");

        // Check if inside condition of while, for or if
        ClavaNode condition = node.getAscendantsStream()
                .filter(ascendant -> ascendant instanceof StmtWithCondition)
                .map(ascendant -> (StmtWithCondition) ascendant)
                .findFirst()
                .flatMap(stmt -> stmt.getStmtCondition())
                .orElse(null);

        if (condition == null) {
            return;
        }

        // Check if node is a child of condition
        boolean isInsideCondition = node.getAscendantsAndSelfStream()
                .filter(ascendant -> ascendant == condition)
                .findFirst().isPresent();

        if (!isInsideCondition) {
            return;
        }

        // Remove member call to operator bool

        queue.replace(node, memberExpr.getBase());

    }

}
