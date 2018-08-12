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
public class BinaryOperator extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<BinaryOperatorKind> OP = KeyFactory.enumeration("op", BinaryOperatorKind.class);

    /// DATAKEYS END

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

}
