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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.attr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.enums.AttributeKind;

public class Attribute extends AttributeBase {

    // Data declarations are generated from the wire schema in AttributeBase.

    public Attribute(DataStore data, Collection<? extends ClavaNode> children, AttributeBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public Attribute(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public AttributeKind getKind() {
        return get(Attribute.KIND);
    }

    public String getAttributeCode(String arguments) {
        String argsCode = arguments == null || arguments.isEmpty() ? "" : "(" + arguments + ")";
        return "__attribute__((" + getWireKind().getAttributeName() + argsCode + "))";
    }

    /**
     * 
     * @return true if the attribute should appear after declarations (e.g., aligned). By default returns false.
     */
    public boolean isPostAttr() {
        return false;
    }

    /**
     * By default, uses the code string defined in the field KIND.
     */
    @Override
    public String getCode() {
        return getAttributeCode(getArgumentsCode());
    }

    /**
     * A string representing the code of the arguments, or null if it has no arguments. By default returns null.
     * 
     * @return
     */
    public String getArgumentsCode() {
        return null;
    }
}
