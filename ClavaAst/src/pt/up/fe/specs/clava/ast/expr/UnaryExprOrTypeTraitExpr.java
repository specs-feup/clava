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

    // private final UnaryExprOrTypeTrait uettKind;
    // private Type argType;
    // private final String literalCode;
    // private boolean useLiteralCode;

    // private final String exprName;
    // private final String argType;

    // public UnaryExprOrTypeTraitExpr(String exprName, String argType, Type type,
    // ClavaNodeInfo info) {
    // this(exprName, argType, type, info, Collections.emptyList());
    // }

    // public UnaryExprOrTypeTraitExpr(UnaryExprOrTypeTrait uettKind, Type argType, String literalCode, ExprData
    // exprData,
    // ClavaNodeInfo info,
    // Expr argumentExpression) {
    //
    // this(uettKind, argType, literalCode, exprData, info, SpecsCollections.ofNullable(argumentExpression));
    // }
    //
    // private UnaryExprOrTypeTraitExpr(UnaryExprOrTypeTrait uettKind, Type argType, String literalCode, ExprData
    // exprData,
    // ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(exprData, info, children);
    //
    // this.uettKind = uettKind;
    // this.argType = argType;
    // this.literalCode = literalCode;
    // // this.useLiteralCode = false;
    //
    // if (argType == null) {
    // // This can happen when copying nodes
    // // Preconditions.checkArgument(!children.isEmpty(), "Not sure if this should hold");
    // } else {
    // // useLiteralCode = true;
    // // Preconditions.checkArgument(children.isEmpty(), "Not sure if this should hold");
    // }
    //
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new UnaryExprOrTypeTraitExpr(uettKind, argType, literalCode, getExprData(), getInfo(),
    // Collections.emptyList());
    // }

    public UnaryExprOrTypeTrait getUettKind() {
        return get(KIND);
        // return uettKind;
    }

    public Optional<Type> getArgumentType() {
        return get(ARG_TYPE);
        // return Optional.ofNullable(argType);
    }

    // public String getExprName() {
    // return exprName;
    // }
    //
    // public String getArgType() {
    // return argType;
    // }

    public Expr getArgumentExpression() {
        return getChild(Expr.class, 0);
    }

    public boolean hasArgumentExpression() {
        return !get(IS_ARGUMENT_TYPE);
    }

    public boolean hasTypeExpression() {
        return get(IS_ARGUMENT_TYPE);
        // return argType != null;
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

        // return literalCode;
        /*
        if (useLiteralCode) {
            return literalCode;
        }
        
        boolean useParenthesis = true;
        if (uettKind == UnaryExprOrTypeTrait.SIZE_OF && hasArgumentExpression()) {
            useParenthesis = false;
        }
        
        String argumentCode = hasArgumentExpression() ? getArgumentExpression().getCode()
                : getArgumentType().get().getCode(this);
        
        if (useParenthesis) {
            return uettKind.getString() + "(" + argumentCode + ")";
        }
        
        return uettKind.getString() + " " + argumentCode;
        */
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

    // @Override
    // public String toContentString() {
    // String argTypeString = argType == null ? null : argType.getBareType();
    // return toContentString(super.toContentString(),
    // "uett kind: " + uettKind + ", arg type: " + argTypeString);
    // }

    // public void setArgType(Type argType) {
    // if (this.argType == null) {
    // SpecsLogs.msgInfo("Cannot set type when kind is '" + uettKind + "'");
    // return;
    // }
    //
    // this.argType = argType;
    //
    // }

    /**
     * Special case: if sizeof, returns argument type.
     */
    /*
    @Override
    public Type getType() {
        if (uettKind == UnaryExprOrTypeTrait.SIZE_OF) {
            return argType;
        }
    
        return super.getType();
    }
    */

    /**
     * Special case: if sizeof, sets argument type.
     */
    /*
    @Override
    public void setType(Type type) {
        if (uettKind == UnaryExprOrTypeTrait.SIZE_OF) {
            this.argType = type;
            return;
        }
    
        super.setType(type);
    }
    */
}
