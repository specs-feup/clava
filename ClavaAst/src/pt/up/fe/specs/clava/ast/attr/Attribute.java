/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.ast.attr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.attr.data.AttributeData;
import pt.up.fe.specs.clava.ast.attr.data.AttributeI;
import pt.up.fe.specs.clava.ast.attr.enums.AttributeKind;
import pt.up.fe.specs.clava.ast.attr.legacy.AttrData;

public abstract class Attribute extends ClavaNode {

    private final AttributeKind kind;
    private final AttrData attrData;

    public Attribute(AttributeData data, Collection<? extends ClavaNode> children) {
        super(data, children);

        this.kind = null;
        this.attrData = null;
    }

    public Attribute(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        this.kind = null;
        this.attrData = null;
    }

    @Override
    public AttributeData getData() {
        return (AttributeData) super.getData();
    }

    /**
     * Legacy support.
     * 
     * @param kind
     * @param attrData
     * @param nodeInfo
     * @param children
     */
    public Attribute(AttributeKind kind, AttrData attrData, ClavaNodeInfo nodeInfo,
            Collection<? extends ClavaNode> children) {
        super(nodeInfo, children);

        this.kind = kind;
        this.attrData = attrData;
    }

    public AttrData getAttrData() {
        if (hasData() || hasDataI()) {
            throw new RuntimeException("Not implemented for ClavaData and DataStore nodes");
        }
        return attrData;
    }

    public AttributeKind getKind() {
        if (hasDataI()) {
            return getDataI().get(AttributeI.KIND);
        }

        if (hasData()) {
            return getData().getKind();
        }
        return kind;
    }

    protected String getAttributeCode(String attrValue) {
        return "__attribute__ ((" + attrValue + "))";
    }

    /**
     * 
     * @return true if the attribute should appear after declarations (e.g., aligned). By default returns false.
     */
    public boolean isPostAttr() {
        return false;
    }
}
