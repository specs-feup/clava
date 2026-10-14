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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.AdjustedType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.collections.SpecsList;

/**
 * Represents the declaration of a variable, function or enum constant.
 * 
 * @author JoaoBispo
 *
 */
public abstract class ValueDecl extends ValueDeclBase implements Typable {

    // Data declarations are generated from the wire schema in ValueDeclBase.

    public ValueDecl(DataStore data, Collection<? extends ClavaNode> children, ValueDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public ValueDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public Type getType() {
        return getWireType();
    }

    @Override
    public void setType(Type type) {
        set(TYPE, type);
    }

    @Override
    public Optional<AdjustedType> getAdjustedType() {
        if (!hasValue(ADJUSTED_TYPE)) {
            return Optional.empty();
        }
        return get(ADJUSTED_TYPE);
    }

    @Override
    public void setAdjustedType(AdjustedType type) {
        set(ADJUSTED_TYPE, Optional.of(type));
    }

    @Override
    public SpecsList<String> getSignatureCustomStrings() {
        SpecsList<String> list = super.getSignatureCustomStrings();

        if (hasValue(TYPE)) {
            list.andAdd(getWireType().get(Type.TYPE_AS_STRING));
        }

        return list;
    }
}
