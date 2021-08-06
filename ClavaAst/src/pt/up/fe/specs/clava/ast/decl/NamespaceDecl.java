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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class NamespaceDecl extends NamedDecl {

    /// DATAKEYS BEGIN

    public final static DataKey<String> SOURCE_LITERAL = KeyFactory.string("sourceLiteral");

    /// DATAKEYS END

    public NamespaceDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public String getCompleteNamespace() {
        // If it has no namespace ancestor, found top-level namespace
        Optional<NamespaceDecl> ancestorNamespace = getAncestorTry(NamespaceDecl.class);
        if (!ancestorNamespace.isPresent()) {
            return getDeclName();
        }

        // Return complete namespace of ancestor suffixed with current namespace
        return ancestorNamespace.get().getCompleteNamespace() + "::" + getDeclName();
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(ln() + "namespace ").append(getDeclName()).append(" {" + ln());

        String declsCode = getChildrenStream()
                .map(child -> child.getCode())
                .collect(Collectors.joining(ln()));

        code.append(indentCode(declsCode)).append(ln());
        code.append("}" + ln());

        return code.toString();
    }

}
