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

import java.util.Collections;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;

public class NamespaceAliasDecl extends NamedDecl {

    private final String nestedPrefix;
    private final DeclRef declInfo;

    public NamespaceAliasDecl(String nestedPrefix, DeclRef declInfo, String declName, DeclData declData,
            ClavaNodeInfo info) {
        super(declName, null, declData, info, Collections.emptyList());

        this.declInfo = declInfo;
        this.nestedPrefix = nestedPrefix;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new NamespaceAliasDecl(nestedPrefix, declInfo, getDeclName(), getDeclData(), getInfo());
    }

    public DeclRef getAliasedNamespaceRef() {
        return declInfo;
    }

    public Optional<NamespaceDecl> getAliasedNamespace() {
        return getApp().getNodeTry(declInfo.getDeclId())
                .map(node -> (NamespaceDecl) node);
    }

    @Override
    public String getCode() {
        return "namespace " + getDeclName() + " = " + nestedPrefix + declInfo.getDeclType();
    }
}
