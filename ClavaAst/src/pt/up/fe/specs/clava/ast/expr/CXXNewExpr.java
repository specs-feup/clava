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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.enums.NewInitStyle;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * Represents a new-expression e.g: "new Object(foo)".
 * 
 * @author Jo�o Bispo
 *
 */
public class CXXNewExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_GLOBAL = KeyFactory.bool("isGlobalNew");
    public final static DataKey<Boolean> IS_ARRAY = KeyFactory.bool("isArray");
    public final static DataKey<Boolean> HAS_INITIALIZER = KeyFactory.bool("hasInitializer");
    public final static DataKey<NewInitStyle> INIT_STYLE = KeyFactory.enumeration("initStyle", NewInitStyle.class);
    public final static DataKey<Optional<Expr>> INITIALIZER = KeyFactory.optional("initializer");
    public final static DataKey<Optional<CXXConstructExpr>> CONSTRUCT_EXPR = KeyFactory.optional("constructExpr");
    public final static DataKey<Optional<Expr>> ARRAY_SIZE = KeyFactory.optional("arraySize");
    public final static DataKey<Optional<FunctionDecl>> OPERATOR_NEW = KeyFactory.optional("operatorNew");

    /// DATAKEYS ENDs

    public CXXNewExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final BareDeclData newOperator;
    //
    // private final boolean isGlobal;
    // private final boolean isArray;
    // private final boolean hasConstructor;
    // private final boolean hasNothrow;

    /*
    private final Long newFunctionId;
    private final String typeSourceInfo;
    private final Type ftype;
    */
    /**
     * Constructor for new arrays.
     * 
     * @param valueKind
     * @param type
     * @param info
     * @param children
     */
    // public CXXNewExpr(boolean isGlobal, boolean isArray, BareDeclData newOperator, ExprData exprData,
    // ClavaNodeInfo info, Expr arraySize, Expr constructorExpr, DeclRefExpr nothrow) {
    //
    // this(isGlobal, isArray, constructorExpr != null, nothrow != null, newOperator, exprData, info,
    // SpecsCollections.asListT(ClavaNode.class, arraySize, constructorExpr, nothrow));
    // }
    //
    // private CXXNewExpr(boolean isGlobal, boolean isArray, boolean hasConstructor, boolean hasNothrow,
    // BareDeclData newOperator, ExprData exprData,
    // ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    //
    // super(exprData, info, children);
    //
    // this.isGlobal = isGlobal;
    // this.isArray = isArray;
    // this.hasConstructor = hasConstructor;
    // this.hasNothrow = hasNothrow;
    //
    // this.newOperator = newOperator;
    // /*
    // this.isArray = isArray;
    // this.hasNothrow = hasNothrow;
    // this.hasConstructor = hasConstructor;
    //
    // this.newFunctionId = newFunctionId;
    // this.typeSourceInfo = typeSourceInfo;
    // this.ftype = ftype;
    // */
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new CXXNewExpr(isGlobal, isArray, hasConstructor, hasNothrow, newOperator, getExprData(), getInfo(),
    // Collections.emptyList());
    // }

    public Optional<CXXConstructExpr> getConstructorExpr() {
        return get(CONSTRUCT_EXPR);
        // if (!hasConstructor) {
        // return Optional.empty();
        // }
        //
        // // If hasArray, is the second child. Otherwise, is the first child
        // int constructorIndex = isArray ? 1 : 0;
        //
        // return Optional.of(getChild(Expr.class, constructorIndex));
    }

    public Optional<Expr> getArrayExpr() {
        return get(ARRAY_SIZE);
        // if (!isArray) {
        // return Optional.empty();
        // }
        //
        // // Array expr is always the first child
        // return Optional.of(getChild(Expr.class, 0));
    }

    @Override
    public String getCode() {
        // System.out.println("Operator new:" + get(OPERATOR_NEW).map(ClavaNode::getCode).orElse("null"));
        // System.out.println("ARRAY:" + get(ARRAY_SIZE).map(ClavaNode::getCode).orElse("null"));
        // System.out.println("Construct:" + get(CONSTRUCT_EXPR).map(ClavaNode::getCode).orElse("null"));
        StringBuilder code = new StringBuilder();

        code.append("new ");

        boolean hasNothrow = get(OPERATOR_NEW).flatMap(fdecl -> SpecsCollections.lastTry(fdecl.getParameters()))
                .filter(param -> param.getCode().startsWith("std::nothrow_"))
                .isPresent();

        if (hasNothrow) {
            code.append("(std::nothrow) ");
        }

        Optional<CXXConstructExpr> constructorExpr = getConstructorExpr();

        if (constructorExpr.isPresent()) {
            // Special case: literal
            Expr expr = constructorExpr.get();
            if (expr instanceof Literal) {
                Type exprType = getExprType();
                if (Types.isPointer(exprType)) {
                    exprType = Types.getPointeeType(exprType);
                }

                code.append(exprType.getCode(this)).append("(").append(expr.getCode()).append(")");
            } else {
                code.append(expr.getCode());
            }

        } else {
            Type exprType = getExprType();
            if (Types.isPointer(exprType)) {
                exprType = Types.getPointeeType(exprType);
            }
            code.append(exprType.getCode(this));

            switch (get(INIT_STYLE)) {
            case NO_INIT:
                // Do nothing
                break;
            case CALL_INIT:
                code.append("(").append(get(INITIALIZER).get().getCode()).append(")");
                break;
            default:
                throw new NotImplementedException(get(INIT_STYLE));
            }

            getArrayExpr().ifPresent(arrayExpr -> code.append("[").append(arrayExpr.getCode()).append("]"));

        }

        // getArrayExpr().ifPresent(arrayExpr -> code.append("[").append(arrayExpr.getCode()).append("]"));
        /*
        if (code.toString().equals("new *graph")) {
            System.out.println("CONST:" + getConstructorExpr());
            System.out.println("CONST CODE:" + constructorExpr.get().getCode());
        }
        */

        return code.toString();
        /*
        if (isArray) {
            // new char[1024];
            StringBuilder code = new StringBuilder();
        
            code.append("new ");
        
         
            Type exprType = getExprType();
            if (Types.isPointer(exprType)) {
                exprType = Types.getPointeeType(exprType);
            }
        
            code.append(exprType.getCode());
            code.append("[").append(getChild(0).getCode()).append("]");
        
            
            // CHECK: Since it is an array, the type must be a pointer
        //            if (!(exprType instanceof PointerTypeOld)) {
        //                throw new RuntimeException("Should be PointerTypeOld?");
        //            }
        //
        //            Type baseType = ((PointerTypeOld) exprType).getPointeeType();
        //
        //            code.append(baseType.getCode());
        //            code.append("[").append(getChild(0).getCode()).append("]");
            
            return code.toString();
        }
        
        throw new RuntimeException("Not implemented yet");
        */
    }
}
