/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class CompoundAssignOperator extends BinaryOperator {

    public CompoundAssignOperator(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final Type lhsType;
    // private final Type resultType;
    //
    // public CompoundAssignOperator(Type lhsType, Type resultType, BinaryOperatorKind op, ExprData exprData,
    // ClavaNodeInfo info, Expr lhs, Expr rhs) {
    //
    // super(op, exprData, info, lhs, rhs);
    //
    // this.lhsType = lhsType;
    // this.resultType = resultType;
    // }

    /**
     * Constructor for copy function.
     * 
     * @param lhsType
     * @param resultType
     * @param op
     * @param valueKind
     * @param type
     * @param info
     */
    // private CompoundAssignOperator(Type lhsType, Type resultType, BinaryOperatorKind op,
    // ExprData exprData, ClavaNodeInfo info) {
    //
    // super(op, exprData, info, Collections.emptyList());
    //
    // this.lhsType = lhsType;
    // this.resultType = resultType;
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new CompoundAssignOperator(lhsType, resultType, getOp(), getExprData(), getInfo());
    // }

    @Override
    public String getCode() {
        // return getLhs().getCode() + " " + getOp().getOpString() + "= " + getRhs().getCode();
        return getLhs().getCode() + " " + getOp().getOpString() + " " + getRhs().getCode();
    }

}
