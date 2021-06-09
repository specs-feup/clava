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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.ConditionalOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATernaryOp;

public class CxxTernaryOp extends ATernaryOp {

    private final ConditionalOperator op;

    public CxxTernaryOp(ConditionalOperator op) {
        super(new CxxOp(op));

        this.op = op;
    }

    @Override
    public ClavaNode getNode() {
        return op;
    }

    @Override
    public AExpression getCondImpl() {
        return CxxJoinpoints.create(op.getCondition(), AExpression.class);
    }

    @Override
    public AExpression getTrueExprImpl() {
        return CxxJoinpoints.create(op.getTrueExpr(), AExpression.class);
    }

    @Override
    public AExpression getFalseExprImpl() {
        return CxxJoinpoints.create(op.getFalseExpr(), AExpression.class);
    }

    @Override
    public List<? extends AExpression> selectCond() {
        return Arrays.asList(getCondImpl());
    }

    @Override
    public List<? extends AExpression> selectTrueExpr() {
        return Arrays.asList(getTrueExprImpl());
    }

    @Override
    public List<? extends AExpression> selectFalseExpr() {
        return Arrays.asList(getFalseExprImpl());
    }

    @Override
    public void defCondImpl(AExpression value) {
        op.setCondition((Expr) value.getNode());
    }

    @Override
    public void defTrueExprImpl(AExpression value) {
        op.setTrueExpr((Expr) value.getNode());
    }

    @Override
    public void defFalseExprImpl(AExpression value) {
        op.setFalseExpr((Expr) value.getNode());
    }

}
