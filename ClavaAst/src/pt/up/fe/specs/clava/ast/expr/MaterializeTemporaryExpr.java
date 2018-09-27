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
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.util.collections.SpecsList;

/**
 * Represents a prvalue (pure rvalue - does not have identity, can be moved) temporary that is written into memory so
 * that a reference can bind to it.
 * 
 * <p>
 * MaterializeTemporaryExprs are always glvalues (either an lvalue or an xvalue, depending on the kind of reference
 * binding to it), maintaining the invariant that references always bind to glvalues.
 * 
 * @author JoaoBispo
 *
 */
public class MaterializeTemporaryExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<Optional<Decl>> EXTENDING_DECL = KeyFactory.optional("extendingDecl");

    /// DATAKEYS END

    public MaterializeTemporaryExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final BareDeclData extendingDecl;
    //
    // public MaterializeTemporaryExpr(ExprData exprData, BareDeclData extendingDecl, ClavaNodeInfo info,
    // Expr temporaryExpr) {
    // this(exprData, extendingDecl, info, Arrays.asList(temporaryExpr));
    // }
    //
    // private MaterializeTemporaryExpr(ExprData exprData, BareDeclData extendingDecl, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    //
    // super(exprData, info, children);
    //
    // this.extendingDecl = extendingDecl;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new MaterializeTemporaryExpr(getExprData(), extendingDecl, getInfo(), Collections.emptyList());
    // }

    @Override
    public String getCode() {
        return getTemporaryExpr().getCode();
    }

    public Expr getTemporaryExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public SpecsList<DataKey<?>> getSignatureKeys() {
        // Add ID in order to "ignore" it during normalization
        return super.getSignatureKeys().andAdd(ID);
    }
}
