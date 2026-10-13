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

package pt.up.fe.specs.clava.ast.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Type that was implicitly adjusted.
 * 
 * @author JoaoBispo
 *
 */
public abstract class AdjustedType extends AdjustedTypeBase {

    // Data declarations are generated from the wire schema in AdjustedTypeBase.

    public AdjustedType(DataStore data, Collection<? extends ClavaNode> children, AdjustedTypeBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public AdjustedType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Type getOriginalType() {
        return getWireOriginalType();
    }

    public void setOriginalType(Type originalType) {
        set(ORIGINAL_TYPE, originalType);
    }

    public Type getAdjustedType() {
        return getWireAdjustedType();
    }

    @Override
    protected List<DataKey<Type>> getUnderlyingTypeKeys() {
        return Arrays.asList(ORIGINAL_TYPE);
    }

}
