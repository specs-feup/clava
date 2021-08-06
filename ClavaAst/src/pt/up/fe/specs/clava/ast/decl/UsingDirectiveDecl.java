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

/**
 * A C++ using-directive.
 * 
 * @author JoaoBispo
 *
 */
public class UsingDirectiveDecl extends NamedDecl {

    /// DATAKEYS BEGIN

    public final static DataKey<String> QUALIFIER = KeyFactory.string("qualifier");

    public final static DataKey<NamespaceDecl> NAMESPACE = KeyFactory.object("namespace", NamespaceDecl.class);

    public final static DataKey<NamedDecl> NAMESPACE_AS_WRITTEN = KeyFactory.object("namespaceAsWritten",
            NamedDecl.class);

    /// DATAKEYS END

    public UsingDirectiveDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return get(QUALIFIER);
    }
}
