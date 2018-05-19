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
import java.util.Collections;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.enums.UnaryTransformTypeKind;
import pt.up.fe.specs.util.SpecsCollections;

public class UnaryTransformType extends Type {

    private final UnaryTransformTypeKind kind;
    private final boolean hasBaseType;
    private final boolean hasUnderlyingType;

    public UnaryTransformType(UnaryTransformTypeKind kind, TypeData data, ClavaNodeInfo info, Type baseType,
            Type underlyingType) {

        this(kind, data, info, baseType != null, underlyingType != null,
                SpecsCollections.asListT(Type.class, baseType, underlyingType));
    }

    private UnaryTransformType(UnaryTransformTypeKind kind, TypeData data, ClavaNodeInfo info, boolean hasBaseType,
            boolean hasUnderlyingType, Collection<? extends ClavaNode> children) {

        super(data, info, children);

        this.kind = kind;
        this.hasBaseType = hasBaseType;
        this.hasUnderlyingType = hasUnderlyingType;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new UnaryTransformType(kind, getTypeData(), getInfo(), hasBaseType, hasUnderlyingType,
                Collections.emptyList());
    }

    public Optional<Type> getBaseType() {
        if (!hasBaseType) {
            return Optional.empty();
        }

        return Optional.of(getChild(Type.class, 0));
    }

    public Optional<Type> getUnderlyingType() {
        if (!hasUnderlyingType) {
            return Optional.empty();
        }

        int index = 0;
        if (hasBaseType) {
            index++;
        }

        return Optional.of(getChild(Type.class, index));
    }

    @Override
    protected Type desugarImpl() {
        // System.out.println("CURRENT TYPE:" + getCode());
        // System.out.println("HAS BASE?:" + hasBaseType);
        // if (hasBaseType) {
        // System.out.println("BASE:" + getBaseType().get().getCode());
        // }
        // System.out.println("HAS UNDERLYING:" + hasUnderlyingType);
        // if (hasUnderlyingType) {
        // System.out.println("UNDERLYING:" + getUnderlyingType().get().getCode());
        // }

        return getBaseType().get();
    }

    @Override
    public boolean hasSugar() {
        return hasBaseType;
    }
}
