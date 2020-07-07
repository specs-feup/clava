/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.utils.foriter;

import java.util.Optional;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.Literal;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class ForIterationsExpression {

    // private final VarDecl iterVar;
    private final String iterVarName;
    private final Type iterVarType;
    private final Expr initExpr;
    private final StepData stepData;
    private final ConditionData conditionData;

    public ForIterationsExpression(String iterVarName, Type iterVarType, Expr initExpr,
            StepData stepData, ConditionData conditionData) {
        // this.iterVar = iterVar;
        this.iterVarName = iterVarName;
        this.iterVarType = iterVarType;
        this.initExpr = initExpr;
        this.stepData = stepData;
        this.conditionData = conditionData;

    }

    // public VarDecl getIterVar() {
    // return iterVar;
    // }
    public String getIterVarName() {
        return iterVarName;
    }

    public Type getIterVarType() {
        return iterVarType;
    }

    public Expr getInitExpr() {
        return initExpr;
    }

    public StepData getStepData() {
        return stepData;
    }

    public ConditionData getConditionData() {
        return conditionData;
    }

    public Optional<Expr> getIterationsExpr() {

        var end = getBoundary(false);
        var start = getBoundary(true);

        Expr interval = end;

        // If start is not zero, include subtraction
        if (!start.getCode().equals("0")) {
            interval = start.getFactory().binaryOperator(BinaryOperatorKind.Sub, iterVarType, end, start);
        }

        // Adjust interval, if needed
        interval = adjustInterval(interval);

        // Divide by step, if not literally 1
        if (stepData.getStepValue().getCode().equals("1")) {
            return Optional.of(interval);
        }

        // Divide by step value
        interval = protectTerm(interval);
        var step = protectTerm(stepData.getStepValue());
        interval = step.getFactory().binaryOperator(BinaryOperatorKind.Div, iterVarType, interval, step);

        return Optional.of(interval);
    }

    private Expr adjustInterval(Expr interval) {
        // No adjustment needed
        if (conditionData.getRelation() == ForCondRelation.GT || conditionData.getRelation() == ForCondRelation.LT) {
            return interval;
        }

        // Only adjust if GE or LE
        return interval.getFactory().binaryOperator(BinaryOperatorKind.Add, iterVarType, interval,
                stepData.getStepValue());
    }

    private Expr getBoundary(boolean isStart) {

        Expr boundary = null;

        switch (stepData.getDirection()) {
        case INC:
            boundary = isStart ? initExpr : conditionData.getValue();
            break;
        case DEC:
            boundary = isStart ? conditionData.getValue() : initExpr;
            break;
        default:
            throw new NotImplementedException(stepData.getDirection());
        }

        // Add parenthesis, if necessary
        boundary = protectTerm(boundary);
        return boundary;
    }

    private Expr protectTerm(Expr expr) {
        // If literal, just return it
        if (expr instanceof Literal) {
            return expr;
        }

        return expr.getFactory().parenExpr(expr);
    }

    public static Optional<ForIterationsExpression> newInstance(ForStmt forStmt) {
        // Get init variable
        // var iterVar = forStmt.getInitVar().orElse(null);
        //
        // if (iterVar == null) {
        // ClavaLog.debug("ForIterationsExpression: could not determine iteration variable of 'for' at "
        // + forStmt.getLocation());
        // return Optional.empty();
        // }

        var iterVarNode = forStmt.getIterationVarNode().orElse(null);

        if (iterVarNode == null) {
            ClavaLog.debug("ForIterationsExpression: could not determine iteration variable of 'for' at "
                    + forStmt.getLocation());
            return Optional.empty();
        }

        var iterVarName = ClavaNodes.getName(iterVarNode);
        var iterVarType = ((Typable) iterVarNode).getType();

        // Get init value
        // var initExpr = iterVar.getInit().orElse(null);
        var initExpr = forStmt.getInitValueExpr().orElse(null);
        if (initExpr == null) {
            ClavaLog.debug(
                    "ForIterationsExpression: could not determine initial value of iteration variable "
                            + iterVarName + " at " + forStmt.getLocation());
            return Optional.empty();
        }

        // Get step value and step direction
        var stepData = forStmt.getInc().flatMap(incStmt -> getStepData(incStmt, iterVarName)).orElse(null);
        if (stepData == null) {
            ClavaLog.debug(
                    "ForIterationsExpression: could not determine step data of 'for' at " + forStmt.getLocation());
            return Optional.empty();
        }

        // Get condition data
        var conditionData = forStmt.getCond().flatMap(cond -> getConditionData(cond, iterVarName)).orElse(null);

        if (conditionData == null) {
            ClavaLog.debug("ForIterationsExpression: could not determine condition data of 'for' at "
                    + forStmt.getLocation());
            return Optional.empty();
        }

        // System.out.println("STEP DATA: " + stepData);
        // System.out.println("COND DATA: " + conditionData);

        // Verify data
        var forIterations = new ForIterationsExpression(iterVarName, iterVarType, initExpr, stepData,
                conditionData);
        if (!verify(forIterations)) {
            return Optional.empty();
        }

        return Optional.of(forIterations);
    }

    private static boolean verify(ForIterationsExpression forIterations) {
        var condRelation = forIterations.getConditionData().getRelation();

        // LT and LE should only be used with INC, GT and GE should only be used with DEC
        switch (forIterations.getStepData().getDirection()) {
        case INC:
            if (condRelation == ForCondRelation.GE || condRelation == ForCondRelation.GT) {
                ClavaLog.debug(() -> "ForIterationsExpression: cannot use > or >= in condition when step increases");
                return false;
            }
            break;
        case DEC:
            if (condRelation == ForCondRelation.LE || condRelation == ForCondRelation.LT) {
                ClavaLog.debug(() -> "ForIterationsExpression: cannot use < or <= in condition when step decreases");
                return false;
            }
            break;
        }

        // LE and GE are only supported when iteration variable in an integer
        if (condRelation == ForCondRelation.GE || condRelation == ForCondRelation.LE) {
            var iterVarType = forIterations.getIterVarType().desugarAll();

            // Only supports built-in type (after desugaring)
            if (!(iterVarType instanceof BuiltinType)) {
                ClavaLog.debug(
                        () -> "ForIterationsExpression: cannot verify <= or >= if iteration variable is not a built-in type, "
                                + iterVarType);
                return false;
            }

            var builtinType = (BuiltinType) iterVarType;
            var isInteger = builtinType.get(BuiltinType.KIND).isInteger();

            if (!isInteger) {
                ClavaLog.debug(
                        () -> "ForIterationsExpression: operation <= or >= in condition expression only supported if iteration variable is an integer, "
                                + iterVarType);
                return false;
            }
        }

        return true;
    }

    private static Optional<ConditionData> getConditionData(Stmt conditionStmt, String iterVarName) {

        // Only supports ExprStmt
        if (!(conditionStmt instanceof ExprStmt)) {
            ClavaLog.debug(() -> "ForIterationsExpression: cond statement is not an ExprStmt, " + conditionStmt);
            return Optional.empty();
        }

        var condExpr = ((ExprStmt) conditionStmt).getExpr();

        if (!(condExpr instanceof BinaryOperator)) {
            ClavaLog.debug(() -> "ForIterationsExpression: cond expr is not a binary operator, " + condExpr);
            return Optional.empty();
        }

        var binaryOp = (BinaryOperator) condExpr;

        // Find iteration var
        int iterVarIndex = getVarIndex(iterVarName, binaryOp);
        if (iterVarIndex == -1) {
            ClavaLog.debug(
                    () -> "ForIterationsExpression: could not find iteration variable on one of the sides of the condition  expression, "
                            + condExpr);
            return Optional.empty();
        }

        int condValueExprIndex = getCondExprIndex(iterVarIndex);
        var condValueExpr = binaryOp.getChild(Expr.class, condValueExprIndex);

        var condRelation = getCondRelation(binaryOp.getOp(), iterVarIndex);

        return Optional.of(new ConditionData(condValueExpr, condRelation));
    }

    private static ForCondRelation getCondRelation(BinaryOperatorKind op, int iterVarIndex) {
        switch (op) {
        case LT:
            return iterVarIndex == 0 ? ForCondRelation.LT : ForCondRelation.GT;
        case GT:
            return iterVarIndex == 0 ? ForCondRelation.GT : ForCondRelation.LT;
        case LE:
            return iterVarIndex == 0 ? ForCondRelation.LE : ForCondRelation.GE;
        case GE:
            return iterVarIndex == 0 ? ForCondRelation.GE : ForCondRelation.LE;
        default:
            ClavaLog.debug(
                    () -> "ForIterationsExpression: unsupported binary operator in condition  expression, "
                            + op.getOpString());
            return null;
        }
    }

    private static int getCondExprIndex(int iterVarIndex) {
        if (iterVarIndex == 0) {
            return 1;
        }

        if (iterVarIndex == 1) {
            return 0;
        }

        throw new RuntimeException("Invalid iteration variable index: " + iterVarIndex);
    }

    private static int getVarIndex(String varName, BinaryOperator expr) {
        for (int i = 0; i < expr.getNumChildren(); i++) {
            var child = expr.getChild(i);

            // Normalize node
            child = ClavaNodes.normalize(child);

            // Only supports DeclRefExpr
            if (!(child instanceof DeclRefExpr)) {
                continue;
            }

            if (varName.equals(((DeclRefExpr) child).getName())) {
                return i;
            }
        }

        return -1;
    }

    private static Optional<StepData> getStepData(Stmt incStmt, String iterVarName) {

        // Only supports ExprStmt
        if (!(incStmt instanceof ExprStmt)) {
            ClavaLog.debug(() -> "ForIterationsExpression: step statement is not an ExprStmt, " + incStmt);
            return Optional.empty();
        }

        var incExpr = ((ExprStmt) incStmt).getExpr();

        // var += incr
        // var -= incr
        // var = var + incr
        // var = incr + var
        // var = var - incr

        if (incExpr instanceof BinaryOperator) {
            BinaryOperator binaryOp = (BinaryOperator) incExpr;

            boolean hasIterVar = isVarReference(iterVarName, binaryOp.getLhs());
            if (!hasIterVar) {
                ClavaLog.debug(
                        () -> "ForIterationsExpression: could not find iteration variable '" + iterVarName
                                + "' on left-hand side of step expression, " + binaryOp.getLhs());
                return Optional.empty();
            }

            BinaryOperatorKind kind = binaryOp.getOp();

            if (kind == BinaryOperatorKind.AddAssign) {
                return Optional.of(new StepData(ForStepDirection.INC, binaryOp.getRhs()));
            }

            if (kind == BinaryOperatorKind.SubAssign) {
                return Optional.of(new StepData(ForStepDirection.DEC, binaryOp.getRhs()));
            }

            ClavaLog.debug(
                    () -> "ForIterationsExpression: not implemented for a step with binary operator of kind " + kind);
            return Optional.empty();

        }

        // ++var
        // var++
        // --var
        // var--
        if (incExpr instanceof UnaryOperator) {
            UnaryOperator unaryOp = (UnaryOperator) incExpr;

            boolean hasIterVar = isVarReference(iterVarName, unaryOp.getSubExpr());
            if (!hasIterVar) {
                ClavaLog.debug(
                        () -> "ForIterationsExpression: could not find iteration variable '" + iterVarName
                                + "' on unary operator sub-expression, " + unaryOp.getSubExpr());
                return Optional.empty();
            }

            UnaryOperatorKind kind = unaryOp.getOp();

            if (kind == UnaryOperatorKind.PreInc || kind == UnaryOperatorKind.PostInc) {
                return Optional.of(new StepData(ForStepDirection.INC, unaryOp.getFactory().integerLiteral(1)));
            }

            if (kind == UnaryOperatorKind.PreDec || kind == UnaryOperatorKind.PostDec) {
                return Optional.of(new StepData(ForStepDirection.DEC, unaryOp.getFactory().integerLiteral(1)));
            }

            ClavaLog.debug(
                    () -> "ForIterationsExpression: not implemented for a step with unary operator of kind " + kind);
            return Optional.empty();
        }

        ClavaLog.debug(
                () -> "ForIterationsExpression: not implemented for this kind of step expression, " + incExpr);
        return Optional.empty();

    }

    private static boolean isVarReference(String iterVarName, Expr expr) {
        // Check if left-hand is iteration variable

        // Expecting decl ref expression
        if (!(expr instanceof DeclRefExpr)) {
            // ClavaLog.debug(() -> "ForIterationsExpression: left-hand side of step expression is not a DeclRefExpr, "
            // + expr);
            return false;
        }

        // Check name
        if (!iterVarName.equals(((DeclRefExpr) expr).getName())) {
            // ClavaLog.debug(() -> "ForIterationsExpression: step variable (" + ((DeclRefExpr) expr).getName()
            // + ") is not the same as the iteration variable (" + iterVarName + ")");
            return false;
        }
        return true;
    }
}
