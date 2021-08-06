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
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.collections.SpecsList;

/**
 * Represents the declaration of a variable, function or enum constant.
 * 
 * @author JoaoBispo
 *
 */
public abstract class ValueDecl extends NamedDecl implements Typable {

    /// DATAKEYS BEGIN

    /**
     * The type associated with this ValueDecl.
     */
    public final static DataKey<Type> TYPE = KeyFactory.object("type", Type.class);

    /**
     * True if this symbol is weakly-imported, or declared with the weak or weak-reference attribute.
     */
    public final static DataKey<Boolean> IS_WEAK = KeyFactory.bool("is_weak");

    /// DATAKEYS END

    public ValueDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public Type getType() {
        return get(TYPE);
    }

    @Override
    public void setType(Type type) {
        set(TYPE, type);
    }

    @Override
    public SpecsList<String> getSignatureCustomStrings() {
        SpecsList<String> list = super.getSignatureCustomStrings();

        if (hasValue(TYPE)) {
            list.andAdd(get(TYPE).get(Type.TYPE_AS_STRING));
        }

        return list;
    }
}
