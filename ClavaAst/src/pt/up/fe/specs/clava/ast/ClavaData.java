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

import pt.up.fe.specs.clava.SourceRange;

public class ClavaData {

    // private static final Map<Class<? extends ClavaData>, Supplier<ClavaData>> DATA_CONSTRUCTORS_CACHE = new
    // HashMap<>();

    // private static <T extends ClavaData> T newCopyConstructor(T data) {
    // try {
    // return constructorMethod.newInstance(data);
    // } catch (Exception e) {
    // throw new RuntimeException(
    // "Could not call constructor for ClavaNode '" + data.getClass().getSimpleName() + "'", e);
    // }
    // }

    public static <T extends ClavaData> T copy(T clavaData) {

        // Get ClavaData class
        @SuppressWarnings("unchecked")
        Class<T> clavaDataClass = (Class<T>) clavaData.getClass();

        Constructor<T> constructorMethod = null;
        try {
            // Create copy constructor: new T(T data)
            constructorMethod = clavaDataClass.getConstructor(clavaDataClass);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not create call to copy constructor for ClavaData '" + clavaDataClass.getSimpleName()
                            + "'. Check if class contains a constructor of the form 'new T(T data)'.",
                    e);
        }

        // Invoke constructor
        try {
            return constructorMethod.newInstance(clavaData);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not call constructor for ClavaNode '" + clavaData.getClass().getSimpleName() + "'", e);
        }

        /*
        
        
        // Get required ClavaData class
        Class<? extends ClavaData> clavaDataClass = ClavaNodeToData.getClavaDataClass(clavaNodeClass);
        
        // Verify in given ClavaData is compatible with the expected Data for the ClavaNode
        if (!clavaDataClass.isInstance(clavaData)) {
            throw new RuntimeException("Given ClavaData '" + clavaData.getClass().getSimpleName()
                    + "' is not compatible with ClavaNode '" + clavaNodeClass.getSimpleName() + "', requires a '"
                    + clavaDataClass.getSimpleName() + "'");
        }
        
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
        */
    }

    public static ClavaData empty() {
        return new ClavaData(null, SourceRange.invalidRange(), false, SourceRange.invalidRange());
    }

    private String id;
    private final SourceRange location;
    private final boolean isMacro;
    private final SourceRange spellingLocation;

    /**
     * @deprecated
     */
    // @Deprecated
    // public ClavaData() {
    // id = null;
    // location = null;
    // // SpecsLogs.msgWarn("Method is deprecated, use constructor that receives id and location");
    // }

    /**
     * 
     * @param id
     * @param location
     */
    public ClavaData(String id, SourceRange location, boolean isMacro, SourceRange spellingLocation) {
        this.id = id;
        this.location = location;
        this.isMacro = isMacro;
        this.spellingLocation = spellingLocation;
    }

    public ClavaData(ClavaData data) {
        this(data.id, data.location, data.isMacro, data.spellingLocation);
    }

    /**
     * Makes a deep copy of this object.
     * 
     * <p>
     * Implementation should use the copy constructor, that way what should be copied and what should be reused is
     * delegated to the constructor.
     * 
     * @return
     */
    // public ClavaData copy() {
    // return copy(this);
    // // return new ClavaData(this);
    // }

    protected String toString(String superToString, String thisToString) {

        // Use bridge if there is content and a suffix
        String bridge = !thisToString.isEmpty() && !superToString.isEmpty() ? ", " : "";

        return superToString + bridge + thisToString;
    }

    public String getId() {
        return id;
    }

    public SourceRange getLocation() {
        return location;
    }

    @Override
    public String toString() {
        // ClavaData is top-level

        StringBuilder builder = new StringBuilder();

        builder.append("id: " + id);
        builder.append(", location: " + location.isValid());
        builder.append(", isMacro: " + isMacro);
        if (isMacro) {
            builder.append(", spellingLoc: " + spellingLocation);
        }

        return toString("", builder.toString());
    }

    /**
     * Package-level method that can be used for any post-processing the node might need.
     * 
     * @param data
     */
    protected void postProcess(ClavaDataPostProcessing data) {
        // By default, do nothing
    }

    public ClavaData setId(String newId) {
        ClavaData copy = ClavaData.copy(this);
        copy.id = newId;

        return copy;
    }

}
