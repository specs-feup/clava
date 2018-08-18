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
import pt.up.fe.specs.clava.ast.expr.enums.PredefinedIdType;

/**
 * Represents a type cast.
 * 
 * @author JoaoBispo
 *
 */
public class PredefinedExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<PredefinedIdType> PREDEFINED_TYPE = KeyFactory.enumeration("PREDEFINED_TYPE",
            PredefinedIdType.class);

    /// DATAKEYS END

    public PredefinedExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final PredefinedIdType id;
    //
    // public PredefinedExpr(PredefinedIdType id, ExprData exprData, ClavaNodeInfo info,
    // Expr subExpr) {
    // this(id, exprData, info, Arrays.asList(subExpr));
    // }
    //
    // protected PredefinedExpr(PredefinedIdType id, ExprData exprData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    //
    // super(exprData, info, children);
    //
    // this.id = id;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new PredefinedExpr(id, getExprData(), getInfo(), Collections.emptyList());
    // }

    public PredefinedIdType getIdentifier() {
        return get(PREDEFINED_TYPE);
    }

    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        return getSubExpr().getCode();
    }

}
