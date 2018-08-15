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
import pt.up.fe.specs.clava.language.CXXOperator;
import pt.up.fe.specs.clava.utils.Nameable;

/**
 * A reference to a name which we were able to look up during parsing but could not resolve to a specific declaration.
 * 
 * @author JoaoBispo
 *
 */
public class UnresolvedLookupExpr extends OverloadExpr implements Nameable {

    /// DATAKEYS BEGIN

    /**
     * True if this declaration should be extended by argument-dependent lookup.
     */
    public final static DataKey<Boolean> REQUIRES_ADL = KeyFactory.bool("requiresAdl");

    /**
     * The looked up name.
     */
    public final static DataKey<String> NAME = KeyFactory.string("name");

    /**
     * The declarations in the unresolved set.
     */
    public final static DataKey<List<Decl>> UNRESOLVED_DECLS = KeyFactory
            .generic("unresolvedDecls", (List<Decl>) new ArrayList<Decl>());

    /// DATAKEYS END

    public UnresolvedLookupExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final boolean requiresAdl;
    // private String name;
    // private final List<String> decls;
    //
    // public UnresolvedLookupExpr(boolean requiresAdl, String name, List<String> decls, String qualifier,
    // ExprData exprData,
    // ClavaNodeInfo info) {
    //
    // super(qualifier, exprData, info, Collections.emptyList());
    //
    // this.requiresAdl = requiresAdl;
    // this.name = name;
    // this.decls = decls;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new UnresolvedLookupExpr(requiresAdl, name, decls, getQualifier().orElse(null), getExprData(),
    // getInfo());
    // }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        // Append qualifier, if present
        getQualifier().ifPresent(code::append);

        // Case operator
        Optional<CXXOperator> operator = CXXOperator.parseTry(get(NAME));
        if (operator.isPresent()) {
            code.append(operator.get().getString());
            return code.toString();
        }

        code.append(get(NAME));

        return code.toString();
    }

    @Override
    public String getName() {
        return get(NAME);
    }

    @Override
    public void setName(String name) {
        set(NAME, name);
        // this.name = name;
    }

}
