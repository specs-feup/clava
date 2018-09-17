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

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * A reference to a name which we were able to look up during parsing but could not resolve to a specific declaration.
 * 
 * @author JoaoBispo
 *
 */
public class UnresolvedLookupExpr extends OverloadExpr {

    /// DATAKEYS BEGIN

    /**
     * True if this declaration should be extended by argument-dependent lookup.
     */
    public final static DataKey<Boolean> REQUIRES_ADL = KeyFactory.bool("requiresAdl");

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

}
