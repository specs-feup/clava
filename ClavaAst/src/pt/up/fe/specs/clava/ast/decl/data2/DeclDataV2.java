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

package pt.up.fe.specs.clava.ast.decl.data2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.ClavaDataPostProcessing;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.util.SpecsCheck;

public class DeclDataV2 extends ClavaData {

    public static DeclDataV2 empty() {
        return new DeclDataV2(false, false, false, false, Collections.emptyList(), ClavaData.empty());
    }

    private final boolean isImplicit;
    private final boolean isUsed;
    private final boolean isReferenced;
    private final boolean isInvalidDecl;
    private final List<String> attributesIds;
    private List<Attribute> attributes;

    public DeclDataV2(boolean isImplicit, boolean isUsed, boolean isReferenced,
            boolean isInvalidDecl, List<String> attributesIds, ClavaData clavaData) {

        super(clavaData);

        this.isImplicit = isImplicit;
        this.isUsed = isUsed;
        this.isReferenced = isReferenced;
        this.isInvalidDecl = isInvalidDecl;
        this.attributesIds = new ArrayList<>(attributesIds);
        this.attributes = null;
    }

    public DeclDataV2(DeclDataV2 data) {
        this(data.isImplicit, data.isUsed, data.isReferenced, data.isInvalidDecl, data.attributesIds, data);
    }

    @Override
    protected void postProcess(ClavaDataPostProcessing data) {
        // Call super
        super.postProcess(data);

        List<Attribute> attributes = new ArrayList<>(attributesIds.size());

        for (String attrId : attributesIds) {
            SpecsCheck.checkNotNull(attrId,
                    () -> "Expected '" + attrId + "' in node '" + getId() + "' to be non-null");

            attributes.add(data.getAttr(attrId));
        }

        this.attributes = attributes;
    }

    public boolean isImplicit() {
        return isImplicit;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public boolean isReferenced() {
        return isReferenced;
    }

    public boolean isInvalidDecl() {
        return isInvalidDecl;
    }

    public List<String> getAttributesIds() {
        return attributesIds;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append("is implicit: " + isImplicit);
        builder.append(", is used: " + isUsed);
        builder.append(", is referenced: " + isReferenced);
        builder.append(", is invalid decl: " + isInvalidDecl);
        builder.append(", attributes: " + attributesIds);

        return toString(super.toString(), builder.toString());
    }
}
