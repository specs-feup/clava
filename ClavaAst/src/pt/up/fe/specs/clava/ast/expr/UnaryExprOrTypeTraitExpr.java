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
import java.util.Collections;
import java.util.Optional;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.UnaryExprOrTypeTrait;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * Represents an expression with a type or with an unevaluated expression operand.
 * 
 * For instance, sizeof, alignof (C) or vec_step (OpenCL).
 * 
 * @author JoaoBispo
 *
 */
public class UnaryExprOrTypeTraitExpr extends Expr {

    private final UnaryExprOrTypeTrait uettKind;
    private Type argType;

    // private final String exprName;
    // private final String argType;

    // public UnaryExprOrTypeTraitExpr(String exprName, String argType, Type type,
    // ClavaNodeInfo info) {
    // this(exprName, argType, type, info, Collections.emptyList());
    // }

    public UnaryExprOrTypeTraitExpr(UnaryExprOrTypeTrait uettKind, Type argType, ExprData exprData, ClavaNodeInfo info,
            Expr argumentExpression) {

        this(uettKind, argType, exprData, info, SpecsCollections.ofNullable(argumentExpression));
    }

    private UnaryExprOrTypeTraitExpr(UnaryExprOrTypeTrait uettKind, Type argType, ExprData exprData,
            ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(exprData, info, children);

        this.uettKind = uettKind;
        this.argType = argType;

        if (argType == null) {
            // This can happen when copying nodes
            // Preconditions.checkArgument(!children.isEmpty(), "Not sure if this should hold");
        } else {
            Preconditions.checkArgument(children.isEmpty(), "Not sure if this should hold");
        }

    }

    @Override
    protected ClavaNode copyPrivate() {
        return new UnaryExprOrTypeTraitExpr(uettKind, argType, getExprData(), getInfo(), Collections.emptyList());
    }

    public UnaryExprOrTypeTrait getUettKind() {
        return uettKind;
    }

    public Optional<Type> getArgumentType() {
        return Optional.ofNullable(argType);
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
        return hasChildren();
    }

    public boolean hasTypeExpression() {
        return argType != null;
    }

    @Override
    public String getCode() {
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
        /*        
        if (getArgumentType().isPresent()) {
            // System.out.println("UETT CODE 1:" + uettKind.getString() + "(" + getArgumentType().get().getCode() +
            // ")");
            return uettKind.getString() + "(" + getArgumentType().get().getCode() + ")";
        }
        
        // System.out.println("UETT CODE 2:" + uettKind.getString() + "(" + getArgumentExpression().getCode() + ")");
        return uettKind.getString() + "(" + getArgumentExpression().getCode() + ")";
        
        // if (hasArgumentExpression()) {
        // return exprName + "(" + getArgumentExpression().getCode() + ")";
        // // LoggingUtils.msgWarn("Code with arg expr, not tested:" + getLocation());
        // }
        // return exprName + "(" + getArgType() + ")";
         
         */
    }

    @Override
    public String toContentString() {
        String argTypeString = argType == null ? null : argType.getBareType();
        return toContentString(super.toContentString(),
                "uett kind: " + uettKind + ", arg type: " + argTypeString);
    }

    public void setArgType(Type argType) {
        if (this.argType == null) {
            SpecsLogs.msgInfo("Cannot set type when kind is '" + uettKind + "'");
            return;
        }

        this.argType = argType;

    }

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
