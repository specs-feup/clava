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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * Represents a builtin binary operation expression.
 * 
 * 
 * @author JoaoBispo
 *
 */
public class BinaryOperator extends Expr {

    public static enum BinaryOperatorKind implements StringProvider {
        PTR_MEM_D,
        PTR_MEM_I,
        MUL("*"),
        DIV("/"),
        REM("%"),
        ADD("+"),
        SUB("-"),
        SHL("<<"),
        SHR(">>"),
        LT("<"),
        GT(">"),
        LE("<="),
        GE(">="),
        EQ("=="),
        NE("!="),
        AND("&"),
        XOR("^"),
        OR("|"),
        L_AND("&&"),
        L_OR("||"),
        ASSIGN("=", true),
        MUL_ASSIGN("*=", true),
        DIV_ASSIGN("/=", true),
        REM_ASSIGN("%=", true),
        ADD_ASSIGN("+=", true),
        SUB_ASSIGN("-=", true),
        SHL_ASSIGN("<<=", true),
        SHR_ASSIGN(">>=", true),
        AND_ASSIGN("&=", true),
        XOR_ASSIGN("^=", true),
        OR_ASSIGN("|=", true),
        COMMA(",");

        private static final Lazy<EnumHelperWithValue<BinaryOperatorKind>> HELPER = EnumHelperWithValue
                .newLazyHelper(BinaryOperatorKind.class);

        public static EnumHelperWithValue<BinaryOperatorKind> getHelper() {
            return HELPER.get();
        }

        private final String opString;
        private final boolean isAssign;

        private BinaryOperatorKind(String opString, boolean isAssign) {
            this.opString = opString;
            this.isAssign = isAssign;
        }

        private BinaryOperatorKind(String opString) {
            this(opString, false);
        }

        private BinaryOperatorKind() {
            this("<UNDEFINED>");
        }

        public String getOpString() {
            return opString;
        }

        @Override
        public String getString() {
            return getOpString();
        }

    }

    private final BinaryOperatorKind op;

    public BinaryOperator(BinaryOperatorKind op, ExprData exprData, ClavaNodeInfo info,
            Expr lhs, Expr rhs) {

        this(op, exprData, info, Arrays.asList(lhs, rhs));
    }

    protected BinaryOperator(BinaryOperatorKind op, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(exprData, info, children);

        this.op = op;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new BinaryOperator(op, getExprData(), getInfo(), Collections.emptyList());
    }

    public Expr getLhs() {
        return getChild(Expr.class, 0);
    }

    public Expr getRhs() {
        return getChild(Expr.class, 1);
    }

    public BinaryOperatorKind getOp() {
        return op;
    }

    @Override
    public String toContentString() {
        return super.toContentString() + ", op:" + op;
    }

    @Override
    public String getCode() {
        String opCode = getLhs().getCode() + " " + op.getOpString() + " " + getRhs().getCode();

        return opCode;
    }

}
