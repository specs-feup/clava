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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.attr;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.enums.FormatAttrKind;

public class FormatAttr extends InheritableAttr {
    /// DATAKEYS BEGIN

    public final static DataKey<FormatAttrKind> TYPE = KeyFactory.enumeration("type", FormatAttrKind.class);
    public final static DataKey<Integer> FORMAT_INDEX = KeyFactory.integer("formatIndex");
    public final static DataKey<Integer> FIRST_ARG = KeyFactory.integer("firstArg");

    /// DATAKEYS END

    public FormatAttr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getArgumentsCode() {
        return get(TYPE) + ", " + get(FORMAT_INDEX) + ", " + get(FIRST_ARG);
    }

    // @Override
    // public String getCode() {
    // // System.out.println("TYPE:" + get(TYPE));
    // // return getAttributeCode("__format__(__" + get(TYPE) + "__, " + get(FORMAT_INDEX) + ", " + get(FIRST_ARG) +
    // // ")");
    // return getAttributeCode("__format__(" + get(TYPE) + ", " + get(FORMAT_INDEX) + ", " + get(FIRST_ARG) + ")");
    // }
}
