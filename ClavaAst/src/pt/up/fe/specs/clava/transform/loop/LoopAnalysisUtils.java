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

import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprUse;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
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

            if (binOp.getOp() != BinaryOperatorKind.ASSIGN) {

                return false;
            }
        }

        return true;
    }

    public static List<String> getControlVarNames(ForStmt targetFor) {

        /*
        if (!hasSimpleInit(targetFor)) {
        
            return Optional.empty();
        }
        
        Stmt init = targetFor.getInit().get();
        
        // if it is an assignment, get the LHS
        if (init instanceof ExprStmt) {
        
            Expr expr = ((ExprStmt) init).getExpr();
            BinaryOperator binOp = ((BinaryOperator) expr);
        
            return Optional.of(binOp.getLhs().toString());
        }
        
        // if it is a declaration, get the name of the declared variable
        if (init instanceof DeclStmt) {
        
            VarDecl decl = (VarDecl) ((DeclStmt) init).getDecls().get(0);
        
            return Optional.of(decl.getDeclName());
        }
        
        throw new RuntimeException("init was neither an ExprStmt nor a DeclStmt");
        */

        Stmt inc = targetFor.getInc().orElse(null);
        if (inc == null) {
            return null;
        }

        List<String> controlVars = inc.getDescendants(DeclRefExpr.class).stream()
                .filter(d -> d.use() == ExprUse.READWRITE || d.use() == ExprUse.WRITE).map(DeclRefExpr::getRefName)
                .collect(Collectors.toList());

        return controlVars;
    }
}
