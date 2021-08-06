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

/**
 * A type to which a type attribute has been applied.
 * 
 * The "modified type" is the fully-sugared type to which the attributed type was applied; generally it is not
 * canonically equivalent to the attributed type. The "equivalent type" is the minimally-desugared type which the type
 * is canonically equivalent to.
 * 
 * @author JoaoBispo
 *
 */
public class AttributedType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<Type> MODIFIED_TYPE = KeyFactory.object("modifiedType", Type.class);

    public final static DataKey<Type> EQUIVALENT_TYPE = KeyFactory.object("equivalentType", Type.class);

    /// DATAKEYS END

    public AttributedType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Type getModifiedType() {
        return get(MODIFIED_TYPE);
    }

    public Type getEquivalentType() {
        return get(EQUIVALENT_TYPE);
    }
}
