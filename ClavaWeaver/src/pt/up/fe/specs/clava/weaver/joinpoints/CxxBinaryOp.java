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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABinaryOp;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;

public class CxxBinaryOp extends ABinaryOp {

    // Hardcoding kind names, in order to avoid breaking LARA code that depends on these names when changing Clang
    // version
    private static final Map<BinaryOperatorKind, String> KIND_NAMES;
    static {
        KIND_NAMES = new HashMap<>();
        KIND_NAMES.put(BinaryOperatorKind.PtrMemD, "ptr_mem_d");
        KIND_NAMES.put(BinaryOperatorKind.PtrMemI, "ptr_mem_i");
        KIND_NAMES.put(BinaryOperatorKind.Mul, "mul");
        KIND_NAMES.put(BinaryOperatorKind.Div, "div");
        KIND_NAMES.put(BinaryOperatorKind.Rem, "rem");
        KIND_NAMES.put(BinaryOperatorKind.Add, "add");
        KIND_NAMES.put(BinaryOperatorKind.Sub, "sub");
        KIND_NAMES.put(BinaryOperatorKind.Shl, "shl");
        KIND_NAMES.put(BinaryOperatorKind.Shr, "shr");
        KIND_NAMES.put(BinaryOperatorKind.Cmp, "cmp");
        KIND_NAMES.put(BinaryOperatorKind.LT, "lt");
        KIND_NAMES.put(BinaryOperatorKind.GT, "gt");
        KIND_NAMES.put(BinaryOperatorKind.LE, "le");
        KIND_NAMES.put(BinaryOperatorKind.GE, "ge");
        KIND_NAMES.put(BinaryOperatorKind.EQ, "eq");
        KIND_NAMES.put(BinaryOperatorKind.NE, "ne");
        KIND_NAMES.put(BinaryOperatorKind.And, "and");
        KIND_NAMES.put(BinaryOperatorKind.Xor, "xor");
        KIND_NAMES.put(BinaryOperatorKind.Or, "or");
        KIND_NAMES.put(BinaryOperatorKind.LAnd, "l_and");
        KIND_NAMES.put(BinaryOperatorKind.LOr, "l_or");
        KIND_NAMES.put(BinaryOperatorKind.Assign, "assign");
        KIND_NAMES.put(BinaryOperatorKind.MulAssign, "mul_assign");
        KIND_NAMES.put(BinaryOperatorKind.DivAssign, "div_assign");
        KIND_NAMES.put(BinaryOperatorKind.RemAssign, "rem_assign");
        KIND_NAMES.put(BinaryOperatorKind.AddAssign, "add_assign");
        KIND_NAMES.put(BinaryOperatorKind.SubAssign, "sub_assign");
        KIND_NAMES.put(BinaryOperatorKind.ShlAssign, "shl_assign");
        KIND_NAMES.put(BinaryOperatorKind.ShrAssign, "shr_assign");
        KIND_NAMES.put(BinaryOperatorKind.AndAssign, "and_assign");
        KIND_NAMES.put(BinaryOperatorKind.XorAssign, "xor_assign");
        KIND_NAMES.put(BinaryOperatorKind.OrAssign, "or_assign");
        KIND_NAMES.put(BinaryOperatorKind.Comma, "comma");

    }

    private final BinaryOperator op;
    private final ACxxWeaverJoinPoint parent;

    public CxxBinaryOp(BinaryOperator op, ACxxWeaverJoinPoint parent) {
        super(new CxxExpression(op, parent));

        this.op = op;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public ClavaNode getNode() {
        return op;
    }

    @Override
    public String getKindImpl() {
        String kindName = KIND_NAMES.get(op.getOp());
        if (kindName == null) {
            throw new RuntimeException("Not implemented for " + op);
        }

        return kindName;
    }

    @Override
    public List<? extends AExpression> selectLeft() {
        return Arrays.asList((AExpression) CxxJoinpoints.create(op.getLhs(), this));
    }

    @Override
    public List<? extends AExpression> selectRight() {
        return Arrays.asList((AExpression) CxxJoinpoints.create(op.getRhs(), this));
    }

    @Override
    public AJoinPoint getLeftImpl() {
        List<? extends AExpression> left = selectLeft();
        return left.isEmpty() ? null : left.get(0);
    }

    @Override
    public AJoinPoint getRightImpl() {
        List<? extends AExpression> right = selectRight();
        return right.isEmpty() ? null : right.get(0);
    }

    @Override
    public Boolean getIsAssignmentImpl() {
        return op.getOp() == BinaryOperatorKind.Assign || op instanceof CompoundAssignOperator;
    }
}
