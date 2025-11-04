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
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.TemplateParameter;
import pt.up.fe.specs.clava.ast.type.Type;

public class NonTypeTemplateParmDecl extends DeclaratorDecl implements TemplateParameter {

    /// DATAKEYS BEGIN

    /**
     * The default-argument expression, if any.
     */
    public final static DataKey<Optional<Expr>> DEFAULT_ARGUMENT = KeyFactory.optional("defaultArgument");

    /**
     * True if the default argument was inherited from a previous declaration of this template.
     */
    public final static DataKey<Boolean> DEFAULT_ARGUMENT_WAS_INHERITED = KeyFactory
            .bool("defaultArgumentWasInherited");

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

    /**
     * The expansion types in an expanded parameter pack.
     */
    public final static DataKey<List<Type>> EXPANSION_TYPES = KeyFactory.list("expansionTypes", Type.class);

    /// DATAKEYS END

    public NonTypeTemplateParmDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {

        StringBuilder code = new StringBuilder();

        String declName = getDeclName();
        if (declName.isEmpty()) {
            code.append(getTypeCode());
        } else {
            code.append(getTypeCode(declName));
        }

        get(DEFAULT_ARGUMENT).ifPresent(expr -> code.append(" = ").append(expr.getCode()));

        return code.toString();

    }
}
