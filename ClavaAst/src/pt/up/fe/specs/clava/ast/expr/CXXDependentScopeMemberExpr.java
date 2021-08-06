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
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;

/**
 * A C++ member access expression where the actual member referenced could not be resolved because the base expression
 * or the member name was dependent.
 * 
 * Like UnresolvedMemberExprs, these can be either implicit or explicit accesses. It is only possible to get one of
 * these with an implicit access if a qualifier is provided.
 * 
 * @author JoaoBispo
 *
 */
public class CXXDependentScopeMemberExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_ARROW = KeyFactory.bool("isArrow");

    public final static DataKey<String> MEMBER_NAME = KeyFactory.string("memberName");

    public final static DataKey<Boolean> IS_IMPLICIT_ACCESS = KeyFactory.bool("isImplicitAccess");

    public final static DataKey<String> QUALIFIER = KeyFactory.string("qualifier");

    public final static DataKey<Boolean> HAS_TEMPLATE_KEYWORD = KeyFactory.bool("hasTemplateKeyword");

    public final static DataKey<List<TemplateArgument>> TEMPLATE_ARGUMENTS = KeyFactory
            .generic("templateArguments", (List<TemplateArgument>) new ArrayList<TemplateArgument>())
            .setDefault(() -> new ArrayList<>());

    /// DATAKEYS END

    public CXXDependentScopeMemberExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<Expr> getBase() {
        return Optional.ofNullable(get(IS_IMPLICIT_ACCESS) ? null : getChild(Expr.class, 0));
    }

    @Override
    public String getCode() {
        String separator = get(IS_ARROW) ? "->" : ".";

        var code = getBase().map(base -> base.getCode() + separator).orElse(get(QUALIFIER)) + get(MEMBER_NAME);

        return code;
    }

}
