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

package pt.up.fe.specs.clava.transform.call;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Literal;
import pt.up.fe.specs.clava.ast.expr.ParenExpr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.classmap.ClassSet;

public class CallAnalysis {

    private final static ClassSet<ClavaNode> ALLOWED_NODES = new ClassSet<>();
    static {
        ALLOWED_NODES.add(Literal.class);
        ALLOWED_NODES.add(CompoundStmt.class);
        ALLOWED_NODES.add(Comment.class);
        ALLOWED_NODES.add(ExprStmt.class);
        ALLOWED_NODES.add(VarDecl.class);
        ALLOWED_NODES.add(ArraySubscriptExpr.class);
        ALLOWED_NODES.add(DeclRefExpr.class);
        ALLOWED_NODES.add(WrapperStmt.class);
        ALLOWED_NODES.add(DeclStmt.class);
        ALLOWED_NODES.add(BinaryOperator.class);
        ALLOWED_NODES.add(UnaryOperator.class);
        ALLOWED_NODES.add(LoopStmt.class);
        ALLOWED_NODES.add(ReturnStmt.class);
        ALLOWED_NODES.add(ParenExpr.class);
    }

    public CallAnalysis() {
        // TODO Auto-generated constructor stub
    }

    public static boolean canBeInlined(CompoundStmt compoundStmt) {
        Set<Class<? extends ClavaNode>> nodesInBody = compoundStmt.getDescendants().stream()
                .map(node -> node.getClass())
                .collect(Collectors.toSet());

        // Check if all nodes are allowed
        Set<String> unsupportedNodes = nodesInBody.stream()
                .filter(node -> !ALLOWED_NODES.contains(node))
                .map(node -> node.getSimpleName())
                .collect(Collectors.toSet());

        if (!unsupportedNodes.isEmpty()) {
            SpecsLogs.msgInfo("Cannot inline function which contains the following nodes: " + unsupportedNodes);
            return false;
        }

        return true;
    }

    public static boolean hasMultipleReturnPoints(CompoundStmt compoundStmt) {
        // Check that is has at most one return statement
        List<ReturnStmt> returnStmts = compoundStmt.getDescendantsStream()
                .filter(ReturnStmt.class::isInstance)
                .map(ReturnStmt.class::cast)
                .collect(Collectors.toList());

        // More than one return statement
        if (returnStmts.size() > 1) {
            SpecsLogs.msgInfo("CallAnalysis: found " + returnStmts.size() + " return statements");
            return true;
        }

        // If no return statements, return false
        if (returnStmts.isEmpty()) {
            return false;
        }

        // One return statement, check that it is the last statement (excluding wrapper statements)
        boolean isReturnLastStmt = SpecsCollections.reverseStream(compoundStmt.getChildren())
                .filter(stmt -> !(stmt instanceof WrapperStmt))
                .findFirst()
                // Use equal, should be the same object
                .map(returnStmt -> returnStmt == returnStmts.get(0))
                // If no return statement found, consider it is inside control flow block
                .orElse(false);

        if (!isReturnLastStmt) {
            SpecsLogs.msgInfo("CallAnalysis: has one return statement, but is not the last statement");
            return true;
        }

        return false;

    }

}
