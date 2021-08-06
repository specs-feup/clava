/**
 * Copyright 2018 SPeCS.
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

/**
 * (From Clang documentation)
 * <p>
 * Represents a pack expansion of types.
 * <p>
 * Pack expansions are part of C++11 variadic templates. A pack expansion contains a pattern, which itself contains one
 * or more "unexpanded" parameter packs. When instantiated, a pack expansion produces a series of types, each
 * instantiated from the pattern of the expansion, where the Ith instantiation of the pattern uses the Ith arguments
 * bound to each of the unexpanded parameter packs. The pack expansion is considered to "expand" these unexpanded
 * parameter packs.
 * 
 * @author JoaoBispo
 *
 */
public class PackExpansionType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<Integer> NUM_EXPANSIONS = KeyFactory.integer("numExpansions");

    public final static DataKey<Type> PATTERN = KeyFactory.object("pattern", Type.class);

    /// DATAKEYS END

    public PackExpansionType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<Type> getPattern() {
        return Optional.of(get(PATTERN));
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        // According to Clang parser, '...' must immediately precede declared identifier
        String packName = name == null ? "..." : "... " + name;

        return getPattern().map(type -> type.getCode(sourceNode, packName)).orElse("");
    }
}
