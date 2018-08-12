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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class DecayedType extends AdjustedType {

    /// DATAKEYS BEGIN

    public final static DataKey<Type> DECAYED_TYPE = KeyFactory.object("decayedType", Type.class);

    public final static DataKey<Type> POINTEE_TYPE = KeyFactory.object("pointeeType", Type.class);

    /// DATAKEYS END

    public DecayedType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public DecayedType(TypeData typeData, ClavaNodeInfo info, Type originalType, Type adjustedType) {
    // super(typeData, info, originalType, adjustedType);
    // }
    //
    // private DecayedType(TypeData typeData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(typeData, info, children);
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new DecayedType(getTypeData(), getInfo(), Collections.emptyList());
    //
    // }

    // TODO: Verify if DECAYED_TYPE corresponds to OriginalType
    public Type getOriginalType() {
        return get(DECAYED_TYPE);
        // return getChild(Type.class, 0);
    }

    public void setOriginalType(Type originalType) {
        set(DECAYED_TYPE, originalType);
        setChild(0, originalType);
    }

    @Override
    public Type getAdjustedType() {
        return get(POINTEE_TYPE);
        // return getChild(Type.class, 1);
    }

    // Return the code of the original type
    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        return getOriginalType().getCode(sourceNode, name);
    }

    @Override
    public boolean isArray() {
        return getOriginalType().isArray();
    }

    // @Override
    // protected Type desugarImpl() {
    // return getOriginalType();
    // }
    //
    // @Override
    // protected void setDesugarImpl(Type desugaredType) {
    // setOriginalType(desugaredType);
    // }

    @Override
    public Type normalize() {
        return getOriginalType();
    }

}
