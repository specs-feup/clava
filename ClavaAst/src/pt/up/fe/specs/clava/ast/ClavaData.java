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

import pt.up.fe.specs.clava.SourceRange;

public class ClavaData {

    public static ClavaData empty() {
        return new ClavaData(null, SourceRange.invalidRange());
    }

    private final String id;
    private final SourceRange location;

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
    public ClavaData(String id, SourceRange location) {
        this.id = id;
        this.location = location;
    }

    public ClavaData(ClavaData data) {
        this(data.id, data.location);
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
    public ClavaData copy() {
        return new ClavaData(this);
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

    @Override
    public String toString() {
        // ClavaData is top-level

        StringBuilder builder = new StringBuilder();

        builder.append("id: " + id);
        builder.append(", location: " + location.isValid());

        return toString("", builder.toString());
    }

}
