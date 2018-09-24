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

/**
 * Represents explicit type conversions in the source code.
 * 
 * @author JoaoBispo
 *
 */
public abstract class ExplicitCastExpr extends CastExpr {

    /// DATAKEY BEGIN

    public final static DataKey<Type> TYPE_AS_WRITTEN = KeyFactory.object("typeAsWritten", Type.class);

    /// DATAKEY END

    public ExplicitCastExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * Besides setting the key 'TYPE', it also sets the key 'TYPE_AS_WRITTEN'.
     */
    @Override
    public void setType(Type type) {
        super.setType(type);

        set(TYPE_AS_WRITTEN, type);
    }

    // public ExplicitCastExpr(CastKind castKind, ExprData exprData, ClavaNodeInfo info,
    // Expr subExpr) {
    //
    // super(castKind, exprData, info, subExpr);
    // }

    /**
     * Constructor without children, for node copy.
     * 
     * @param castKind
     * @param location
     */
    // protected ExplicitCastExpr(CastKind castKind, ExprData exprData, ClavaNodeInfo info,
    // List<? extends ClavaNode> children) {
    //
    // super(castKind, exprData, info, children);
    // }

}
