/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.ast.decl.data;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.utils.Typable;

public class CXXBaseSpecifier extends ADataClass<CXXBaseSpecifier> implements Typable {

    /// DATAKEYS BEGIN

    /**
     * True if this is a virtual base class.
     */
    public final static DataKey<Boolean> IS_VIRTUAL = KeyFactory.bool("isVirtual");

    /**
     * True if this base specifier is a pack expansion.
     */
    public final static DataKey<Boolean> IS_PACK_EXPANSION = KeyFactory.bool("isPackExpansion");

    /**
     * The access specifier as written in the source code.
     */
    public final static DataKey<AccessSpecifier> ACCESS_SPECIFIER_AS_WRITTEN = KeyFactory
            .enumeration("accessSpecifierAsWritten", AccessSpecifier.class);

    /**
     * The actual base specifier, after semantic analysis.
     */
    public final static DataKey<AccessSpecifier> ACCESS_SPECIFIER_SEMANTIC = KeyFactory
            .enumeration("accessSpecifierSemantic", AccessSpecifier.class);

    /**
     * The type of the base class.
     */
    public final static DataKey<Type> TYPE = KeyFactory.object("type", Type.class);

    /// DATAKEYS END

    @Override
    public Type getType() {
        return get(TYPE);
    }

    @Override
    public void setType(Type type) {
        set(TYPE, type);
    }

    /**
     * 
     * @return
     */
    public String getCode(ClavaNode sourceNode) {
        StringBuilder code = new StringBuilder();

        // Add access specifier
        code.append(get(ACCESS_SPECIFIER_AS_WRITTEN).getString());

        // Add virtual, if present
        if (get(IS_VIRTUAL)) {
            code.append(" virtual");
        }

        // Add type
        code.append(" ").append(get(TYPE).getCode(sourceNode));

        if (get(IS_PACK_EXPANSION)) {
            code.append("...");
        }

        return code.toString();
    }
}
