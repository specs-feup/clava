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

package pt.up.fe.specs.clava.ast.expr.legacy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.ImplicitCastExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.CastKind;

/**
 * Represents an implicit type conversions which has no direct representation in the original source code.
 * 
 * @author JoaoBispo
 *
 */
public class ImplicitCastExprLegacy extends ImplicitCastExpr {

    public ImplicitCastExprLegacy(CastKind castKind, ExprData exprData, ClavaNodeInfo info, Expr subExpr) {
        super(castKind, exprData, info, Arrays.asList(subExpr));
    }

    /**
     * Constructor for node copy.
     * 
     * @param castKind
     * @param destinationTypes
     * @param location
     */
    private ImplicitCastExprLegacy(CastKind castKind, ExprData exprData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        super(castKind, exprData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new ImplicitCastExprLegacy(getCastKind(), getExprData(), getInfo(), Collections.emptyList());
    }

    @Override
    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        // The code of its only child
        return getSubExpr().getCode();
    }

    @Override
    public String toContentString() {
        return super.toContentString() + " (type:" + getExprType().getCode(this) + ", CastKind:" + getCastKind() + ")";
    }

    /**
     * Implicit cast overrides getType() and returns the type of sub expression.
     */
    @Override
    public Type getType() {
        return getSubExpr().getType();
    }

    @Override
    public Type getCastType() {
        return super.getType();
    }

}
