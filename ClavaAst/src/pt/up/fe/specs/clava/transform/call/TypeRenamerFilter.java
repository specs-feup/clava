/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.transform.call;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.ParenType;
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.VariableArrayType;

/**
 * Determines if a node inside a Type should be renamed.
 * 
 * @author JoaoBispo
 *
 */
public class TypeRenamerFilter {

    /**
     * Returns a list of nodes that can be safely renamed.
     * 
     * @param type
     * @return
     */
    public List<ClavaNode> nodesToRename(Type type) {
        // System.out.println("START");
        List<ClavaNode> result = new ArrayList<>();
        typeVisitor(type, result);
        return result;
    }

    private void typeVisitor(Type type, List<ClavaNode> currentNodes) {

        if (type instanceof PointerType) {
            // Copy pointee, continue through it
            Type pointeeCopy = type.get(PointerType.POINTEE_TYPE).copy();
            type.set(PointerType.POINTEE_TYPE, pointeeCopy);

            typeVisitor(pointeeCopy, currentNodes);
            return;
        }

        if (type instanceof ParenType) {
            // Copy desugar, continue through it
            Type desugarCopy = type.desugar().copy();
            type.setDesugar(desugarCopy);

            typeVisitor(desugarCopy, currentNodes);
            return;
        }

        if (type instanceof VariableArrayType) {
            Expr sizeExprCopy = (Expr) type.get(VariableArrayType.SIZE_EXPR).copy();
            type.set(VariableArrayType.SIZE_EXPR, sizeExprCopy);

            renameCandidate(sizeExprCopy, currentNodes);
            return;
        }

    }

    private void renameCandidate(ClavaNode node, List<ClavaNode> currentNodes) {

        currentNodes.addAll(node.getDescendants(DeclRefExpr.class));
        // System.out.println("RENAME CANDIDATE:" + node);

        // if (node instanceof ImplicitCastExpr) {
        // renameCandidate(((ImplicitCastExpr) node).getSubExpr(), currentNodes);
        // return;
        // }
        //
        // if (node instanceof DeclRefExpr) {
        // // This is a candidate for renaming
        // currentNodes.add(node);
        // return;
        // }

    }
}
