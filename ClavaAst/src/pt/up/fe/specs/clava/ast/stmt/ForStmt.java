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
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.Expr;

public class ForStmt extends LoopStmt {

    private static final Set<BinaryOperatorKind> RELATIONAL_OPS = EnumSet.of(BinaryOperatorKind.LE,
            BinaryOperatorKind.LT, BinaryOperatorKind.GE, BinaryOperatorKind.GT);

    /**
     * Constructor of a 'for' statement.
     *
     * <p>
     * Init, cond and inc are optional and can be null.
     *
     * @param info
     * @param init
     * @param cond
     * @param inc
     * @param body
     */
    public ForStmt(ClavaNodeInfo info, Stmt init, Stmt cond, Stmt inc, CompoundStmt body) {
        this(info, sanitize(info, init, cond, inc, body));
    }

    private ForStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new ForStmt(getInfo(), Collections.emptyList());
    }

    public Optional<Stmt> getInit() {
        return getNullable(0, Stmt.class);
    }

    public Optional<Stmt> getCond() {
        return getNullable(1, Stmt.class);
    }

    public Optional<Stmt> getInc() {
        return getNullable(2, Stmt.class);
    }

    @Override
    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, 3);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        // If the first parent that is not a CompountStmt is not a ForStmt

        code.append("for(");

        // ifPresent(init -> code.append(init.getCode()));
        // code.append(getInit().orElse(ClavaNodeFactory.literalStmt(";", getInfo())).getCode());
        // Append 'init'
        code.append(getInit().map(init -> init.getCode()).orElse(";"));
        // code.append(";");

        // Append 'cond'
        code.append(getCond().map(init -> " " + init.getCode()).orElse(";"));

        // getCond().ifPresent(cond -> code.append(cond.getCode()));
        // code.append(";");
        // Get 'inc' code
        String incCode = getInc().map(init -> " " + init.getCode()).orElse("");
        if (incCode.endsWith(";")) {
            incCode = incCode.substring(0, incCode.length() - 1);
        }
        code.append(incCode);
        // getInc().ifPresent(inc -> code.append(inc.getCode()));
        code.append(")");
        code.append(getBody().getCode());

        return code.toString();
    }

    @Override
    public Optional<ClavaNode> getStmtCondition() {
        return getCond().map(node -> (ClavaNode) node);
    }

    public void setInit(LiteralStmt literalStmt) {

        setChild(0, literalStmt);
    }

    public void setCond(LiteralStmt literalStmt) {

        setChild(1, literalStmt);
    }

    public void setInc(LiteralStmt literalStmt) {

        setChild(2, literalStmt);
    }

    public Optional<BinaryOperator> getCondOperator() {
        return getCond()
                .map(cond -> cond.getChild(0))
                .filter(BinaryOperator.class::isInstance)
                .map(BinaryOperator.class::cast);

    }

    /**
     * The value expression of the test relation in the condition of an OpenM canonical loop form.
     * 
     * <p>
     * The value expression is define if the condition expression is a binary operator with one of the following
     * operators: <, <=, >, >=
     * 
     * @return
     */
    public Optional<Expr> getConditionValueExpr() {
        return getCondOperator()
                .filter(binOp -> RELATIONAL_OPS.contains(binOp.getOp()))
                .map(BinaryOperator::getRhs);

    }

    public Optional<Expr> getInitValueExpr() {
        return getInit().flatMap(init -> getInitValueExpr(init.getChild(0)));
    }

    private Optional<Expr> getInitValueExpr(ClavaNode initExpr) {

        if (initExpr instanceof VarDecl) {
            return ((VarDecl) initExpr).getInit();
        }

        if (initExpr instanceof BinaryOperator) {

            BinaryOperator binOp = (BinaryOperator) initExpr;
            if (binOp.getOp() != BinaryOperatorKind.ASSIGN) {
                return Optional.empty();
            }

            return Optional.of(binOp.getRhs());
        }

        return Optional.empty();
    }

}