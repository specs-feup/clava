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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.SourceRange;

public class UnresolvedUsingValueDecl extends ValueDecl {

    public static final DataKey<String> QUALIFIER = KeyFactory.string("qualifier");

    public static final DataKey<Boolean> IS_PACK_EXPANSION = KeyFactory.bool("isPackExpansion");

    public static final DataKey<Optional<SourceRange>> QUALIFIER_SOURCE_RANGE =
            KeyFactory.optional("qualifierSourceRange");

    public static final DataKey<Optional<SourceLocation>> USING_LOCATION = KeyFactory.optional("usingLocation");

    public static final DataKey<Optional<SourceLocation>> ELLIPSIS_LOCATION = KeyFactory.optional("ellipsisLocation");

    public static final DataKey<Optional<SourceLocation>> NAME_LOCATION = KeyFactory.optional("nameLocation");

    public UnresolvedUsingValueDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();
        if (get(USING_LOCATION).filter(SourceLocation::isValid).isPresent()) {
            code.append("using ");
        }
        code.append(get(QUALIFIER)).append(getDeclName());
        if (get(IS_PACK_EXPANSION)) {
            code.append("...");
        }
        return code.append(';').toString();
    }

}
