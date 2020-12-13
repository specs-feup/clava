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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;

public class DependentScopeDeclRefExpr extends Expr {

    /**
     * The name that this expression refers to.
     */
    public static final DataKey<String> DECL_NAME = KeyFactory.string("declName");

    /**
     * The nested-name qualifier that precedes the name, or empty string if it has none.
     */
    public final static DataKey<String> QUALIFIER = KeyFactory.string("qualifier");

    /**
     * True if the name was preceded by the template keyword.
     */
    public static final DataKey<Boolean> HAS_TEMPLATE_KEYWORD = KeyFactory.bool("hasTemplateKeyword");

    /**
     * The explicit template arguments of this lookup.
     */
    public final static DataKey<List<TemplateArgument>> TEMPLATE_ARGUMENTS = KeyFactory
            .generic("templateArguments", (List<TemplateArgument>) new ArrayList<TemplateArgument>())
            .setDefault(() -> new ArrayList<>());

    public DependentScopeDeclRefExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        // Not implemented yet

        if (get(HAS_TEMPLATE_KEYWORD)) {
            ClavaLog.warning(this, "Not implemented yet when the 'template' keyword is present");
        }

        if (!get(TEMPLATE_ARGUMENTS).isEmpty()) {
            ClavaLog.warning(this, "Not implemented yet when explicit template arguments are present");
        }

        return get(QUALIFIER) + get(DECL_NAME);
    }

}
