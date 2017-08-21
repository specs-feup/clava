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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.Type;

public class NamespaceDecl extends NamedDecl {

    // private final String namespace;
    private final boolean isInline;
    private final BareDeclData originalNamespace;

    public NamespaceDecl(boolean isInline, BareDeclData originalNamespace, String namespace, DeclData declData,
            ClavaNodeInfo info, List<? extends ClavaNode> namespaceDecls) {

        this(isInline, originalNamespace, namespace, null, declData, info, namespaceDecls);
    }

    private NamespaceDecl(boolean isInline, BareDeclData originalNamespace, String namespace, Type type,
            DeclData declData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {

        super(namespace, type, declData, info, children);

        // this.namespace = namespace;
        this.isInline = isInline;
        this.originalNamespace = originalNamespace;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new NamespaceDecl(isInline, originalNamespace, getDeclName(), getType(), getDeclData(), getInfo(),
                Collections.emptyList());
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

    // private String getCompleteNamespace(StringBuilder builder) {
    // // Add self
    // if(builder.)
    // }

    // @Override
    // public String getDeclName() {
    // return namespace;
    // }

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
