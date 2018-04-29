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

import java.util.Arrays;
import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.data.ExprStmtData;
import pt.up.fe.specs.clava.ast.stmt.data.StmtData;

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

    // private final boolean hasSemicolon;

    public ExprStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public ExprStmt(ExprStmtData data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public ExprStmt(boolean hasSemicolon, Expr expr) {
        this(new ExprStmtData(hasSemicolon, new StmtData(expr.getData())), Arrays.asList(expr));
    }

    public ExprStmt(Expr expr) {
        this(true, expr);
    }

    @Override
    public ExprStmtData getData() {
        return (ExprStmtData) super.getData();
    }

    /**
     * For legacy.
     * 
     * @param info
     * @param expr
     */
    /*
    public ExprStmt(ClavaNodeInfo info, Expr expr) {
        this(true, info, Arrays.asList(expr));
    }
    */

    /**
     * For legacy.
     * 
     * @param hasSemicolon
     * @param info
     * @param expr
     */
    /*
    public ExprStmt(boolean hasSemicolon, ClavaNodeInfo info, Expr expr) {
        this(hasSemicolon, info, Arrays.asList(expr));
    }
    */

    /**
     * Legacy.
     * 
     * @param info
     * @param children
     */
    protected ExprStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        // this.hasSemicolon = hasSemicolon;
    }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new ExprStmt(hasSemicolon, getInfo(), Collections.emptyList());
    // }

    public Expr getExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        String suffix = getData().hasSemicolon() ? ";" : "";
        return getExpr().getCode() + suffix;
    }

}
