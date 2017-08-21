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
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ValueKind;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Represents a unary expression.
 * 
 * @author JoaoBispo
 *
 */
public class UnaryOperator extends Expr {

    private final UnaryOperatorKind opcode;
    private final boolean isPrefix;

    public UnaryOperator(UnaryOperatorKind opcode, boolean isPrefix, ValueKind valueKind, Type type, ClavaNodeInfo info,
            Expr subExpr) {
        this(opcode, isPrefix, valueKind, type, info, Arrays.asList(subExpr));
    }

    private UnaryOperator(UnaryOperatorKind opcode, boolean isPrefix, ValueKind valueKind, Type type,
            ClavaNodeInfo info,
            List<? extends Expr> children) {
        super(valueKind, type, info, children);

        this.opcode = opcode;
        this.isPrefix = isPrefix;

    }

    @Override
    protected ClavaNode copyPrivate() {
        return new UnaryOperator(opcode, isPrefix, getValueKind(), getType(), getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        // Get code of child
        builder.append(getSubExpr().getCode());
        if (isPrefix) {
            builder.insert(0, opcode.op);
        } else {
            builder.append(opcode.op);
        }

        return builder.toString();
    }

    public UnaryOperatorKind getOp() {
        return opcode;
    }

    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String toContentString() {
        return super.toContentString() + ", op:" + opcode;
    }

    public static enum UnaryOperatorKind {
        POST_INC("++"),
        POST_DEC("--"),
        PRE_INC("++"),
        PRE_DEC("--"),
        // &
        ADDR_OF("&"),
        // *
        DEREF("*"),
        PLUS("+"),
        MINUS("-"),
        NOT("~"),
        // Logical not
        L_NOT("!"),
        REAL("<unknown_opcode>"),
        IMAG("<unknown_opcode>"),
        EXTENSION("<unknown_opcode>"),
        COAWAIT("<unknown_opcode>");

        private final String op;

        private UnaryOperatorKind(String op) {
            this.op = op;
        }

        public String getCode() {
            return op;
        }

        public String getName() {
            return name().toLowerCase();
        }

    }

}
