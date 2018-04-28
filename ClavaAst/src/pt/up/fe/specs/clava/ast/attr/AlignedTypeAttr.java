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

package pt.up.fe.specs.clava.ast.attr;

import static pt.up.fe.specs.clava.ast.attr.data.AlignedTypeAttrI.*;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class AlignedTypeAttr extends Attribute {

    public AlignedTypeAttr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public AlignedTypeAttr(AlignedTypeAttrData data, Collection<? extends ClavaNode> children) {
    // super(data, children);
    // }
    //
    // @Override
    // public AlignedTypeAttrData getData() {
    // return (AlignedTypeAttrData) super.getData();
    // }

    @Override
    public String getCode() {
        String typeCode = getDataI().get(TYPE).getCode();
        String attrValue = "aligned (" + typeCode + ")";

        return getAttributeCode(attrValue);
    }

    @Override
    public boolean isPostAttr() {
        return true;
    }
}
