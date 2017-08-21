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

import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CXXConstructExpr;
import pt.up.fe.specs.clava.ast.expr.CXXNewExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Removes constructors that are implicit in the source code, but that where added by Clang.
 * 
 * <p>
 * Example: std::string a = String("a_string"); instead of std::string a = "a_string";<br>
 * 
 * 
 * @author JoaoBispo
 *
 */
public class RemoveImplicitConstructors implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof CXXConstructExpr)) {
            return;
        }

        CXXConstructExpr constructorExpr = (CXXConstructExpr) node;
        List<Expr> args = constructorExpr.getArgs();

        // If is elidable, check that has a single non-default argument and remove
        if (constructorExpr.isElidable()) {

            Preconditions.checkArgument(args.size() == 1);
            queue.replace(constructorExpr, args.get(0));
            return;
        }

        // If constructor has more than one non-default argument, should always appear in the code
        if (args.size() != 1) {
            return;
        }

        // If only one non-default arg, most of the time the constructor should be omitted
        // One case where it should not is when the parent is a CxxNewExpr
        if (constructorExpr.getParent() instanceof CXXNewExpr) {
            return;
        }

        // Otherwise, remove constructor
        queue.replace(constructorExpr, args.get(0));
    }

}
