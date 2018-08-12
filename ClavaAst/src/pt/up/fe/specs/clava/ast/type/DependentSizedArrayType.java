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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;

public class DependentSizedArrayType extends ArrayType {

    /// DATAKEYS BEGIN

    public final static DataKey<Optional<Expr>> SIZE_EXPR = KeyFactory.optional("sizeExpr");

    /// DATAKEYS END

    public DependentSizedArrayType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public DependentSizedArrayType(ArrayTypeData arrayTypeData, TypeData typeData, ClavaNodeInfo info,
    // Type elementType, Expr sizeExpr) {
    //
    // this(arrayTypeData, typeData, info, Arrays.asList(elementType, sizeExpr));
    // }
    //
    // private DependentSizedArrayType(ArrayTypeData arrayTypeData, TypeData typeData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    // this(new LegacyToDataStore().setArrayType(arrayTypeData).setType(typeData).setNodeInfo(info).getData(),
    // children);
    // // super(arrayTypeData, typeData, info, children);
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new DependentSizedArrayType(getArrayTypeData(), getTypeData(), getInfo(), Collections.emptyList());
    // }

    // @Override
    // public Type getElementType() {
    // return getChild(Type.class, 0);
    // }

    public Optional<Expr> getSizeExpr() {
        return get(SIZE_EXPR);

        // if (getNumChildren() < 2) {
        // return Optional.empty();
        // }
        //
        // Expr sizeExpr = getChild(Expr.class, 1);
        // return sizeExpr instanceof NullExpr ? Optional.empty() : Optional.of(sizeExpr);
    }

    @Override
    protected String getArrayCode() {
        return getSizeExpr().map(Expr::getCode).orElse("");
    }

}
