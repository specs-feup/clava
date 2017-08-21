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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * Represents an array subscript.
 * 
 * @author JoaoBispo
 *
 */
public class ArraySubscriptExpr extends Expr {

    public ArraySubscriptExpr(ExprData exprData, ClavaNodeInfo info, Expr lhs, Expr rhs) {
        this(exprData, info, Arrays.asList(lhs, rhs));
    }

    private ArraySubscriptExpr(ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(exprData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new ArraySubscriptExpr(getExprData(), getInfo(), Collections.emptyList());
    }

    public Expr getLhs() {
        return getChild(Expr.class, 0);
    }

    public Expr getRhs() {
        return getChild(Expr.class, 1);
    }

    @Override
    public String getCode() {
        return getLhs().getCode() + "[" + getRhs().getCode() + "]";
    }

    /**
     * Visit first childs until a declRef is found.
     * 
     * @return
     */
    public DeclRefExpr getDeclRef() {
        ClavaNode currentNode = getChild(0);
        while (!(currentNode instanceof DeclRefExpr)) {
            Preconditions.checkArgument(currentNode.hasChildren(), "Expected to find DeclRefExpr:" + this);
            currentNode = currentNode.getChild(0);
        }

        return (DeclRefExpr) currentNode;
    }

}
