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
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.utils.Nameable;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.clava.utils.foriter.ForIterationsExpression;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class ForStmt extends LoopStmt {

    private static final Set<BinaryOperatorKind> RELATIONAL_OPS = EnumSet.of(BinaryOperatorKind.LE,
            BinaryOperatorKind.LT, BinaryOperatorKind.GE, BinaryOperatorKind.GT);

    public ForStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<Stmt> getInit() {
        return getOptionalChild(Stmt.class, 0);
    }

    public Optional<Stmt> getCond() {
        return getOptionalChild(Stmt.class, 1);
    }

    public Optional<Stmt> getInc() {
        return getOptionalChild(Stmt.class, 2);
    }

    @Override
    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, 3);
    }

    public Optional<VarDecl> getConditionVariable() {
        var condVar = getChild(4);

        if (condVar instanceof NullNode) {
            return Optional.empty();
        }

        return Optional.of((VarDecl) condVar);
    }

    @Override
    public String getCode() {
        return getCode("for");
    }

    protected String getCode(String forKeyword) {
        StringBuilder code = new StringBuilder();

        code.append(forKeyword).append("(");

        // Append 'init'
        code.append(getInit().map(init -> init.getCode()).orElse(";"));

        // Append 'cond'

        var condVar = getConditionVariable();
        // System.out.println("COND CODE: " + getCond().map(cond -> " " + cond.getCode()).orElse(";"));
        // System.out.println("VARDECL CODE: " + condVar.map(var -> var.getCode()).orElse("<no code>"));

        var condCode = condVar.map(var -> var.getCode() + ";")
                .orElse(getCond().map(cond -> " " + cond.getCode()).orElse(";"));

        code.append(condCode);
        // code.append(getCond().map(cond -> " " + cond.getCode()).orElse(";"));

        // Get 'inc' code
        String incCode = getInc()
                .map(init -> " " + removeSemicolon(init.getCode()))
                .orElse("");

        code.append(incCode);

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

    public void setInitValue(Expr expr) {
        getInitValueExpr().ifPresent(initExpr -> NodeInsertUtils.replace(initExpr, expr));
    }

    public void setConditionValue(Expr expr) {
        getConditionValueExpr().ifPresent(initExpr -> NodeInsertUtils.replace(initExpr, expr));
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

    public Optional<VarDecl> getInitVar() {
        return getInit()
                // Only for DeclStmt
                .filter(stmt -> stmt instanceof DeclStmt)
                // Get VarDecls
                .map(declStmt -> ((DeclStmt) declStmt).getVarDecls())
                // Only one variable
                .filter(varDecls -> varDecls.size() == 1)
                // Return VarDecl
                .map(varDecls -> varDecls.get(0));
    }

    /**
     * A ClavaNode that implements Nameable, that represents the iteration variable.
     * 
     * @return
     */
    public Optional<ClavaNode> getIterationVarNode() {
        var initStmt = getInit().orElse(null);

        if (initStmt == null) {
            return Optional.empty();
        }

        // Check DeclStmt
        if (initStmt instanceof DeclStmt) {
            var decls = ((DeclStmt) initStmt).getVarDecls();

            if (decls.size() != 1) {
                ClavaLog.debug(
                        () -> "ForStmt.getIterationVarName(): more than one iteration variable detected, " + decls);
                return Optional.empty();
            }

            return Optional.of(decls.get(0));
        }

        // Check ExprStmt
        if (initStmt instanceof ExprStmt) {
            var expr = ((ExprStmt) initStmt).getExpr();

            if (!(expr instanceof BinaryOperator)) {
                ClavaLog.debug(() -> "ForStmt.getIterationVarName(): expression not supported, " + expr);
                return Optional.empty();
            }

            var binaryOp = (BinaryOperator) expr;

            if (binaryOp.getOp() != BinaryOperatorKind.Assign) {
                ClavaLog.debug(
                        () -> "ForStmt.getIterationVarName(): binary operatior not supported, " + binaryOp.getOp());
                return Optional.empty();
            }

            var lhs = binaryOp.getLhs();

            if (!(lhs instanceof Nameable)) {
                ClavaLog.debug(
                        () -> "ForStmt.getIterationVarName(): could not determin name from left-hand expression, "
                                + lhs);
                return Optional.empty();
            }

            return Optional.of(lhs);
        }

        ClavaLog.debug(() -> "ForStmt.getIterationVarName(): statement not supported, " + initStmt);

        return Optional.empty();
    }

    private Optional<Expr> getInitValueExpr(ClavaNode initExpr) {

        if (initExpr instanceof VarDecl) {
            return ((VarDecl) initExpr).getInit();
        }

        if (initExpr instanceof BinaryOperator) {

            BinaryOperator binOp = (BinaryOperator) initExpr;
            if (binOp.getOp() != BinaryOperatorKind.Assign) {
                return Optional.empty();
            }

            return Optional.of(binOp.getRhs());
        }

        return Optional.empty();
    }

    /**
     * The value by which the iteration variable changes each iteration.
     * 
     * <p>
     * Supports values for increment expressions of Canonical Loop Forms as defined by the OpenMP standard.
     */
    public Optional<Expr> getStepValueExpr() {
        return getInc().flatMap(inc -> getStepValueExpr(inc.getChild(0)));
    }

    private Optional<Expr> getStepValueExpr(ClavaNode incExpr) {
        // var += incr
        // var -= incr
        // var = var + incr
        // var = incr + var
        // var = var - incr

        if (incExpr instanceof BinaryOperator) {
            BinaryOperator binaryOp = (BinaryOperator) incExpr;
            BinaryOperatorKind kind = binaryOp.getOp();

            if (kind == BinaryOperatorKind.AddAssign) {
                return Optional.of(binaryOp.getRhs());
            }

            if (kind == BinaryOperatorKind.SubAssign) {
                Expr rhs = binaryOp.getRhs();
                return Optional.of(getFactory().unaryOperator(UnaryOperatorKind.Minus, rhs.getExprType(), rhs));
            }

            ClavaLog.warning("Not yet implemented for binary operator of kind " + kind);
            return Optional.empty();
        }

        // ++var
        // var++
        // --var
        // var--
        if (incExpr instanceof UnaryOperator) {
            UnaryOperator unaryOp = (UnaryOperator) incExpr;
            UnaryOperatorKind kind = unaryOp.getOp();

            if (kind == UnaryOperatorKind.PreInc || kind == UnaryOperatorKind.PostInc) {
                return Optional.of(getFactory().integerLiteral(1));
            }

            if (kind == UnaryOperatorKind.PreDec || kind == UnaryOperatorKind.PostDec) {
                return Optional.of(getFactory().integerLiteral(-1));
            }

            return Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * 
     * @return an expression that represents the number of iterations of the loop
     */
    @Override
    public Optional<Expr> getIterationsExpr() {
        return ForIterationsExpression.newInstance(this).flatMap(iter -> iter.getIterationsExpr());
    }
}