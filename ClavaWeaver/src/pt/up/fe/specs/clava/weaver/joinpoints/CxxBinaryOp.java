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
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABinaryOp;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;

public class CxxBinaryOp extends ABinaryOp {

    private final BinaryOperator op;

    public CxxBinaryOp(BinaryOperator op) {
        super(new CxxOp(op));

        this.op = op;
    }

    @Override
    public ClavaNode getNode() {
        return op;
    }

    @Override
    public List<? extends AExpression> selectLeft() {
        return Arrays.asList((AExpression) CxxJoinpoints.create(op.getLhs()));
    }

    @Override
    public List<? extends AExpression> selectRight() {
        return Arrays.asList((AExpression) CxxJoinpoints.create(op.getRhs()));
    }

    @Override
    public AExpression getLeftImpl() {
        List<? extends AExpression> left = selectLeft();
        return left.isEmpty() ? null : left.get(0);
    }

    @Override
    public AExpression getRightImpl() {
        List<? extends AExpression> right = selectRight();
        return right.isEmpty() ? null : right.get(0);
    }

    @Override
    public Boolean getIsAssignmentImpl() {
        return op.getOp() == BinaryOperatorKind.Assign || op instanceof CompoundAssignOperator;
    }

    @Override
    public Boolean getIsBitwiseImpl() {
        return op.getOp().isBitwise();
    }

    @Override
    public void defLeftImpl(AExpression value) {
        op.setLhs((Expr) value.getNode());
    }

    @Override
    public void defRightImpl(AExpression value) {
        op.setRhs((Expr) value.getNode());
    }

    @Override
    public void setLeftImpl(AExpression left) {
        defLeftImpl(left);
    }

    @Override
    public void setRightImpl(AExpression right) {
        defRightImpl(right);
    }
}
