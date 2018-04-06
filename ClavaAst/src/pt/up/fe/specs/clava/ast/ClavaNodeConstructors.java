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

package pt.up.fe.specs.clava.ast;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import pt.up.fe.specs.clava.ClavaNode;

public class ClavaNodeConstructors {

    private final Map<Class<? extends ClavaNode>, BiFunction<ClavaData, Collection<? extends ClavaNode>, ClavaNode>> constructorsCache;

    public ClavaNodeConstructors() {
        this.constructorsCache = new HashMap<>();
    }

    public ClavaNode newClavaNode(Class<? extends ClavaNode> clavaNodeClass, ClavaData clavaData,
            Collection<? extends ClavaNode> children) {

        // TODO:
        // Verify in given ClavaData is assignable from the expected Data for the ClavaNode

        // TODO: replace with Java 10 var
        BiFunction<ClavaData, Collection<? extends ClavaNode>, ClavaNode> constructor = constructorsCache
                .get(clavaNodeClass);

        // Check if constructor not built yet
        if (constructor == null) {
            try {
                Constructor<? extends ClavaNode> constructorMethod = clavaNodeClass.getConstructor(clavaData.getClass(),
                        Collection.class);

                constructor = (data, childrenNodes) -> {
                    try {
                        return constructorMethod.newInstance(data, childrenNodes);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not call constructor for ClavaNode", e);
                    }
                };

                // Save constructor
                constructorsCache.put(clavaNodeClass, constructor);
            } catch (Exception e) {
                throw new RuntimeException("Could not create constructor for ClavaNode:" + e.getMessage());
                // SpecsLogs.msgLib("Could not create constructor for ClavaNode:" + e.getMessage());
                // return null;
            }

        }

        return constructor.apply(clavaData, children);
    }

    // Constructor<? extends ClavaNode> constructor = clavaNodeClass.getConstructor(clavaDataClass,
    // Collection.class);builder=(node,children)->
    // {
    // try {
    // return constructor.newInstance(node, children);
    // } catch (Exception e) {
    // throw new RuntimeException("Could not call constructor for ClavaNode", e);
    // }
    // };
}
