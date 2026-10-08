/**
 * Copyright 2017 SPeCS.
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

import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.utils.Nameable;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AArrayAccess;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref;

public class CxxArrayAccess<Self extends CxxArrayAccess<Self>> extends AArrayAccess<Self> {

    public CxxArrayAccess(ArraySubscriptExpr arraySub, CxxWeaver weaver) {
        super(arraySub, weaver);
    }

    @Override
    public ArraySubscriptExpr getNodeImpl() {
        return (ArraySubscriptExpr) super.getNodeImpl();
    }

    @Override
    public AExpression<?> getArrayVarImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getArrayExpr(), getWeaverEngine(), AExpression.class);
    }

    @Override
    public AExpression<?>[] getSubscriptImpl() {
        return this.getNodeImpl().getSubscripts().stream()
                .map(expr -> CxxJoinpoints.create(expr, getWeaverEngine(), AExpression.class))
                .toArray(AExpression<?>[]::new);
    }

    @Override
    public AVardecl<?> getVardeclImpl() {
        AExpression<?> arrayVar = getArrayVarImpl();

        if (arrayVar instanceof AVarref varref) {
            return varref.getVardeclImpl();
        }

        return null;
    }

    @Override
    public ADecl<?> getDeclImpl() {
        return getVardeclImpl();
    }

    @Override
    public AArrayAccess<?> getParentAccessImpl() {
        return this.getNodeImpl().getParentAccess()
                .map(parentAccess -> CxxJoinpoints.create(parentAccess, getWeaverEngine(), AArrayAccess.class))
                .orElse(null);
    }

    @Override
    public int getNumSubscriptsImpl() {
        return this.getNodeImpl().getSubscripts().size();
    }

    @Override
    public String getNameImpl() {
        var arrayVar = getArrayVarImpl().getNodeImpl();

        if (arrayVar instanceof Nameable nameable) {
            return nameable.getName();
        }

        return null;
    }

}
