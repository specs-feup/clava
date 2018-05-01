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

    // public ClavaNode newClavaNode(Class<? extends ClavaNode> clavaNodeClass, ClavaData clavaData,
    // Collection<? extends ClavaNode> children) {
    public <T extends ClavaNode> T newClavaNode(Class<T> clavaNodeClass, ClavaData clavaData,
            Collection<? extends ClavaNode> children) {

        // // Get required ClavaData class
        // Class<? extends ClavaData> clavaDataClass = ClavaNodeToData.getClavaDataClass(clavaNodeClass);
        //
        // // Verify in given ClavaData is compatible with the expected Data for the ClavaNode
        // if (!clavaDataClass.isInstance(clavaData)) {
        // throw new RuntimeException("Given ClavaData '" + clavaData.getClass().getSimpleName()
        // + "' is not compatible with ClavaNode '" + clavaNodeClass.getSimpleName() + "', requires a '"
        // + clavaDataClass.getSimpleName() + "'");
        // }

        // TODO: replace with Java 10 var
        BiFunction<ClavaData, Collection<? extends ClavaNode>, ClavaNode> constructor = constructorsCache
                .get(clavaNodeClass);

        // Check if constructor not built yet
        if (constructor == null) {
            try {

                Constructor<? extends ClavaNode> constructorMethod = clavaNodeClass.getConstructor(clavaData.getClass(),
                        Collection.class);
                // Constructor<? extends ClavaNode> constructorMethod = clavaNodeClass.getConstructor(ClavaData.class,
                // Collection.class);
                /*
                if (clavaNodeClass.equals(UnsupportedNode.class)) {
                    constructorMethod = clavaNodeClass.getConstructor(ClavaData.class,
                            Collection.class);
                } else {
                    constructorMethod = clavaNodeClass.getConstructor(clavaData.getClass(),
                            Collection.class);
                }
                
                Constructor<? extends ClavaNode> constructorMethod2 = constructorMethod;
                */
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

        return clavaNodeClass.cast(constructor.apply(clavaData, children));
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
    /*
    public <T extends ClavaNode> T newClavaNode(Class<T> clavaNodeClass, DataStore data,
            Collection<? extends ClavaNode> children) {
    
        try {
    
            Constructor<? extends ClavaNode> constructorMethod = clavaNodeClass.getConstructor(DataStore.class,
                    Collection.class);
    
            try {
                return clavaNodeClass.cast(constructorMethod.newInstance(data, children));
            } catch (Exception e) {
                throw new RuntimeException("Could not call constructor for ClavaNode", e);
            }
    
        } catch (Exception e) {
            throw new RuntimeException("Could not create constructor for ClavaNode:" + e.getMessage());
        }
    
    }
    
    */
}
