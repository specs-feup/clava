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
import pt.up.fe.specs.clava.ast.decl.data.nestedname.NestedNameSpecifier;

/**
 * A C++ using-declaration.
 * 
 * @author JoaoBispo
 *
 */
public class UsingDecl extends NamedDecl {

    /// DATAKEYS BEGIN

    public final static DataKey<NestedNameSpecifier> NESTED_NAME_SPECIFIER = KeyFactory.object("nestedNameSpecifier",
            NestedNameSpecifier.class);

    /// DATAKEYS END

    public UsingDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return "using " + get(NESTED_NAME_SPECIFIER).getQualifier() + getDeclName() + ";";
    }

}
