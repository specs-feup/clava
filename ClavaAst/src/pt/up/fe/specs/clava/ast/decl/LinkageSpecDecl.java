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
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.enums.LanguageId;

/**
 * Represents a linkage specification (e.g., extern "C" ...).
 * 
 * @author JoaoBispo
 *
 */
public class LinkageSpecDecl extends Decl {

    private final LanguageId linkageType;

    public LinkageSpecDecl(LanguageId linkageType, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(declData, info, children);

        this.linkageType = linkageType;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new LinkageSpecDecl(linkageType, getDeclData(), getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();
        builder.append(ln() + "extern \"" + linkageType + "\" {" + ln());

        String childrenCode = getChildrenStream().map(child -> child.getCode())
                .collect(Collectors.joining(ln() + getTab(), getTab(), ln()));
        builder.append(childrenCode);
        builder.append("}" + ln());

        return builder.toString();
    }

    public LanguageId getLanguage() {
        return linkageType;
    }

}
