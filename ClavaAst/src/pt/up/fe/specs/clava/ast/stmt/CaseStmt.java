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
import java.util.Collections;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.Expr;

/**
 * Represents a regular switch case statement.
 * 
 * @author JoaoBispo
 *
 */
public class CaseStmt extends SwitchCase {

    // Right-hand side is used in the GNU extension "case 1 ... 4"
    private final boolean hasRhs;

    public CaseStmt(ClavaNodeInfo info, Expr lhs, Stmt subStmt) {
        this(false, info, Arrays.asList(lhs, subStmt));
    }

    public CaseStmt(ClavaNodeInfo info, Expr lhs, Expr rhs, Stmt subStmt) {
        this(true, info, Arrays.asList(lhs, rhs, subStmt));
    }

    private CaseStmt(boolean hasRhs, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.hasRhs = hasRhs;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CaseStmt(hasRhs, getInfo(), Collections.emptyList());
    }

    public Expr getLhs() {
        return getChild(Expr.class, 0);
    }

    public Optional<Expr> getRhs() {
        if (!hasRhs) {
            return Optional.empty();
        }

        return Optional.of(getChild(Expr.class, 1));
    }

    @Override
    public Stmt getSubStmt() {
        if (hasRhs) {
            return getChild(Stmt.class, 2);
        }

        return getChild(Stmt.class, 1);
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        builder.append("case ").append(getLhs().getCode());

        // Add rhs, if present
        getRhs().ifPresent(rhs -> builder.append(" ... ").append(rhs.getCode()));
        builder.append(":" + ln()).append(indentCode(getSubStmt().getCode()));

        return builder.toString();
    }

}
