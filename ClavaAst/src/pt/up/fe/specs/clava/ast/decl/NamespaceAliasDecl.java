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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class NamespaceAliasDecl extends NamedDecl {

    /// DATAKEYS BEGIN

    public final static DataKey<String> NESTED_PREFIX = KeyFactory.string("nestedPrefix");

    public final static DataKey<NamespaceDecl> ALIASED_NAMESPACE = KeyFactory.object("aliasedNamespace",
            NamespaceDecl.class);

    /// DATAKEYS END

    public NamespaceAliasDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final String nestedPrefix;
    // private final DeclRef declInfo;
    //
    // public NamespaceAliasDecl(String nestedPrefix, DeclRef declInfo, String declName, DeclData declData,
    // ClavaNodeInfo info) {
    // super(declName, null, declData, info, Collections.emptyList());
    //
    // this.declInfo = declInfo;
    // this.nestedPrefix = nestedPrefix;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new NamespaceAliasDecl(nestedPrefix, declInfo, getDeclName(), getDeclData(), getInfo());
    // }

    // public DeclRef getAliasedNamespaceRef() {
    // return get(ALIASED_NAMESPACE);
    // // return declInfo;
    // }

    public NamespaceDecl getAliasedNamespace() {
        return get(ALIASED_NAMESPACE);
        // return getApp().getNodeTry(declInfo.getDeclId())
        // .map(node -> (NamespaceDecl) node);
    }

    @Override
    public String getCode() {
        // System.out.println("DECL NAME:" + get(DECL_NAME));
        // System.out.println("QUALIFIED NAME:" + get(QUALIFIED_NAME));
        // System.out.println("aLIASED DECL NAME:" + get(ALIASED_NAMESPACE).get(DECL_NAME));
        // System.out.println("ID:" + get(PREVIOUS_ID));
        // System.out.println("NESTED PREFIX:" + get(NESTED_PREFIX));
        return "namespace " + get(DECL_NAME) + " = " + get(NESTED_PREFIX) + get(ALIASED_NAMESPACE).get(DECL_NAME);
        // return "namespace " + getDeclName() + " = " + nestedPrefix + declInfo.getDeclType();
    }
}
