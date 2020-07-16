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
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorPosition;

/**
 * Represents a unary expression.
 * 
 * @author JoaoBispo
 *
 */
public class UnaryOperator extends Operator {

    /// DATAKEYS BEGIN

    public final static DataKey<UnaryOperatorKind> OP = KeyFactory.enumeration("op", UnaryOperatorKind.class);

    public final static DataKey<UnaryOperatorPosition> POSITION = KeyFactory.enumeration("position",
            UnaryOperatorPosition.class);

    /// DATAKEYS END

    // Hardcoding kind names, in order to avoid breaking code that depends on these names when changing Clang
    // version
    private static final Map<UnaryOperatorKind, String> KIND_NAMES;
    static {
        KIND_NAMES = new HashMap<>();
        KIND_NAMES.put(UnaryOperatorKind.PostInc, "post_inc");
        KIND_NAMES.put(UnaryOperatorKind.PostDec, "post_dec");
        KIND_NAMES.put(UnaryOperatorKind.PreInc, "pre_inc");
        KIND_NAMES.put(UnaryOperatorKind.PreDec, "pre_dec");
        KIND_NAMES.put(UnaryOperatorKind.AddrOf, "addr_of");
        KIND_NAMES.put(UnaryOperatorKind.Deref, "deref");
        KIND_NAMES.put(UnaryOperatorKind.Plus, "plus");
        KIND_NAMES.put(UnaryOperatorKind.Minus, "minus");
        KIND_NAMES.put(UnaryOperatorKind.Not, "not");
        KIND_NAMES.put(UnaryOperatorKind.LNot, "l_not");
        KIND_NAMES.put(UnaryOperatorKind.Real, "real");
        KIND_NAMES.put(UnaryOperatorKind.Imag, "imag");
        KIND_NAMES.put(UnaryOperatorKind.Extension, "extension");
        KIND_NAMES.put(UnaryOperatorKind.Coawait, "cowait");
    }

    public UnaryOperator(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final UnaryOperatorKind opcode;
    // private final UnaryOperatorPosition position;
    //
    // /**
    // * @param opcode
    // * @param isPrefix
    // * @param valueKind
    // * @param type
    // * @param info
    // * @param subExpr
    // */
    // public UnaryOperator(UnaryOperatorKind opcode, UnaryOperatorPosition position, ExprData exprData,
    // ClavaNodeInfo info, Expr subExpr) {
    // this(opcode, position, exprData, info, Arrays.asList(subExpr));
    // }
    //
    // private UnaryOperator(UnaryOperatorKind opcode, UnaryOperatorPosition position, ExprData expr, ClavaNodeInfo
    // info,
    // List<? extends Expr> children) {
    // super(expr, info, children);
    //
    // this.opcode = opcode;
    // this.position = position;
    //
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new UnaryOperator(opcode, position, getExprData(), getInfo(), Collections.emptyList());
    // }

    @Override
    public String getCode() {

        StringBuilder builder = new StringBuilder();

        // Get code of child
        builder.append(getSubExpr().getCode());
        UnaryOperatorKind opcode = get(OP);
        String code = opcode.getCode();

        switch (get(POSITION)) {
        case PREFIX:
            if (opcode.requiresSpace()) {
                code += " ";
            }

            builder.insert(0, code);
            break;
        case POSTFIX:

            if (opcode.requiresSpace()) {
                code = " " + code;
            }

            builder.append(code);
            break;
        }

        return builder.toString();
    }

    public UnaryOperatorKind getOp() {
        return get(OP);
    }

    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
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

    // @Override
    // public String toContentString() {
    // return super.toContentString() + ", op:" + opcode;
    // }

    @Override
    public String getOperatorCode() {
        return getOp().getCode();
    }
}
