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
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TemplateParameter;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TemplateTypeParmKind;

/**
 * Declaration of a template type parameter.
 * 
 * @author JoaoBispo
 *
 */
public class TemplateTypeParmDecl extends TypeDecl implements TemplateParameter {

    /// DATAKEYS BEGIN

    public final static DataKey<TemplateTypeParmKind> KIND = KeyFactory.enumeration("kind", TemplateTypeParmKind.class);

    /**
     * True if this is a parameter pack.
     */
    public final static DataKey<Boolean> IS_PARAMETER_PACK = KeyFactory.bool("isParameterPack");

    public final static DataKey<Optional<Type>> DEFAULT_ARGUMENT = KeyFactory.optional("defaultArgument");

    /// DATAKEYS END

    public TemplateTypeParmDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final TemplateTypeParmKind kind;
    // private final boolean isParameterPack;

    // public TemplateTypeParmDecl(TemplateTypeParmKind kind, boolean isParameterPack, String name, DeclData declData,
    // ClavaNodeInfo info, TemplateArgument defaultArgument) {
    //
    // this(kind, isParameterPack, name, declData, info, SpecsCollections.ofNullable(defaultArgument));
    // }
    //
    // private TemplateTypeParmDecl(TemplateTypeParmKind kind, boolean isParameterPack, String name, DeclData declData,
    // ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    //
    // super(name, null, declData, info, children);
    //
    // this.kind = kind;
    // this.isParameterPack = isParameterPack;
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new TemplateTypeParmDecl(kind, isParameterPack, getDeclName(), getDeclData(), getInfo(),
    // Collections.emptyList());
    // }

    /*
    public Optional<TemplateArgument> getDefaultArgument() {
        if (!hasChildren()) {
            return Optional.empty();
        }
    
        return Optional.of(getChild(TemplateArgument.class, 0));
    }
    */

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(get(KIND).getString());

        if (get(IS_PARAMETER_PACK)) {
            code.append("...");
            // Preconditions.checkArgument(!getDefaultArgument().isPresent());
            Preconditions.checkArgument(!get(DEFAULT_ARGUMENT).isPresent());
        }

        if (hasDeclName()) {
            code.append(" ").append(getDeclName());
        }

        get(DEFAULT_ARGUMENT).ifPresent(arg -> code.append(" = ").append(arg.getCode(this)));

        return code.toString();
    }

}
