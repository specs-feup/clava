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

package pt.up.fe.specs.clava.ast.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.language.CXXOperator;
import pt.up.fe.specs.clava.utils.Nameable;

/**
 * A reference to an overloaded function set, either an UnresolvedLookupExpr or an UnresolvedMemberExpr.
 * 
 * @author JoaoBispo
 *
 */
public abstract class OverloadExpr extends Expr implements Nameable {

    /// DATAKEYS BEGIN

    /**
     * The nested-name qualifier that precedes the name, or empty string if it has none.
     */
    public final static DataKey<String> QUALIFIER = KeyFactory.string("qualifier");

    /**
     * The looked up name.
     */
    public final static DataKey<String> NAME = KeyFactory.string("name");

    /**
     * The declarations in the unresolved set.
     */
    public final static DataKey<List<Decl>> UNRESOLVED_DECLS = KeyFactory
            .generic("unresolvedDecls", (List<Decl>) new ArrayList<Decl>());

    public final static DataKey<List<TemplateArgument>> TEMPLATE_ARGUMENTS = KeyFactory
            .generic("templateArguments", (List<TemplateArgument>) new ArrayList<TemplateArgument>())
            .setDefault(() -> new ArrayList<>());

    /// DATAKEYS END

    public OverloadExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<String> getQualifier() {
        String qualifier = get(QUALIFIER);

        return qualifier.isEmpty() ? Optional.empty() : Optional.of(qualifier);
    }

    @Override
    public String getName() {
        return get(NAME);
    }

    @Override
    public void setName(String name) {
        set(NAME, name);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        // Append qualifier, if present
        getQualifier().ifPresent(code::append);

        // Case operator
        Optional<CXXOperator> operator = CXXOperator.parseTry(get(NAME));
        if (operator.isPresent()) {
            code.append(operator.get().getString());
        } else {
            code.append(get(NAME));
        }

        code.append(TemplateArgument.getCode(get(TEMPLATE_ARGUMENTS), this));

        return code.toString();
    }
}
