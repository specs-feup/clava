/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AAttribute;

public class CxxAttribute<Self extends CxxAttribute<Self>> extends AAttribute<Self> {

    public CxxAttribute(Attribute attr, CxxWeaver weaver) {
        super(attr, weaver);
    }

    @Override
    public Attribute getNodeImpl() {
        return (Attribute) super.getNodeImpl();
    }

    @Override
    public String getKindImpl() {
        var attrName = this.getNodeImpl().getKind().name();

        if (attrName.endsWith("Attr")) {
            attrName = attrName.substring(0, attrName.length() - "Attr".length());
        }

        return attrName;
    }

}
