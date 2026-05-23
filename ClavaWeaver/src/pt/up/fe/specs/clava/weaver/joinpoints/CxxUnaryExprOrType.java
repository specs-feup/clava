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

import pt.up.fe.specs.clava.ast.expr.UnaryExprOrTypeTraitExpr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AUnaryExprOrType;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxUnaryExprOrType<Self extends CxxUnaryExprOrType<Self>> extends AUnaryExprOrType<Self> {

    public CxxUnaryExprOrType(UnaryExprOrTypeTraitExpr expr, CxxWeaver weaver) {
        super(expr, weaver);
    }

    @Override
    public UnaryExprOrTypeTraitExpr getNodeImpl() {
        return (UnaryExprOrTypeTraitExpr) super.getNodeImpl();
    }

    @Override
    public boolean getHasTypeExprImpl() {
        return this.getNodeImpl().hasTypeExpression();
    }

    @Override
    public boolean getHasArgExprImpl() {
        return this.getNodeImpl().hasArgumentExpression();
    }

    @Override
    public AType<?> getArgTypeImpl() {
        var expr = this.getNodeImpl();

        if (!expr.hasTypeExpression()) {
            return null;
        }

        return CxxJoinpoints.create(expr.getArgumentType().get(), getWeaverEngine(), AType.class);
    }

    @Override
    public AExpression<?> getArgExprImpl() {
        var expr = this.getNodeImpl();

        if (!expr.hasArgumentExpression()) {
            return null;
        }

        return CxxJoinpoints.create(expr.getArgumentExpression(), getWeaverEngine(), AExpression.class);
    }

    @Override
    public void setArgTypeImpl(AType<?> argType) {
        var expr = this.getNodeImpl();

        if (!expr.hasTypeExpression()) {
            SpecsLogs.msgInfo("UnaryExprOrType '" + expr.getUettKind() + "' does not have a type argument");
            return;
        }

        expr.setArgType((Type) argType.getNodeImpl());
    }

    @Override
    public String getKindImpl() {
        return this.getNodeImpl().getUettKind().getString();
    }
}
