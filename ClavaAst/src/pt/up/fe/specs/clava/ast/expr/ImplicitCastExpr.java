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

import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.CastKind;

/**
 * Represents an implicit type conversions which has no direct representation in the original source code.
 * 
 * @author JoaoBispo
 *
 */
public class ImplicitCastExpr extends CastExpr {

    public ImplicitCastExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * Legacy support.
     * 
     * @param castKind
     * @param exprData
     * @param info
     * @param subExpr
     */
    protected ImplicitCastExpr(CastKind castKind, ExprData exprData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        super(castKind, exprData, info, children);
    }

    // @Override
    // public Expr getSubExpr() {
    // return getChild(Expr.class, 0);
    // }

    @Override
    public String getCode() {
        // The code of its only child
        return getSubExpr().getCode();
    }

    /**
     * Implicit cast overrides getType() and returns the type of sub expression.
     */
    // @Override
    // public Type getType() {
    // return getSubExpr().getType();
    // }

    @Override
    public Type getCastType() {
        return super.getType();
    }

}
