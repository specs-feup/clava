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
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AArrayAccess;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref;

public class CxxArrayAccess extends AArrayAccess {

    private final ArraySubscriptExpr arraySub;
    private final ACxxWeaverJoinPoint parent;

    public CxxArrayAccess(ArraySubscriptExpr arraySub, ACxxWeaverJoinPoint parent) {
        super(new CxxExpression(arraySub, parent));
        this.arraySub = arraySub;
        this.parent = parent;
    }

    // @Override
    // public ACxxWeaverJoinPoint getParentImpl() {
    // return parent;
    // }

    @Override
    public ClavaNode getNode() {
        return arraySub;
    }

    @Override
    public AExpression getArrayVarImpl() {
        return (AExpression) CxxJoinpoints.create(arraySub.getArrayExpr(), this);
    }

    // @Override
    // public AJoinpoint[] getSubscriptImpl() {
    // return (AExpression) CxxJoinpoints.create(arraySub.getSubscript(), this);
    // }

    @Override
    public AExpression[] getSubscriptArrayImpl() {
        return arraySub.getSubscripts().stream()
                .map(expr -> (AExpression) CxxJoinpoints.create(expr, this))
                .toArray(length -> new AExpression[length]);
    }

    @Override
    public AVardecl getVardeclImpl() {
        AExpression arrayVar = getArrayVarImpl();

        if ((arrayVar instanceof AVarref)) {
            return null;
        }

        return ((AVarref) arrayVar).getVardeclImpl();

        // return ((AExpression) CxxJoinpoints.create(arraySub.getArrayExpr(), this)).getVardeclImpl();
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

}
