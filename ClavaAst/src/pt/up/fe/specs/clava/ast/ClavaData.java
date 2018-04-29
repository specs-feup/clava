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

import org.suikasoft.jOptions.Interfaces.DataStore;

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
 * @deprecated
 */
@Deprecated
public class ClavaData {

    // ONGOING: Experiment to check if this object can replace all ClavaData instances
    private DataStore data;

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
    // public ClavaData(String id, SourceRange location, boolean isMacro, SourceRange spellingLocation) {
    // this(id, location, isMacro, spellingLocation, Collections.emptyList());
    // }

    // public static ClavaData newInstance(String id) {
    // return new ClavaData(id, SourceRange.invalidRange(), false, SourceRange.invalidRange(),
    // Collections.emptyList());
    // }

    public ClavaData(String id, SourceRange location, boolean isMacro, SourceRange spellingLocation,
            List<InlineComment> inlineComments) {

        this.id = id;
        this.location = location;
        this.isMacro = isMacro;
        this.spellingLocation = spellingLocation;

        // Optional
        this.inlineComments = new ArrayList<>(inlineComments);
    }

    public ClavaData(ClavaData data) {
        // this(data.id, data.location, data.isMacro, data.spellingLocation, data.inlineComments);
        setData(data);
    }

    public ClavaData() {
        this(null, SourceRange.invalidRange(), false, SourceRange.invalidRange(), Collections.emptyList());
    }

    public ClavaData setData(ClavaData data) {
        this.id = data.id;
        this.location = data.location;
        this.isMacro = data.isMacro;
        this.spellingLocation = data.spellingLocation;
        this.inlineComments = data.inlineComments;

        this.data = data.data;

        return this;
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

    public DataStore getData() {
        return data;
    }

    public void setData(DataStore data) {
        this.data = data;
    }
}
