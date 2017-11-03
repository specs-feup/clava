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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * Represents a prvalue (pure rvalue - does not have identity, can be moved) temporary that is written into memory so
 * that a reference can bind to it.
 * 
 * <p>
 * MaterializeTemporaryExprs are always glvalues (either an lvalue or an xvalue, depending on the kind of reference
 * binding to it), maintaining the invariant that references always bind to glvalues.
 * 
 * @author JoaoBispo
 *
 */
public class MaterializeTemporaryExpr extends Expr {

    private final BareDeclData extendingDecl;

    public MaterializeTemporaryExpr(ExprData exprData, BareDeclData extendingDecl, ClavaNodeInfo info,
            Expr temporaryExpr) {
        this(exprData, extendingDecl, info, Arrays.asList(temporaryExpr));
    }

    private MaterializeTemporaryExpr(ExprData exprData, BareDeclData extendingDecl, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(exprData, info, children);

        this.extendingDecl = extendingDecl;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new MaterializeTemporaryExpr(getExprData(), extendingDecl, getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        return getTemporaryExpr().getCode();
    }

    public Expr getTemporaryExpr() {
        return getChild(Expr.class, 0);
    }

}
