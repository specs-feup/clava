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
import java.util.Collections;
import java.util.Optional;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;
import pt.up.fe.specs.clava.language.TemplateTypeParmKind;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * Declaration of a template type parameter.
 * 
 * @author JoaoBispo
 *
 */
public class TemplateTypeParmDecl extends TypeDecl {

    private final TemplateTypeParmKind kind;
    private final boolean isParameterPack;

    public TemplateTypeParmDecl(TemplateTypeParmKind kind, boolean isParameterPack, String name, DeclData declData,
            ClavaNodeInfo info, TemplateArgument defaultArgument) {

        this(kind, isParameterPack, name, declData, info, SpecsCollections.ofNullable(defaultArgument));
    }

    private TemplateTypeParmDecl(TemplateTypeParmKind kind, boolean isParameterPack, String name, DeclData declData,
            ClavaNodeInfo info, Collection<? extends ClavaNode> children) {

        super(name, null, declData, info, children);

        this.kind = kind;
        this.isParameterPack = isParameterPack;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new TemplateTypeParmDecl(kind, isParameterPack, getDeclName(), getDeclData(), getInfo(),
                Collections.emptyList());
    }

    public Optional<TemplateArgument> getDefaultArgument() {
        if (!hasChildren()) {
            return Optional.empty();
        }

        return Optional.of(getChild(TemplateArgument.class, 0));
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(kind.getString());

        if (isParameterPack) {
            code.append("...");
            Preconditions.checkArgument(!getDefaultArgument().isPresent());
        }

        if (hasDeclName()) {
            code.append(" ").append(getDeclName());
        }

        getDefaultArgument().ifPresent(arg -> code.append(" = ").append(arg.getCode()));

        return code.toString();
    }

}
