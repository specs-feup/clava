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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.util.utilities.StringLines;

/**
 * Represents a group of statements (Stmt).
 *
 * @author JoaoBispo
 *
 */
public class CompoundStmt extends Stmt {

    // true if this CompoundStmt initially did not have curly braces {}
    private final boolean isNaked;

    public static CompoundStmt newNakedInstance(ClavaNodeInfo info, Collection<? extends Stmt> children) {
        return new CompoundStmt(true, info, children);
    }

    public CompoundStmt(ClavaNodeInfo info, Collection<? extends Stmt> children) {
        this(false, info, children);
    }

    private CompoundStmt(boolean isNaked, ClavaNodeInfo info, Collection<? extends Stmt> children) {
        super(info, children);

        this.isNaked = isNaked;

        // If naked, can have only only child, or be empty
        if (isNaked) {
            Preconditions.checkArgument(children.size() < 2, "Expected at most 1 child:" + children);
        }
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CompoundStmt(isNaked, getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        return getCode(false);
    }

    public String getCode(boolean inline) {

        List<Stmt> statements = getStatements();

        String newLine = inline ? "" : ln();
        String tab = inline ? " " : getTab();

        // If naked, and has only zero or one statements
        if (isNaked && statements.size() < 2) {

            if (statements.isEmpty()) {
                return ";";
            }
            Stmt statement = statements.get(0);

            return " " + statement.getCode() + statement.getInlineCommentsCode();
        }

        StringBuilder builder = new StringBuilder();

        // If not the direct child of another CompoundStmt (not a scope), add a space
        boolean hasCompoundStmtParent = getParent() instanceof CompoundStmt;
        if (hasCompoundStmtParent || inline) {
            // builder.append(ln() + "{");
            builder.append("{");
        } else {
            builder.append(" {");
        }

        String inlineComments = getInlineCommentsCode();
        builder.append(inlineComments);
        // builder.append(getInlineCommentsCode());

        // If not inline, or if there are comments, always add new line
        if (!inline || !inlineComments.isEmpty()) {
            builder.append(ln());
        }

        for (Stmt stmt : getStatements()) {

            String stmtCode = StringLines.getLines(stmt.getCode()).stream()
                    // Add ";" in the end if not present
                    // .map(line -> line.endsWith(";") ? line : line + ";")
                    // Add tab
                    .map(line -> tab + line)
                    .collect(Collectors.joining(newLine, "", stmt.getInlineCommentsCode() + newLine));
            builder.append(stmtCode);
        }

        if (inline) {
            builder.append(" ");
        }
        builder.append("}" + newLine);
        return builder.toString();
    }

    public List<Stmt> getStatements() {
        return getChildren(Stmt.class);
    }

    @Override
    public List<Stmt> toStatements() {
        return getStatements();
    }

    /**
     * A compound statement is a nested scope if it's parent is itself a compound statement.
     *
     * @return
     */
    public boolean isNestedScope() {
        if (!hasParent()) {
            throw new RuntimeException("Expected node to have a parent");
        }

        return getParent() instanceof CompoundStmt;
    }

    @Override
    public boolean isAggregateStmt() {
        return true;
    }
}
