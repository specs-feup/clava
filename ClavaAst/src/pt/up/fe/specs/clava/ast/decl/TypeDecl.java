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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.utils.Typable;

/**
 * Represents a declaration of a type.
 * 
 * @author JoaoBispo
 *
 */
public abstract class TypeDecl extends NamedDecl implements Typable {

    /// DATAKEYS BEGIN

    /**
     * The type associated with this TypeDecl.
     */
    public final static DataKey<Type> TYPE_FOR_DECL = KeyFactory.object("type_for_decl", Type.class);

    /// DATAKEYS END

    public TypeDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * @deprecated
     * @param declName
     * @param type
     * @param declData
     * @param info
     * @param children
     */
    @Deprecated
    public TypeDecl(String declName, Type type, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(new LegacyToDataStore().setDecl(declData).setNodeInfo(info).getData(), children);

        set(NamedDecl.DECL_NAME, declName);
        set(TYPE_FOR_DECL, processType(type));
        // super(declName, type, declData, info, children);
    }

    @Override
    protected Type processType(Type type) {
        return type == null ? LegacyToDataStore.getFactory().nullType() : type.copy();
    }

    @Override
    public Type getType() {
        return get(TYPE_FOR_DECL);
    }

    @Override
    public void setType(Type type) {
        set(TYPE_FOR_DECL, type);
    }

}
