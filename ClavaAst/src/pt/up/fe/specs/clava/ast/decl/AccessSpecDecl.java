/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.language.AccessSpecifier;

/**
 * Represents an access specifier followed by colon ':'.
 * 
 * @author JBispo
 *
 */
public class AccessSpecDecl extends AccessSpecDeclBase {

    // Data declarations are generated from the wire schema in AccessSpecDeclBase.

    public AccessSpecDecl(DataStore data, Collection<? extends ClavaNode> children, AccessSpecDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public AccessSpecDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public AccessSpecifier getAccessSpecifier() {
        return getWireAccessSpecifier();
    }

    @Override
    public String getCode() {
        return ln() + ln() + getAccessSpecifier().getString() + ":";
    }

}
