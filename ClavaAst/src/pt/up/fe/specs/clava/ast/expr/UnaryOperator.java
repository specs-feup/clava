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
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorPosition;

/**
 * Represents a unary expression.
 * 
 * @author JoaoBispo
 *
 */
public class UnaryOperator extends Expr {

    private final UnaryOperatorKind opcode;
    private final UnaryOperatorPosition position;

    /**
     * @param opcode
     * @param isPrefix
     * @param valueKind
     * @param type
     * @param info
     * @param subExpr
     */
    public UnaryOperator(UnaryOperatorKind opcode, UnaryOperatorPosition position, ExprData exprData,
            ClavaNodeInfo info, Expr subExpr) {
        this(opcode, position, exprData, info, Arrays.asList(subExpr));
    }

    private UnaryOperator(UnaryOperatorKind opcode, UnaryOperatorPosition position, ExprData expr, ClavaNodeInfo info,
            List<? extends Expr> children) {
        super(expr, info, children);

        this.opcode = opcode;
        this.position = position;

    }

    @Override
    protected ClavaNode copyPrivate() {
        return new UnaryOperator(opcode, position, getExprData(), getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        // Get code of child
        builder.append(getSubExpr().getCode());
        String code = opcode.getCode();

        switch (position) {
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
        return opcode;
    }

    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String toContentString() {
        return super.toContentString() + ", op:" + opcode;
    }

}
