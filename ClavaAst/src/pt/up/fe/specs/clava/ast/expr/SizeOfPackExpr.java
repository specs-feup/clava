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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;

/**
 * Represents an expression that computes the length of a parameter pack.
 * 
 * @author JoaoBispo
 *
 */
public class SizeOfPackExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_PARTIALLY_SUBSTITUTED = KeyFactory.bool("isPartiallySubstituted");

    public final static DataKey<NamedDecl> PACK = KeyFactory.object("pack", NamedDecl.class);

    public final static DataKey<List<TemplateArgument>> PARTIAL_ARGUMENTS = KeyFactory.generic("partialArguments",
            new ArrayList<TemplateArgument>());

    /// DATAKEYS END

    public SizeOfPackExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public NamedDecl getPack() {
        return get(PACK);
    }

    public boolean isPartiallySubstituted() {
        return get(IS_PARTIALLY_SUBSTITUTED);
    }

    public List<TemplateArgument> getPartialArguments() {
        return get(PARTIAL_ARGUMENTS);
    }

    @Override
    public String getCode() {
        return "sizeof...(" + get(PACK).get(NamedDecl.DECL_NAME) + ")";
    }
}
