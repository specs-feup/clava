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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class NamespaceAliasDecl extends NamespaceAliasDeclBase {

    // Data declarations are generated from the wire schema in NamespaceAliasDeclBase.

    public NamespaceAliasDecl(DataStore data, Collection<? extends ClavaNode> children, NamespaceAliasDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public NamespaceAliasDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public NamespaceDecl getAliasedNamespace() {
        return getWireAliasedNamespace();
    }

    @Override
    public String getCode() {
        // System.out.println("DECL NAME:" + getWireDeclName());
        // System.out.println("QUALIFIED NAME:" + get(QUALIFIED_NAME));
        // System.out.println("aLIASED DECL NAME:" + getWireAliasedNamespace().get(DECL_NAME));
        // System.out.println("ID:" + get(PREVIOUS_ID));
        // System.out.println("NESTED PREFIX:" + getWireNestedPrefix());
        return "namespace " + getWireDeclName() + " = " + getWireNestedPrefix() + getWireAliasedNamespace().get(DECL_NAME);
    }
}
