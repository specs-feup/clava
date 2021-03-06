/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;

public class VariableArrayType extends ArrayType {

    /// DATAKEYS BEGIN

    // public final static DataKey<Optional<Expr>> SIZE_EXPR = KeyFactory.optional("sizeExpr");
    public final static DataKey<Expr> SIZE_EXPR = KeyFactory.object("sizeExpr", Expr.class);

    /// DATAKEYS END

    public VariableArrayType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // Dummy
    // public VariableArrayType(ArrayTypeData arrayTypeData, TypeData typeData, ClavaNodeInfo info, Type elementType,
    // Expr sizeExpr) {
    // this(sizeExpr, arrayTypeData, typeData, info, Arrays.asList(elementType, sizeExpr));
    // }

    // protected VariableArrayType(Expr sizeExpr, ArrayTypeData arrayTypeData, TypeData typeData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    // super(arrayTypeData, typeData, info, children);
    //
    // // setInPlace(SIZE_EXPR, sizeExpr);
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new VariableArrayType((Expr) getExpr().copy(), getArrayTypeData(), getTypeData(), getInfo(),
    // Collections.emptyList());
    // }

    // @Override
    // public Type getElementType() {
    // return getChild(Type.class, 0);
    // }

    // public Optional<Expr> getExpr() {
    public Expr getExpr() {
        return get(SIZE_EXPR);
        // return getChild(Expr.class, 1);
    }

    @Override
    protected String getArrayCode() {
        // System.out.println("VLA:" + this);
        // System.out.println("SIZE_EXPR:" + get(SIZE_EXPR).toTree());

        // get(SIZE_EXPR).getDescendants(DeclRefExpr.class).stream()
        // .forEach(declRef -> System.out.println("DECL: " + declRef.get(DeclRefExpr.DECL)));
        // System.out.println("EXPR CODE:" + getExpr().getCode());
        // System.out.println("VARIABLE ARRAY TYPEEXPR:" + getExpr());
        return getExpr().getCode();
        // return getExpr().map(Expr::getCode).orElse("");
    }

}
