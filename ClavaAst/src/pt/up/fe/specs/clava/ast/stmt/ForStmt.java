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
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class ForStmt extends LoopStmt {

    private static final Set<BinaryOperatorKind> RELATIONAL_OPS = EnumSet.of(BinaryOperatorKind.LE,
            BinaryOperatorKind.LT, BinaryOperatorKind.GE, BinaryOperatorKind.GT);

    public ForStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

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
    // public ForStmt(ClavaNodeInfo info, Stmt init, Stmt cond, Stmt inc, CompoundStmt body) {
    // this(info, sanitize(info, init, cond, inc, body));
    // }
    //
    // private ForStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(info, children);
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new ForStmt(getInfo(), Collections.emptyList());
    // }

    public Optional<Stmt> getInit() {
        return getOptionalChild(Stmt.class, 0);
        // return getNullable(0, Stmt.class);
    }

    public Optional<Stmt> getCond() {
        return getOptionalChild(Stmt.class, 1);
        // return getNullable(1, Stmt.class);
    }

    public Optional<Stmt> getInc() {
        return getOptionalChild(Stmt.class, 2);
        // return getNullable(2, Stmt.class);
    }

    @Override
    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, 3);

        // ClavaNode body = getChild(3);
        // if (body instanceof CompoundStmt) {
        // return (CompoundStmt) body;
        // }
        //
        // throw new RuntimeException("ForStmt: Not being normalized as a CompoundStmt\n" + toTree());
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
    public Optional<Expr> getIterationsExpr() {
        // Calculate diff between begin and end
        Expr beginEndDiff = getBeginEndDiff().orElse(null);
        if (beginEndDiff == null) {
            return Optional.empty();
        }

        Expr stepValue = getStepValueExpr().orElse(null);
        if (stepValue == null) {
            return Optional.empty();
        }

        Integer stepValueInteger = SpecsStrings.parseInteger(stepValue.getCode());
        // Special case: step value 1
        if (stepValueInteger != null && stepValueInteger.intValue() == 1) {
            return Optional.of(beginEndDiff);
        }

        return Optional
                .of(getFactory().binaryOperator(BinaryOperatorKind.Div, beginEndDiff.get(Expr.TYPE).get(), beginEndDiff,
                        stepValue));

        // return Optional.of(ClavaNodeFactory.binaryOperator(BinaryOperatorKind.DIV, beginEndDiff, stepValue));
    }

    private Optional<Expr> getBeginEndDiff() {
        Expr endExpr = getConditionValueExpr().orElse(null);
        if (endExpr == null) {
            return Optional.empty();
        }

        BinaryOperator binOp = getCondOperator().orElse(null);
        if (binOp == null) {
            return Optional.empty();
        }

        Expr initExpr = getInitValueExpr().orElse(null);
        if (initExpr == null) {
            return Optional.empty();
        }

        Integer initExprInteger = SpecsStrings.parseInteger(initExpr.getCode());

        // Special case: relation < and begin 0
        if (binOp.getOp() == BinaryOperatorKind.LT && initExprInteger != null && initExprInteger.intValue() == 0) {
            return Optional.of(endExpr);
        }

        // Special case: relation <= and begin 1
        if (binOp.getOp() == BinaryOperatorKind.LE && initExprInteger != null && initExprInteger.intValue() == 1) {
            return Optional.of(endExpr);
        }

        return Optional
                .of(getFactory().binaryOperator(BinaryOperatorKind.Sub, initExpr.get(Expr.TYPE).get(), endExpr,
                        initExpr));

        // return Optional.of(ClavaNodeFactory.binaryOperator(BinaryOperatorKind.SUB, endExpr, initExpr));
    }

}