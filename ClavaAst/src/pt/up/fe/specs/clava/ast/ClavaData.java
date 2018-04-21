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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.comment.InlineComment;

/**
 * Represents the data of a ClavaNode.
 * 
 * <p>
 * ClavaData instances are mutable for convenience of ClavaNodes, however they should only be modified by the ClavaNodes
 * themselves.
 * 
 * <p>
 * While in transition between ClavaData nodes and Legacy nodes, ClavaData nodes should not be directly accessed (e.g.,
 * .getData()). To maintain compatibility, implement getters in the ClavaNode instances.
 * 
 * <p>
 * [closed, ClavaData instances can have ClavaNodes] It is yet open for discussion if ClavaData instances should have
 * ClavaNodes (e.g., ExprData has a Type, DeclData has Attribute nodes), or if they all should be children).
 * 
 * @author JoaoBispo
 *
 */
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

    /*
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
    // }

    public static ClavaData empty() {
        return new ClavaData(null, SourceRange.invalidRange(), false, SourceRange.invalidRange());
    }

    public static ClavaData newInstance(SourceRange location) {
        return new ClavaData(null, location, false, location);
    }

    private String id;
    private SourceRange location;
    private boolean isMacro;
    private SourceRange spellingLocation;

    // Optional
    private List<InlineComment> inlineComments;

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
        this(id, location, isMacro, spellingLocation, Collections.emptyList());
    }

    private ClavaData(String id, SourceRange location, boolean isMacro, SourceRange spellingLocation,
            List<InlineComment> inlineComments) {

        this.id = id;
        this.location = location;
        this.isMacro = isMacro;
        this.spellingLocation = spellingLocation;

        // Optional
        this.inlineComments = new ArrayList<>(inlineComments);
    }

    public ClavaData(ClavaData data) {
        this(data.id, data.location, data.isMacro, data.spellingLocation, data.inlineComments);
    }

    public void addInlineComment(InlineComment inlineComment) {
        Preconditions.checkArgument(!inlineComment.isStmtComment(),
                "InlineComment must not be a statement comment:" + inlineComment);

        inlineComments.add(inlineComment);
    }

    public List<InlineComment> getInlineComments() {
        return inlineComments;
    }

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

    public boolean isMacro() {
        return isMacro;
    }

    public SourceRange getSpellingLocation() {
        return spellingLocation;
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
     * Sets the id of the current instance.
     * 
     * @param newId
     * @return
     */
    // public ClavaData setId(String newId) {
    public void setId(String newId) {
        this.id = newId;
        // return this;
        // ClavaData copy = ClavaData.copy(this);
        // copy.id = newId;
        //
        // return copy;
    }

    public ClavaData setLocation(SourceRange location) {
        this.location = location;
        return this;
    }

    public ClavaData setMacro(boolean isMacro) {
        this.isMacro = isMacro;
        return this;
    }

    public ClavaData setSpellingLocation(SourceRange spellingLocation) {
        this.spellingLocation = spellingLocation;
        return this;
    }

    public ClavaData setInlineComments(List<InlineComment> inlineComments) {
        this.inlineComments = inlineComments;
        return this;
    }

}
