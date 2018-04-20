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

package pt.up.fe.specs.clava.ast.stmt.legacy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;

/**
 * Encapsulates an Expr that is being used as a statement.
 * 
 * @author JoaoBispo
 *
 */
public class ExprStmtLegacy extends ExprStmt {

    private final boolean hasSemicolon;

    public ExprStmtLegacy(ClavaNodeInfo info, Expr expr) {
        this(true, info, Arrays.asList(expr));
    }

    public ExprStmtLegacy(boolean hasSemicolon, ClavaNodeInfo info, Expr expr) {
        this(hasSemicolon, info, Arrays.asList(expr));
    }

    private ExprStmtLegacy(boolean hasSemicolon, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.hasSemicolon = hasSemicolon;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new ExprStmtLegacy(hasSemicolon, getInfo(), Collections.emptyList());
    }

    public Expr getExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        String suffix = hasSemicolon ? ";" : "";
        return getExpr().getCode() + suffix;
    }

}
