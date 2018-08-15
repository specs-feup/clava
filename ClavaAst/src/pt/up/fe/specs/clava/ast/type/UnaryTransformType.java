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
import pt.up.fe.specs.clava.ast.type.enums.UnaryTransformTypeKind;

/**
 * A unary type transform, which is a type constructed from another.
 * 
 * @author JoaoBispo
 *
 */
public class UnaryTransformType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<UnaryTransformTypeKind> KIND = KeyFactory
            .enumeration("kind", UnaryTransformTypeKind.class);

    public final static DataKey<Type> UNDERLYING_TYPE = KeyFactory.object("underlyingType", Type.class);

    public final static DataKey<Type> BASE_TYPE = KeyFactory.object("baseType", Type.class);

    /// DATAKEYS END

    public UnaryTransformType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final UnaryTransformTypeKind kind;
    // private final boolean hasBaseType;
    // private final boolean hasUnderlyingType;
    //
    // public UnaryTransformType(UnaryTransformTypeKind kind, TypeData data, ClavaNodeInfo info, Type baseType,
    // Type underlyingType) {
    //
    // this(kind, data, info, baseType != null, underlyingType != null,
    // SpecsCollections.asListT(Type.class, baseType, underlyingType));
    // }
    //
    // private UnaryTransformType(UnaryTransformTypeKind kind, TypeData data, ClavaNodeInfo info, boolean hasBaseType,
    // boolean hasUnderlyingType, Collection<? extends ClavaNode> children) {
    //
    // super(data, info, children);
    //
    // this.kind = kind;
    // this.hasBaseType = hasBaseType;
    // this.hasUnderlyingType = hasUnderlyingType;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new UnaryTransformType(kind, getTypeData(), getInfo(), hasBaseType, hasUnderlyingType,
    // Collections.emptyList());
    // }

    // public Optional<Type> getBaseType() {
    public Type getBaseType() {
        return get(BASE_TYPE);
        // if (!hasBaseType) {
        // return Optional.empty();
        // }
        //
        // return Optional.of(getChild(Type.class, 0));
    }

    // public Optional<Type> getUnderlyingType() {
    public Type getUnderlyingType() {
        return get(UNDERLYING_TYPE);
        // if (!hasUnderlyingType) {
        // return Optional.empty();
        // }
        //
        // int index = 0;
        // if (hasBaseType) {
        // index++;
        // }
        //
        // return Optional.of(getChild(Type.class, index));
    }

    // @Override
    // protected Type desugarImpl() {
    // // System.out.println("CURRENT TYPE:" + getCode());
    // // System.out.println("HAS BASE?:" + hasBaseType);
    // // if (hasBaseType) {
    // // System.out.println("BASE:" + getBaseType().get().getCode());
    // // }
    // // System.out.println("HAS UNDERLYING:" + hasUnderlyingType);
    // // if (hasUnderlyingType) {
    // // System.out.println("UNDERLYING:" + getUnderlyingType().get().getCode());
    // // }
    //
    // return getBaseType().get();
    // }
    //
    // @Override
    // public boolean hasSugar() {
    // return hasBaseType;
    // }
}
