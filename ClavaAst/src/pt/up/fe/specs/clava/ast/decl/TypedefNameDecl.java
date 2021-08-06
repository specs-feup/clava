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
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Base class for declarations which introduce a typedef-name.
 * 
 * @author JoaoBispo
 *
 */
public abstract class TypedefNameDecl extends TypeDecl {

    /// DATAKEYS BEGIN

    public final static DataKey<Type> UNDERLYING_TYPE = KeyFactory.object("underlyingType", Type.class);

    /// DATAKEYS END

    public TypedefNameDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public void setType(Type type) {
        super.setType(type);

        set(UNDERLYING_TYPE, type);
    }

    public Type getUnderlyingType() {
        return get(UNDERLYING_TYPE);
    }

}
