/**
 * Copyright 2019 SPeCS.
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
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.extra.TemplateParameter;

public class TemplateTemplateParmDecl extends TemplateDecl implements TemplateParameter {
    /// DATAKEYS BEGIN

    /**
     * The default-argument expression, if any.
     */
    public final static DataKey<Optional<TemplateArgument>> DEFAULT_ARGUMENT = KeyFactory.optional("defaultArgument");

    /**
     * True if this is a parameter pack.
     */
    public final static DataKey<Boolean> IS_PARAMETER_PACK = KeyFactory.bool("isParameterPack");

    /**
     * True if this parameter pack is a pack expansion.
     */
    public final static DataKey<Boolean> IS_PACK_EXPANSION = KeyFactory.bool("isPackExpansion");

    /**
     * True if this parameter is a non-type template parameter pack that has a known list of different types at
     * different positions.
     */
    public final static DataKey<Boolean> IS_EXPANDED_PARAMETER_PACK = KeyFactory.bool("isExpandedParameterPack");

    public TemplateTemplateParmDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /// DATAKEYS END

    @Override
    public String getCode() {

        StringBuilder code = new StringBuilder();

        code.append("template <");

        String parameterList = getTemplateParameters().stream()
                .map(param -> param.getCode())
                .collect(Collectors.joining(", "));

        code.append(parameterList).append("> class ").append(getDeclName());
        get(DEFAULT_ARGUMENT).ifPresent(arg -> code.append(" = ").append(arg.getCode(this)));

        return code.toString();
    }

}
