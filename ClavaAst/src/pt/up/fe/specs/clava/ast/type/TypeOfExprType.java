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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;

/**
 * Represents a typeof (or typeof) expression (a GCC extension).
 * 
 * @author JBispo
 *
 */
public class TypeOfExprType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_SUGARED = KeyFactory.bool("isSugared");

    public final static DataKey<Expr> UNDERLYING_EXPR = KeyFactory.object("underlingExpr", Expr.class);

    /// DATAKEYS END

    public TypeOfExprType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public Type desugar() {
        if (get(IS_SUGARED)) {
            return get(UNDERLYING_EXPR).getType();
        }

        return this;
    }

    public Expr getUnderlyingExpr() {
        return get(UNDERLYING_EXPR);
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        // If GNU, do not change type (i.e., typeof)
        if (sourceNode != null) {
            boolean isGnu = sourceNode.getAncestorTry(TranslationUnit.class)
                    .map(tu -> tu.get(TranslationUnit.LANGUAGE).get(Language.IS_GNU))
                    .orElse(false);

            if (isGnu) {
                return super.getCode(sourceNode, name);
            }
        }

        // Not GNU, change to __typeof__
        String typeCode = super.getCode(sourceNode, name);
        Preconditions.checkArgument(typeCode.startsWith("typeof "), "Expected code of type to start with 'typeof '");

        return "__typeof__" + typeCode.substring("typeof".length());
    }

}
