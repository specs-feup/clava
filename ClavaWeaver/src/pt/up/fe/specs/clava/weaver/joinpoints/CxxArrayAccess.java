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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.utils.Nameable;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AArrayAccess;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref;

public class CxxArrayAccess extends AArrayAccess {

    private final ArraySubscriptExpr arraySub;

    public CxxArrayAccess(ArraySubscriptExpr arraySub) {
        super(new CxxExpression(arraySub));
        this.arraySub = arraySub;
    }

    @Override
    public ClavaNode getNode() {
        return arraySub;
    }

    @Override
    public AExpression getArrayVarImpl() {
        return (AExpression) CxxJoinpoints.create(arraySub.getArrayExpr());
    }

    @Override
    public AExpression[] getSubscriptArrayImpl() {
        return arraySub.getSubscripts().stream()
                .map(expr -> (AExpression) CxxJoinpoints.create(expr))
                .toArray(length -> new AExpression[length]);
    }

    @Override
    public AVardecl getVardeclImpl() {
        AExpression arrayVar = getArrayVarImpl();

        if (!(arrayVar instanceof AVarref)) {
            return null;
        }

        return ((AVarref) arrayVar).getVardeclImpl();

    }

    @Override
    public ADecl getDeclImpl() {
        return getVardeclImpl();
    }

    @Override
    public List<? extends AVardecl> selectVardecl() {
        return CxxExpression.selectVarDecl(this);
    }

    @Override
    public List<? extends AExpression> selectArrayVar() {
        return Arrays.asList(getArrayVarImpl());
    }

    @Override
    public List<? extends AExpression> selectSubscript() {
        return Arrays.asList(getSubscriptArrayImpl());
    }

    @Override
    public AArrayAccess getParentAccessImpl() {
        return arraySub.getParentAccess()
                .map(parentAccess -> CxxJoinpoints.create(parentAccess, AArrayAccess.class))
                .orElse(null);
    }

    @Override
    public Integer getNumSubscriptsImpl() {
        return arraySub.getSubscripts().size();
    }

    @Override
    public String getNameImpl() {
        var arrayVar = getArrayVarImpl().getNode();

        if (!(arrayVar instanceof Nameable)) {
            return null;
        }

        return ((Nameable) arrayVar).getName();
    }

}
