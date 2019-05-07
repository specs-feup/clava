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
import java.util.HashMap;
import java.util.Map;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;

/**
 * Represents a builtin binary operation expression.
 * 
 * 
 * @author JoaoBispo
 *
 */
public class BinaryOperator extends Operator {

    /// DATAKEYS BEGIN

    public final static DataKey<BinaryOperatorKind> OP = KeyFactory.enumeration("op", BinaryOperatorKind.class);

    /// DATAKEYS END

    // Hardcoding kind names, in order to avoid breaking code that depends on these names when changing Clang
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

    public BinaryOperator(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final BinaryOperatorKind op;

    // public BinaryOperator(BinaryOperatorKind op, ExprData exprData, ClavaNodeInfo info,
    // Expr lhs, Expr rhs) {
    //
    // this(op, exprData, info, Arrays.asList(lhs, rhs));
    // }

    // protected BinaryOperator(BinaryOperatorKind op, ExprData exprData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    //
    // super(exprData, info, children);
    //
    // this.op = op;
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new BinaryOperator(op, getExprData(), getInfo(), Collections.emptyList());
    // }

    public Expr getLhs() {
        return getChild(Expr.class, 0);
    }

    public Expr getRhs() {
        return getChild(Expr.class, 1);
    }

    public Expr setLhs(Expr newLhs) {
        return (Expr) setChild(0, newLhs);
    }

    public Expr setRhs(Expr newRhs) {
        return (Expr) setChild(1, newRhs);
    }

    public BinaryOperatorKind getOp() {
        return get(OP);
        // return op;
    }

    // @Override
    // public String toContentString() {
    // return super.toContentString() + ", op:" + op;
    // }

    @Override
    public String getCode() {
        String opCode = getLhs().getCode() + " " + get(OP).getOpString() + " " + getRhs().getCode();

        return opCode;
    }

    @Override
    public boolean isBitwise() {
        return getOp().isBitwise();
    }

    @Override
    public String getKindName() {
        String kindName = KIND_NAMES.get(getOp());
        if (kindName == null) {
            throw new RuntimeException("Not implemented for " + getOp());
        }

        return kindName;
    }

}
