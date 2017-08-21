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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * Represents a new-expression e.g: "new Object(foo)".
 * 
 * @author João Bispo
 *
 */
public class CXXNewExpr extends Expr {

    private final BareDeclData newOperator;

    private final boolean isGlobal;
    private final boolean isArray;
    private final boolean hasConstructor;
    private final boolean hasNothrow;

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
    public CXXNewExpr(boolean isGlobal, boolean isArray, BareDeclData newOperator, ExprData exprData,
            ClavaNodeInfo info, Expr arraySize, Expr constructorExpr, DeclRefExpr nothrow) {

        this(isGlobal, isArray, constructorExpr != null, nothrow != null, newOperator, exprData, info,
                SpecsCollections.asListT(ClavaNode.class, arraySize, constructorExpr, nothrow));
    }

    private CXXNewExpr(boolean isGlobal, boolean isArray, boolean hasConstructor, boolean hasNothrow,
            BareDeclData newOperator, ExprData exprData,
            ClavaNodeInfo info, Collection<? extends ClavaNode> children) {

        super(exprData, info, children);

        this.isGlobal = isGlobal;
        this.isArray = isArray;
        this.hasConstructor = hasConstructor;
        this.hasNothrow = hasNothrow;

        this.newOperator = newOperator;
        /*
        this.isArray = isArray;
        this.hasNothrow = hasNothrow;
        this.hasConstructor = hasConstructor;
        
        this.newFunctionId = newFunctionId;
        this.typeSourceInfo = typeSourceInfo;
        this.ftype = ftype;
        */
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXNewExpr(isGlobal, isArray, hasConstructor, hasNothrow, newOperator, getExprData(), getInfo(),
                Collections.emptyList());
    }

    public Optional<Expr> getConstructorExpr() {
        if (!hasConstructor) {
            return Optional.empty();
        }

        // If hasArray, is the second child. Otherwise, is the first child
        int constructorIndex = isArray ? 1 : 0;

        return Optional.of(getChild(Expr.class, constructorIndex));
    }

    public Optional<Expr> getArrayExpr() {
        if (!isArray) {
            return Optional.empty();
        }

        // Array expr is always the first child
        return Optional.of(getChild(Expr.class, 0));
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("new ");

        if (hasNothrow) {
            code.append("(std::nothrow) ");
        }

        Optional<Expr> constructorExpr = getConstructorExpr();
        if (constructorExpr.isPresent()) {
            code.append(constructorExpr.get().getCode());
        } else {
            Type exprType = getExprType();
            if (Types.isPointer(exprType)) {
                exprType = Types.getPointeeType(exprType);
            }
            code.append(exprType.getCode());
        }

        getArrayExpr().ifPresent(arrayExpr -> code.append("[").append(arrayExpr.getCode()).append("]"));
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
