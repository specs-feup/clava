/**
 * Copyright 2017 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AUnaryOp;

import java.util.Arrays;
import java.util.List;

public class CxxUnaryOp extends AUnaryOp {

//    private final UnaryOperator unaryOp;

    public CxxUnaryOp(UnaryOperator unaryOp) {
        super(unaryOp);
//        this.unaryOp = unaryOp;
    }

    @Override
    public UnaryOperator getNode() {
        return (UnaryOperator) super.getNode();
    }

    @Override
    public AExpression getOperandImpl() {
        return CxxJoinpoints.create(ClavaNodes.normalize(getNode().getSubExpr()), AExpression.class);
    }

    @Override
    public Boolean getIsPointerDerefImpl() {
        return getNode().getOp() == UnaryOperatorKind.Deref;
    }

    @Override
    public List<? extends AExpression> selectOperand() {
        return Arrays.asList((AExpression) getOperandImpl());
    }

    @Override
    public Boolean getIsBitwiseImpl() {
        return getNode().getOp().isBitwise();
    }

}
