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
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.util.utilities.StringLines;

/**
 * Represents a group of statements (Stmt).
 *
 * @author JoaoBispo
 *
 */
public class CompoundStmt extends Stmt {

    public final static DataKey<Boolean> IS_NAKED = KeyFactory.bool("isNaked");

    public CompoundStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
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
        if (get(IS_NAKED) && statements.size() < 2) {

            if (statements.isEmpty()) {
                return ";";
            }
            Stmt statement = statements.get(0);

            String commentsCode = statement.getInlineCommentsCode();
            if (inline && !commentsCode.isEmpty()) {
                commentsCode += ln();
            }
            // System.out.println("STMT: " + statement);
            // System.out.println("STATEMENT: " + statement.getCode() + commentsCode);
            // System.out.println("PROCESSED:\n" + StringLines.getLines(statement.getCode() + commentsCode).stream()
            // .collect(Collectors.joining(newLine, tab, newLine)));

            return StringLines.getLines(statement.getCode() + commentsCode).stream()
                    .collect(Collectors.joining(newLine + tab, newLine + tab, newLine));

            // return newLine + tab + statement.getCode() + commentsCode;
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

            String inlineComment = stmt.getInlineCommentsCode();

            String stmtCode = StringLines.getLines(stmt.getCode()).stream()
                    // Add ";" in the end if not present
                    // .map(line -> line.endsWith(";") ? line : line + ";")
                    // Add tab
                    .map(line -> tab + line)
                    .collect(Collectors.joining(newLine, "", inlineComment + newLine));
            builder.append(stmtCode);

            if (inline && !inlineComment.isEmpty()) {
                builder.append(ln());
            }

            if (inline && (stmt instanceof WrapperStmt)) {
                if (((WrapperStmt) stmt).getWrappedNode() instanceof InlineComment) {
                    builder.append(ln());
                }
            }
        }

        if (inline) {
            builder.append(" ");
        }
        builder.append("}" + newLine);
        return builder.toString();
    }

    @Override
    public void setChildren(Collection<? extends ClavaNode> children) {
        // Check that all children are statements
        ClavaNode nonStmtNode = children.stream()
                .filter(child -> !(child instanceof Stmt))
                .findFirst()
                .orElse(null);

        if (nonStmtNode != null) {
            throw new RuntimeException("Found at least a child that is not a Stmt, " + nonStmtNode);
        }

        super.setChildren(children);
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

    public boolean isNaked() {

        // If has more than one statement, cannot be naked
        var oneOrZeroStatements = getStatements().size() < 2;

        // If one statement but is a scope, cannot be naked
        var noScope = getStatements().size() == 1 ? !(getStatements().get(0) instanceof CompoundStmt) : true;
        return get(IS_NAKED) && oneOrZeroStatements && noScope;
    }

    public CompoundStmt setNaked(boolean isNaked) {
        if (isNaked && getStatements().size() > 1) {
            ClavaLog.warning("Compount statements with more than one statement can't be naked");
            return this;
        }

        set(IS_NAKED, isNaked);
        return this;
    }

}
