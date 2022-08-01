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

package pt.up.fe.specs.clava.utils;

import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.AdjustedType;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Interface for ClavaNodes that have an associated type.
 * 
 * @author JoaoBispo
 *
 */
public interface Typable {

    /**
     * If the type was adjusted, the field contains the adjusted type
     */
    final static DataKey<Optional<AdjustedType>> ADJUSTED_TYPE = KeyFactory
            .optional("adjustedType");

    Type getType();

    void setType(Type type);

    default String getTypeCode() {
        return getType().getCode((ClavaNode) this, null);
    }

    default String getTypeCode(String name) {
        return getType().getCode((ClavaNode) this, name);
    }

    Optional<AdjustedType> getAdjustedType();

    void setAdjustedType(AdjustedType type);

    // <K> K get(DataKey<K> key);
    //
    // <K, E extends K, T extends DataClass<T>> T set(DataKey<K> key, E value);
    //
    // <VT> boolean hasValue(DataKey<VT> key);

    // default Optional<AdjustedType> getAdjustedType() {
    // if (!hasValue(ADJUSTED_TYPE)) {
    // return Optional.empty();
    // }
    // return get(ADJUSTED_TYPE);
    // }
    //
    // default void setAdjustedType(AdjustedType type) {
    // set(ADJUSTED_TYPE, Optional.of(type));
    // }

}
