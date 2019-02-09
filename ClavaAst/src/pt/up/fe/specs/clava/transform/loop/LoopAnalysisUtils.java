/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.transform.loop;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.Literal;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.ExprUse;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.stmt.BreakStmt;
import pt.up.fe.specs.clava.ast.stmt.ContinueStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

public class LoopAnalysisUtils {

    /**
     * Checks if the for loop has init, cond and inc statements.
     *
     * @param forLoop
     *            the loop to test
     * @return true if it has all the elements, false otherwise
     */
    static boolean hasHeader(ForStmt forLoop) {

        if (!forLoop.getInit().isPresent()
                || !forLoop.getCond().isPresent()
                || !forLoop.getInc().isPresent()) {

            return false;
        }

        return true;
    }

    static boolean hasSimpleInit(ForStmt forLoop) {

        // has init
        if (!forLoop.getInit().isPresent()) {

            return false;
        }

        // init is either a declaration of variable or an assignment
        Stmt init = forLoop.getInit().get();

        if (!(init instanceof DeclStmt || init instanceof ExprStmt)) {
            return false;
        }

        // test if it is a declaration
        if (init instanceof DeclStmt) {

            DeclStmt declStmt = (DeclStmt) init;
            if (declStmt.getDecls().size() != 1) {
                return false;
            }

            Decl decl = declStmt.getDecls().get(0);
            if (!(decl instanceof VarDecl)) {

                return false;
            }

            VarDecl varDecl = (VarDecl) decl;

            if (!varDecl.hasInit()) {

                return false;
            }
        }

        // test if it is an assignment
        if (init instanceof ExprStmt) {

            Expr expr = ((ExprStmt) init).getExpr();

            if (!(expr instanceof BinaryOperator)) {

                return false;
            }

            BinaryOperator binOp = ((BinaryOperator) expr);

            if (binOp.getOp() != BinaryOperatorKind.Assign) {

                return false;
            }
        }

        return true;
    }

    /**
     * Tests if the target loop has a condition of the form: i < SIZE.
     *
     * @param targetFor
     * @return
     */
    public static boolean hasSimpleCond(ForStmt targetFor) {

        // has cond
        if (!targetFor.getCond().isPresent()) {

            return false;
        }

        // cond is a binary expression
        Stmt cond = targetFor.getCond().get();

        if (!(cond instanceof ExprStmt)) {

            return false;
        }

        Expr expr = ((ExprStmt) cond).getExpr();

        if (!(expr instanceof BinaryOperator)) {

            return false;
        }

        // cond is a comparison using < or <=
        BinaryOperator binaryOperator = (BinaryOperator) expr;

        if (!(binaryOperator.getOp() == BinaryOperatorKind.LT ||
                binaryOperator.getOp() == BinaryOperatorKind.LE)) {

            return false;
        }

        // the RHS of cond is a literal or a reference to a variable
        Expr rhs = binaryOperator.getRhs();

        if (!(rhs instanceof DeclRefExpr || rhs instanceof Literal)) {
            return false;
        }

        return true;
    }

    /**
     * Tests if the target loop has an increment of the form: i++, i+=1 or i = i + 1;
     *
     * @param targetFor
     * @return
     */
    public static boolean hasSimpleInc(ForStmt targetFor) {

        // has inc
        if (!targetFor.getInc().isPresent()) {

            return false;
        }

        // inc has to be an expression statement
        Stmt inc = targetFor.getInc().get();

        if (!(inc instanceof ExprStmt)) {

            return false;
        }

        // the expression needs to be a unary or a binary operation
        Expr expr = ((ExprStmt) inc).getExpr();

        if (!(expr instanceof UnaryOperator || expr instanceof BinaryOperator)) {

            return false;
        }

        // if it is unary, it needs to be an increment
        if (expr instanceof UnaryOperator) {

            UnaryOperatorKind op = ((UnaryOperator) expr).getOp();
            if (!(op == UnaryOperatorKind.PostInc || op == UnaryOperatorKind.PreInc)) {

                return false;
            }
        }

        // if it is binary, it needs to an increment or an assignment
        if (expr instanceof BinaryOperator) {

            BinaryOperatorKind op = ((BinaryOperator) expr).getOp();
            if (!(op == BinaryOperatorKind.AddAssign || op == BinaryOperatorKind.Assign)) {

                return false;
            }
        }

        return true;
    }

    public static List<String> getControlVarNames(ForStmt targetFor) {

        Stmt inc = targetFor.getInc().orElse(null);
        if (inc == null) {
            return Collections.emptyList();
        }

        List<String> controlVars = inc.getDescendants(DeclRefExpr.class).stream()
                .filter(d -> d.use() == ExprUse.READWRITE || d.use() == ExprUse.WRITE).map(DeclRefExpr::getRefName)
                .collect(Collectors.toList());

        return controlVars;
    }

    public static Optional<Expr> getLowerBound(ForStmt targetFor) {

        if (!hasSimpleInit(targetFor)) {

            return Optional.empty();
        }

        Stmt init = targetFor.getInit().get();

        // if it is an assignment, get the LHS
        if (init instanceof ExprStmt) {

            Expr expr = ((ExprStmt) init).getExpr();
            BinaryOperator binOp = ((BinaryOperator) expr);

            return Optional.of(binOp.getRhs());
        }

        // if it is a declaration, get the name of the declared variable
        if (init instanceof DeclStmt) {

            VarDecl decl = (VarDecl) ((DeclStmt) init).getDecls().get(0);

            return Optional.of(decl.getInit().get());
        }

        throw new RuntimeException("init was neither an ExprStmt nor a DeclStmt");
    }

    /**
     * Tries to find the upper bound of the target loop by looking at the right-hand side of the condition expression.
     *
     * @param targetFor
     * @return
     */
    public static Optional<Expr> getUpperBound(ForStmt targetFor) {

        if (!hasSimpleCond(targetFor)) {

            return Optional.empty();
        }

        BinaryOperator comparison = (BinaryOperator) ((ExprStmt) targetFor.getCond().get()).getExpr();

        return Optional.of(comparison.getRhs());

    }

    /**
     * Tests whether the body of the target loop has complex control flow instructions such as: returns, breaks, and
     * continues.
     *
     * @param targetFor
     * @return
     */
    public static boolean hasComplexControlFlow(LoopStmt targetFor) {

        return targetFor.getDescendantsStream().anyMatch(LoopAnalysisUtils::isComplexControlFlow);

    }

    private static boolean isComplexControlFlow(ClavaNode node) {

        // TODO: add goto tests
        return node instanceof ReturnStmt
                || node instanceof BreakStmt
                || node instanceof ContinueStmt;
    }
}
