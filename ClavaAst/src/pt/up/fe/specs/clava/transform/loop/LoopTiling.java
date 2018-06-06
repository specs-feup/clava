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

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.LiteralExpr;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class LoopTiling {

    private final ClavaContext context;
    private final ClavaFactory factory;

    public LoopTiling(ClavaContext context) {
        this.context = context;
        this.factory = context.get(ClavaContext.FACTORY);
    }

    public boolean apply(LoopStmt targetLoop, String blockSize) {

        return apply(targetLoop, targetLoop, blockSize);
    }

    public boolean apply(LoopStmt targetLoop, LoopStmt referenceLoop, String blockSize) {

        return apply(targetLoop, referenceLoop, blockSize, true);
    }

    public boolean apply(LoopStmt targetLoop, LoopStmt referenceLoop, String blockSize, boolean useTernary) {

        if (!test(targetLoop, referenceLoop)) {

            return false;
        }

        tile(targetLoop, referenceLoop, blockSize, useTernary);

        return true;
    }

    private void tile(LoopStmt targetLoop, LoopStmt referenceLoop, String blockSize, boolean useTernary) {

        ForStmt targetFor = (ForStmt) targetLoop;
        ForStmt referenceFor = (ForStmt) referenceLoop;

        String controlVarName = LoopAnalysisUtils.getControlVarNames(targetFor).get(0);
        String blockVarName = controlVarName + "_block"; // TODO: check for variable name collisions

        // test guarantees there is a lower bound
        Expr oldLowerBound = LoopAnalysisUtils.getLowerBound(targetFor).get();

        // test guarantees there is an upper bound
        Expr oldUpperBound = LoopAnalysisUtils.getUpperBound(targetFor).get();

        changeTarget(targetFor, blockSize, blockVarName, oldUpperBound, useTernary);
        addBlockLoop(targetFor, referenceFor, blockSize, blockVarName, oldLowerBound, oldUpperBound);
    }

    /**
     *
     * Note: for now it's based on literal statements, which means that the elements of the statements are not present
     * in the tree and cannot be iterated through.
     *
     * @param targetFor
     * @param referenceFor
     * @param blockSize
     * @param blockVarName
     * @param oldUpperBound
     */
    private void addBlockLoop(ForStmt targetFor, ForStmt referenceFor, String blockSize, String blockVarName,
            Expr oldLowerBound, Expr oldUpperBound) {

        // make header parts
        // Stmt init = ClavaNodeFactory.literalStmt("size_t " + blockVarName + " = " + oldLowerBound.getCode() + ";");
        Stmt init = ClavaNodeFactory.literalStmt("int " + blockVarName + " = " + oldLowerBound.getCode() + ";");
        Stmt cond = ClavaNodeFactory.literalStmt(blockVarName + " < " + oldUpperBound.getCode() + ";");
        Stmt inc = ClavaNodeFactory.literalStmt(blockVarName + " += " + blockSize);

        // make for loop
        CompoundStmt emptyBody = ClavaNodeFactory.compoundStmt(ClavaNodeInfo.undefinedInfo(), Collections.emptyList());
        ForStmt newFor = ClavaNodeFactory.forStmt(ClavaNodeInfo.undefinedInfo(), init, cond, inc, emptyBody);

        // add loop as parent
        NodeInsertUtils.replace(referenceFor, newFor, true);
        newFor.getBody().addChild(referenceFor);
    }

    private void changeTarget(ForStmt targetFor, String blockSize, String blockVarName, Expr oldUpperBound,
            boolean useTernary) {

        changeInit(targetFor, blockVarName);
        changeCond(targetFor, blockSize, blockVarName, oldUpperBound, useTernary);
    }

    private void changeCond(ForStmt targetFor, String blockSize, String blockVarName, Expr oldUpperBound,
            boolean useTernary) {

        Type oldUpperBoundType = oldUpperBound.getType();
        String oldUpperBoundCode = oldUpperBound.getCode();

        Expr newLimit = null;
        String blockLimit = blockVarName + " + " + blockSize;

        if (useTernary) {

            String newLimitCode = "(" + oldUpperBoundCode + " < " + blockLimit + " ? " + oldUpperBoundCode + " : "
                    + blockLimit + ")";
            newLimit = ClavaNodeFactory.literalExpr(newLimitCode, oldUpperBoundType);
        } else {

            // add limit check and limit variable before the target loop
            String limitVar = makeLimitCheck(targetFor, oldUpperBoundType, oldUpperBoundCode, blockLimit);

            // change the limit code in the target loop
            newLimit = ClavaNodeFactory.literalExpr(limitVar, oldUpperBoundType);
        }

        NodeInsertUtils.replace(oldUpperBound, newLimit);
    }

    private String makeLimitCheck(ForStmt targetFor, Type oldUpperBoundType, String oldUpperBoundCode,
            String blockLimit) {

        List<String> controlVars = LoopAnalysisUtils.getControlVarNames(targetFor);
        String controlVar = controlVars.get(0);
        String limitVar = controlVar + "_limit";
        String limitVarDecl = oldUpperBoundType.unqualifiedType().getCode(targetFor) + " " + limitVar + " = "
                + blockLimit + ";";
        String limitCheck = "if(" + limitVar + " > " + oldUpperBoundCode + ")" + limitVar + " = "
                + oldUpperBoundCode + ";";

        String limitDeclCode = limitVarDecl + limitCheck;
        Stmt limitDecl = ClavaNodeFactory.literalStmt(limitDeclCode);
        NodeInsertUtils.insertBefore(targetFor, limitDecl);

        return limitVar;
    }

    /**
     * @param targetFor
     * @param blockVarName
     */
    private void changeInit(ForStmt targetFor, String blockVarName) {

        Stmt init = targetFor.getInit().get();

        // LiteralExpr newRHS = ClavaNodeFactory.literalExpr(blockVarName, ClavaNodeFactory.builtinType("int"));
        // LiteralExpr newRHS = ClavaNodeFactory.literalExpr(blockVarName,
        // ClavaNodeFactory.builtinType(BuiltinKind.INT));

        LiteralExpr newRHS = factory.literalExpr(blockVarName, factory.builtinType(BuiltinKind.Int));

        if (init instanceof ExprStmt) {

            Expr expr = ((ExprStmt) init).getExpr();
            BinaryOperator binOp = ((BinaryOperator) expr);

            NodeInsertUtils.replace(binOp.getRhs(), newRHS);

            return;
        }

        if (init instanceof DeclStmt) {

            // init has a single statement guaranteed by test()
            VarDecl decl = (VarDecl) ((DeclStmt) init).getDecls().get(0);
            decl.setInit(newRHS);

            return;
        }

        throw new RuntimeException("init was neither an ExprStmt nor a DeclStmt");
    }

    /**
     * Tests whether the transformation can be applied. The following conditions must be met:
     *
     * 1) Both loops are FOR loops
     *
     * 2) targetLoop contains init, cond and inc statements
     *
     * 3) init is a simple assignment ot the control variable (e.g., i = 0)
     *
     * 4) cond is a simple binary comparison (e.g., i < SIZE)
     *
     * 5) inc is a simple increment (e.g., i++ or i+=1)
     *
     * 6) referenceLoop is an ancestor of targetLoop or the same loop
     *
     * 7) no complex control flow inside the loop (break, continue, return and goto)
     *
     * @param targetLoop
     *            the loop that will be tiled
     * @param referenceLoop
     *            the loop before which the new block-iterating loop will be introduced
     * @return true if we the transformation can be applied, false otherwise
     */
    public boolean test(LoopStmt targetLoop, LoopStmt referenceLoop) {

        // 1
        if (!(targetLoop instanceof ForStmt && referenceLoop instanceof ForStmt)) {

            return false;
        }

        // 6
        if (!(targetLoop.isAncestor(referenceLoop) || targetLoop == referenceLoop)) {

            return false;
        }

        // 2
        ForStmt targetFor = (ForStmt) targetLoop;

        if (!LoopAnalysisUtils.hasHeader(targetFor)) {

            return false;
        }

        // test if we can get a control variable
        if (LoopAnalysisUtils.getControlVarNames(targetFor).size() != 1) {
            return false;
        }

        // 3
        if (!LoopAnalysisUtils.hasSimpleInit(targetFor)) {

            return false;
        }

        // 4
        if (!LoopAnalysisUtils.hasSimpleCond(targetFor)) {

            return false;
        }

        // 5
        if (!LoopAnalysisUtils.hasSimpleInc(targetFor)) {

            return false;
        }

        // 7
        if (LoopAnalysisUtils.hasComplexControlFlow(targetFor)) {

            return false;
        }

        return true;
    }
}
