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

import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.LiteralExpr;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class LoopTiling {

    public static boolean apply(LoopStmt targetLoop, String blockSize) {

        return apply(targetLoop, targetLoop, blockSize);
    }

    public static boolean apply(LoopStmt targetLoop, LoopStmt referenceLoop, String blockSize) {

        if (!test(targetLoop, referenceLoop)) {

            return false;
        }

        tile(targetLoop, referenceLoop, blockSize);

        return true;
    }

    private static void tile(LoopStmt targetLoop, LoopStmt referenceLoop, String blockSize) {

        ForStmt targetFor = (ForStmt) targetLoop;
        ForStmt referenceFor = (ForStmt) referenceLoop;

        String controlVarName = LoopAnalysisUtils.getControlVarNames(targetFor).get(0);
        String blockVarName = controlVarName + "_block"; // TODO: check for varaible name collisions

        changeTarget(targetFor, blockSize, blockVarName);
    }

    private static void changeTarget(ForStmt targetFor, String blockSize, String blockVarName) {

        changeInit(targetFor, blockVarName);
        changeCond(targetFor, blockVarName);
    }

    private static void changeCond(ForStmt targetFor, String blockVarName) {

        throw new NotImplementedException("anything beoynd change init is not done");
    }

    /**
     * @param targetFor
     * @param blockVarName
     */
    private static void changeInit(ForStmt targetFor, String blockVarName) {

        Stmt init = targetFor.getInit().get();

        LiteralExpr newRHS = ClavaNodeFactory.literalExpr(blockVarName, ClavaNodeFactory.builtinType("int"));
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
     * @param targetLoop
     *            the loop that will be tiled
     * @param referenceLoop
     *            the loop before which the new block-iterating loop will be introduced
     * @return true if we the transformation can be applied, false otherwise
     */
    public static boolean test(LoopStmt targetLoop, LoopStmt referenceLoop) {

        if (!(targetLoop instanceof ForStmt && referenceLoop instanceof ForStmt)) {

            return false;
        }

        if (!(targetLoop.isAncestor(referenceLoop) || targetLoop == referenceLoop)) {

            return false;
        }

        ForStmt targetFor = (ForStmt) targetLoop;

        if (!LoopAnalysisUtils.hasHeader(targetFor)) {

            return false;
        }

        if (LoopAnalysisUtils.getControlVarNames(targetFor).size() != 1) {
            return false;
        }

        if (!LoopAnalysisUtils.hasSimpleInit(targetFor)) {

            return false;
        }

        return true;
    }
}
