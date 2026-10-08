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

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABinaryOp;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;

public class CxxBinaryOp<Self extends CxxBinaryOp<Self>> extends ABinaryOp<Self> {

    public CxxBinaryOp(BinaryOperator op, CxxWeaver weaver) {
        super(op, weaver);
    }

    @Override
    public BinaryOperator getNodeImpl() {
        return (BinaryOperator) super.getNodeImpl();
    }

    @Override
    public AExpression<?> getLeftImpl() {
        List<? extends AExpression<?>> left = Arrays.asList((AExpression<?>) CxxJoinpoints.create(this.getNodeImpl().getLhs(),
                getWeaverEngine()));
        return left.isEmpty() ? null : left.get(0);
    }

    @Override
    public AExpression<?> getRightImpl() {
        List<? extends AExpression<?>> right = Arrays.asList((AExpression<?>) CxxJoinpoints.create(this.getNodeImpl().getRhs(),
                getWeaverEngine()));
        return right.isEmpty() ? null : right.get(0);
    }

    @Override
    public boolean getIsAssignmentImpl() {
        return this.getNodeImpl().getOp() == BinaryOperatorKind.Assign || this.getNodeImpl() instanceof CompoundAssignOperator;
    }

    @Override
    public boolean getIsBitwiseImpl() {
        return this.getNodeImpl().getOp().isBitwise();
    }

    @Override
    public void setLeftImpl(AExpression<?> left) {
        this.getNodeImpl().setLhs((Expr) left.getNodeImpl());
    }

    @Override
    public void setRightImpl(AExpression<?> right) {
        this.getNodeImpl().setRhs((Expr) right.getNodeImpl());
    }
}
