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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.decl.data.nestedname;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.decl.enums.NestedNameSpecifierKind;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public abstract class NestedNameSpecifier extends ADataClass<NestedNameSpecifier> {

    public final static DataKey<NestedNameSpecifierKind> KIND = KeyFactory.enumeration("qualifier",
            NestedNameSpecifierKind.class);

    public NestedNameSpecifier(NestedNameSpecifierKind kind) {
        set(KIND, kind);
    }

    public String getQualifier() {
        throw new NotImplementedException(get(KIND));
    }

    public static NestedNameSpecifier newInstance(NestedNameSpecifierKind kind) {
        switch (kind) {
        case Namespace:
            return new NamespaceSpecifier();
        case NamespaceAlias:
            return new NamespaceAliasSpecifier();
        case TypeSpec:
            return new TypeSpecSpecifier();
        case TypeSpecWithTemplate:
            return new TypeSpecWithTemplateSpecifier();
        case Global:
            return new GlobalSpecifier();
        case Super:
            return new SuperSpecifier();
        default:
            throw new NotImplementedException(kind);
        }
    }
}
