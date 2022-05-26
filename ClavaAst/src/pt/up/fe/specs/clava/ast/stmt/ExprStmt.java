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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;

/**
 * Encapsulates an Expr that is being used as a statement.
 * 
 * @author JoaoBispo
 *
 */
public class ExprStmt extends Stmt {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> HAS_SEMICOLON = KeyFactory.bool("hasSemicolon");

    /// DATAKEYS END

    public ExprStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Expr getExpr() {
        return getChild(Expr.class, 0);
    }

    public Expr setExpr(Expr newExpr) {
        return (Expr) setChild(0, newExpr);
    }

    public boolean hasSemicolon() {
        return get(HAS_SEMICOLON);
    }

    @Override
    public String getCode() {
        String suffix = hasSemicolon() ? ";" : "";
        return getExpr().getCode() + suffix;
    }

    public ExprStmt setHasSemicolon(boolean hasSemicolon) {
        set(HAS_SEMICOLON, hasSemicolon);
        return this;
    }

}
