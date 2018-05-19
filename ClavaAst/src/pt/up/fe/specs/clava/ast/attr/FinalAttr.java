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

import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.attr.enums.AttributeKind;
import pt.up.fe.specs.clava.ast.attr.legacy.AttrData;

public class FinalAttr extends InheritableAttr {

    public FinalAttr(AttrData attrData, ClavaNodeInfo nodeInfo) {
        super(AttributeKind.Final, attrData, nodeInfo, Collections.emptyList());
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new FinalAttr(getAttrData(), getInfo());
    }

    @Override
    public String getCode() {
        return "final";
    }

}
