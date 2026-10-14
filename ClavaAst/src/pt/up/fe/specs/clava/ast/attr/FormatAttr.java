/**
 * Copyright 2019 SPeCS.
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
import pt.up.fe.specs.clava.ast.attr.enums.FormatAttrKind;

public class FormatAttr extends FormatAttrBase {

    // Data declarations are generated from the wire schema in FormatAttrBase.

    public FormatAttr(DataStore data, Collection<? extends ClavaNode> children, FormatAttrBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public FormatAttr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getArgumentsCode() {
        return getWireType() + ", " + getWireFormatIndex() + ", " + getWireFirstArg();
    }

    // @Override
    // public String getCode() {
    // // System.out.println("TYPE:" + getWireType());
    // // return getAttributeCode("__format__(__" + getWireType() + "__, " + getWireFormatIndex() + ", " + getWireFirstArg() +
    // // ")");
    // return getAttributeCode("__format__(" + getWireType() + ", " + getWireFormatIndex() + ", " + getWireFirstArg() + ")");
    // }
}
