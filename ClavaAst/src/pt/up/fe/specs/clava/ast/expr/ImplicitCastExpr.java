/**
 * Copyright 2016 SPeCS.
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
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.collections.SpecsList;

/**
 * Represents an implicit type conversions which has no direct representation in the original source code.
 * 
 * @author JoaoBispo
 *
 */
public class ImplicitCastExpr extends CastExpr {

    /// DATAKEY BEGIN

    public final static DataKey<Expr> SUB_EXPR = KeyFactory.object("subExpr", Expr.class);

    /// DATAKEY END

    public ImplicitCastExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // @Override
    // public Expr getSubExpr() {
    // return getChild(Expr.class, 0);
    // }

    @Override
    public String getCode() {
        // The code of its only child
        return getSubExpr().getCode();
    }

    @Override
    public Expr getSubExpr() {
        if (!hasChildren()) {
            return get(SUB_EXPR);
        }

        return super.getSubExpr();
    }

    /**
     * Implicit cast overrides getType() and returns the type of sub expression.
     */
    // @Override
    // public Type getType() {
    // return getSubExpr().getType();
    // }

    @Override
    public Type getCastType() {
        return super.getType();
    }

    @Override
    public SpecsList<DataKey<?>> getSignatureKeys() {
        return super.getSignatureKeys().andAdd(TYPE);
    }
}
