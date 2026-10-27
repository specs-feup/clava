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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.UnaryExprOrTypeTraitExpr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AUnaryExprOrType;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxUnaryExprOrType extends AUnaryExprOrType {

    private final UnaryExprOrTypeTraitExpr expr;

    public CxxUnaryExprOrType(UnaryExprOrTypeTraitExpr expr, CxxWeaver weaver) {
        super(new CxxExpression(expr, weaver), weaver);

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

        return CxxJoinpoints.create(expr.getArgumentType().get(), getWeaverEngine(), AType.class);
    }

    @Override
    public AExpression getArgExprImpl() {
        if (!expr.hasArgumentExpression()) {
            return null;
        }

        return CxxJoinpoints.create(expr.getArgumentExpression(), getWeaverEngine(), AExpression.class);
    }

    @Override
    public void setArgTypeImpl(AType argType) {
        if (!expr.hasTypeExpression()) {
            SpecsLogs.msgInfo("UnaryExprOrType '" + expr.getUettKind() + "' does not have a type argument");
            return;
        }

        expr.setArgType((Type) argType.getNode());
    }

    @Override
    public String getKindImpl() {
        return expr.getUettKind().getString();
    }
}
