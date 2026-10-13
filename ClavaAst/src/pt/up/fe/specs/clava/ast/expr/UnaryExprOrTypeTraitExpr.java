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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.UnaryExprOrTypeTrait;

/**
 * Represents an expression with a type or with an unevaluated expression operand.
 * 
 * For instance, sizeof, alignof (C) or vec_step (OpenCL).
 * 
 * @author JoaoBispo
 *
 */
public class UnaryExprOrTypeTraitExpr extends UnaryExprOrTypeTraitExprBase {

    // Data declarations are generated from the wire schema in UnaryExprOrTypeTraitExprBase.

    public UnaryExprOrTypeTraitExpr(DataStore data, Collection<? extends ClavaNode> children, UnaryExprOrTypeTraitExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public UnaryExprOrTypeTraitExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public UnaryExprOrTypeTrait getUettKind() {
        return getWireKind();
    }

    public Optional<Type> getArgumentType() {
        return getWireArgType();
    }

    public Expr getArgumentExpression() {
        return getChild(Expr.class, 0);
    }

    public boolean hasArgumentExpression() {
        return !getWireIsArgumentType();
    }

    public boolean hasTypeExpression() {
        return getWireIsArgumentType();
    }

    @Override
    public String getCode() {

        switch (getUettKind()) {
        case SizeOf:
            return "sizeof(" + getExpressionCode() + ")";
        default:
            ClavaLog.debug(() -> "UnaryExprOrTypeTraitExpr.getCode(): Not implemented yet for kind '" + getUettKind()
                    + "', using 'sourceLiteral'");
            return getWireSourceLiteral();
        }

    }

    private String getExpressionCode() {
        if (hasArgumentExpression()) {
            Expr argument = getArgumentExpression();
            // The sizeof printer supplies the operand's outer parentheses.
            return argument instanceof ParenExpr parenthesized
                    ? parenthesized.getSubExpr().getCode()
                    : argument.getCode();
        }

        if (hasTypeExpression()) {
            return getArgumentType().get().getCode(this);
        }

        throw new RuntimeException("Expression code not implemented yet for this node: " + this);
    }

    public void setArgType(Type node) {
        if (!hasTypeExpression()) {
            ClavaLog.info("UnaryExprOrType '" + getUettKind() + "' does not have a type argument");
            return;
        }

        set(ARG_TYPE, Optional.ofNullable(node));
    }

}
