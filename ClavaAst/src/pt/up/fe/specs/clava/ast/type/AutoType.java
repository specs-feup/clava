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
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class AutoType extends Type {

    /// DATAKEYS BEGIN

    /**
     * The type deduced for this auto type, or empty if it's either not been deduced or was deduced to a dependent type.
     */
    public final static DataKey<Optional<Type>> DEDUCED_TYPE = KeyFactory.optional("deducedType");

    /// DATAKEYS END

    public AutoType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<Type> getDeducedType() {
        return get(DEDUCED_TYPE);
    }

    public void setDeducedType(Type deducedType) {
        set(DEDUCED_TYPE, Optional.ofNullable(deducedType));
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        if (name == null) {
            return "auto";
        }

        return "auto" + " " + name;
    }

}
