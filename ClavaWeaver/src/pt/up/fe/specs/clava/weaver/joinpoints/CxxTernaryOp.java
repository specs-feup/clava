/**
 * Copyright 2021 SPeCS.
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

import pt.up.fe.specs.clava.ast.expr.ConditionalOperator;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATernaryOp;

public class CxxTernaryOp<Self extends CxxTernaryOp<Self>> extends ATernaryOp<Self> {

    public CxxTernaryOp(ConditionalOperator op, CxxWeaver weaver) {
        super(op, weaver);
    }

    @Override
    public ConditionalOperator getNodeImpl() {
        return (ConditionalOperator) super.getNodeImpl();
    }

    @Override
    public AExpression<?> getCondImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getCondition(), getWeaverEngine(), AExpression.class);
    }

    @Override
    public AExpression<?> getTrueExprImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getTrueExpr(), getWeaverEngine(), AExpression.class);
    }

    @Override
    public AExpression<?> getFalseExprImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getFalseExpr(), getWeaverEngine(), AExpression.class);
    }
}
