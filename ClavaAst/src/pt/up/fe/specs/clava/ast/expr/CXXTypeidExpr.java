/**
 * Copyright 2018 SPeCS.
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
import java.util.Optional;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.TypeidData;

/**
 * A C++ typeid expression (C++ [expr.typeid]), which gets the type_info that corresponds to the supplied type, or the
 * (possibly dynamic) type of the supplied expression
 * 
 * <p>
 * This represents code like typeid(int) or typeid(*objPtr)
 * 
 * @author JoaoBispo
 *
 */
public class CXXTypeidExpr extends Expr {

    private final TypeidData typeidData;

    public CXXTypeidExpr(TypeidData typeidData, ExprData exprData, ClavaNodeInfo info, Expr operandExpr) {
        this(typeidData, exprData, info, Arrays.asList(operandExpr));
    }

    public CXXTypeidExpr(TypeidData typeidData, ExprData exprData, ClavaNodeInfo info) {
        this(typeidData, exprData, info, Collections.emptyList());
    }

    private CXXTypeidExpr(TypeidData typeidData, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(exprData, info, children);

        this.typeidData = typeidData;

        if (typeidData.isTypeOperand()) {
            Preconditions.checkArgument(children.isEmpty());
        } else {
            Preconditions.checkArgument(children.size() == 1);
        }
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXTypeidExpr(typeidData, getExprData(), getInfo(), Collections.emptyList());
    }

    public Optional<Expr> getOperandExpr() {
        return typeidData.isTypeOperand() ? Optional.empty() : Optional.of(getChild(Expr.class, 0));
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("typeid(");
        if (typeidData.isTypeOperand()) {
            code.append(typeidData.getOperandType().getCode(this));
        } else {
            code.append(getOperandExpr().get().getCode());
        }

        code.append(")");

        return code.toString();
    }
}
