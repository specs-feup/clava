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
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
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
public class UnaryExprOrTypeTraitExpr extends Expr {
    /// DATAKEYS BEGIN

    public final static DataKey<UnaryExprOrTypeTrait> KIND = KeyFactory.enumeration("kind", UnaryExprOrTypeTrait.class);

    public final static DataKey<Boolean> IS_ARGUMENT_TYPE = KeyFactory.bool("isArgumentType");

    public final static DataKey<Optional<Type>> ARG_TYPE = KeyFactory.optional("argType");

    public final static DataKey<String> SOURCE_LITERAL = KeyFactory.string("sourceLiteral");

    /// DATAKEYS END

    public UnaryExprOrTypeTraitExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public UnaryExprOrTypeTrait getUettKind() {
        return get(KIND);
    }

    public Optional<Type> getArgumentType() {
        return get(ARG_TYPE);
    }

    public Expr getArgumentExpression() {
        return getChild(Expr.class, 0);
    }

    public boolean hasArgumentExpression() {
        return !get(IS_ARGUMENT_TYPE);
    }

    public boolean hasTypeExpression() {
        return get(IS_ARGUMENT_TYPE);
    }

    @Override
    public String getCode() {

        switch (getUettKind()) {
        case SizeOf:
            return "sizeof(" + getExpressionCode() + ")";
        default:
            ClavaLog.debug(() -> "UnaryExprOrTypeTraitExpr.getCode(): Not implemented yet for kind '" + getUettKind()
                    + "', using 'sourceLiteral'");
            return get(SOURCE_LITERAL);
        }

    }

    private String getExpressionCode() {
        if (hasArgumentExpression()) {
            return getArgumentExpression().getCode();
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
