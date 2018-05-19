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
import java.util.Collections;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.SpecsCollections;

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

    private final int numExpansions;

    public PackExpansionType(int numExpansions, TypeData data, ClavaNodeInfo info,
            Type pattern) {

        this(numExpansions, data, info, SpecsCollections.ofNullable(pattern));
    }

    private PackExpansionType(int numExpansions, TypeData data, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(data, info, children);

        this.numExpansions = numExpansions;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new PackExpansionType(numExpansions, getTypeData(), getInfo(), Collections.emptyList());
    }

    public Optional<Type> getPattern() {
        if (!hasChildren()) {
            return Optional.empty();
        }

        return Optional.of(getChild(Type.class, 0));
    }

    @Override
    public String getCode(String name) {
        // According to Clang parser, '...' must immediately precede declared identifier
        String packName = name == null ? "..." : "... " + name;
        // return getPattern().map(type -> type.getCode(name)).orElse("") + "...";
        return getPattern().map(type -> type.getCode(packName)).orElse("");
    }
}
