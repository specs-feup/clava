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

package pt.up.fe.specs.clava.ast.attr.data;

import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.attr.enums.AttributeKind;

public class AttributeData extends ClavaData {

    /*
    public static AttributeData empty(ClavaData data) {
        if (data instanceof AttributeData) {
            return (AttributeData) data;
        }
    
        return new AttributeData(null, false, false, false, false, data);
    }
    */

    private AttributeKind kind;
    private boolean isImplicit;
    private boolean isInherited;
    private boolean isLateParsed;
    private boolean isPackExpansion;

    /**
     * Full constructor.
     * 
     * @param kind
     * @param isImplicit
     * @param isInherited
     * @param isLateParsed
     * @param isPackExpansion
     * @param data
     */
    public AttributeData(AttributeKind kind, boolean isImplicit, boolean isInherited, boolean isLateParsed,
            boolean isPackExpansion, ClavaData data) {

        super(data);

        this.kind = kind;
        this.isImplicit = isImplicit;
        this.isInherited = isInherited;
        this.isLateParsed = isLateParsed;
        this.isPackExpansion = isPackExpansion;
    }

    /**
     * Copy constructor.
     * 
     * @param data
     */
    public AttributeData(AttributeData data) {
        this(data.kind, data.isImplicit, data.isInherited, data.isLateParsed, data.isPackExpansion, data);
    }

    /**
     * Empty constructor.
     */
    public AttributeData() {
        this(null, false, false, false, false, new ClavaData());
    }

    public AttributeData setData(AttributeData data) {
        this.kind = data.kind;
        this.isImplicit = data.isImplicit;
        this.isInherited = data.isInherited;
        this.isLateParsed = data.isLateParsed;
        this.isPackExpansion = data.isPackExpansion;

        return this;
    }

    public AttributeKind getKind() {
        return kind;
    }

    public boolean isImplicit() {
        return isImplicit;
    }

    public boolean isInherited() {
        return isInherited;
    }

    public boolean isLateParsed() {
        return isLateParsed;
    }

    public boolean isPackExpansion() {
        return isPackExpansion;
    }

    @Override
    public String toString() {
        return toString(super.toString(), "kind=" + kind + ", isImplicit=" + isImplicit + ", isInherited=" + isInherited
                + ", isLateParsed=" + isLateParsed + ", isPackExpansion=" + isPackExpansion);
    }

}
