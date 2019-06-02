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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.UnaryExprOrTypeTraitExpr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AUnaryExprOrType;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxUnaryExprOrType extends AUnaryExprOrType {

    private final UnaryExprOrTypeTraitExpr expr;

    public CxxUnaryExprOrType(UnaryExprOrTypeTraitExpr expr) {
        super(new CxxExpression(expr));

        this.expr = expr;
    }

    @Override
    public ClavaNode getNode() {
        return expr;
    }

    @Override
    public Boolean getHasTypeExprImpl() {
        return expr.hasTypeExpression();
    }

    @Override
    public Boolean getHasArgExprImpl() {
        return expr.hasArgumentExpression();
    }

    @Override
    public AType getArgTypeImpl() {
        if (!expr.hasTypeExpression()) {
            return null;
        }

        return CxxJoinpoints.create(expr.getArgumentType().get(), AType.class);
    }

    @Override
    public AExpression getArgExprImpl() {
        if (!expr.hasArgumentExpression()) {
            return null;
        }

        return CxxJoinpoints.create(expr.getArgumentExpression(), AExpression.class);
    }

    @Override
    public void defArgTypeImpl(AType value) {
        if (!expr.hasTypeExpression()) {
            SpecsLogs.msgInfo("UnaryExprOrType '" + expr.getUettKind() + "' does not have a type argument");
            return;
        }

        expr.setArgType((Type) value.getNode());

        // SpecsLogs.msgInfo("Setting the argument type of an UnaryExprOrType is currently disabled");
        return;
        // if (!expr.hasTypeExpression()) {
        // SpecsLogs.msgInfo("UnaryExprOrType '" + expr.getUettKind() + "' does not have a type argument");
        // return;
        // }
        //
        // expr.setArgType((Type) value.getNode());
    }

    @Override
    public void setArgTypeImpl(AType argType) {
        defArgTypeImpl(argType);
    }

    @Override
    public String getKindImpl() {
        return expr.getUettKind().getString();
    }
}
