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

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.data.IdNormalizer;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CallInliner {

    private final FunctionExtractorForInlining functionExtractor;
    // private final CallExpr call;

    public CallInliner() {
        this(null);
    }

    public CallInliner(IdNormalizer idNormalizer) {
        // this.call = call;
        this.functionExtractor = new FunctionExtractorForInlining(idNormalizer);
    }

    private void failureMsg(CallExpr call, String message) {
        ClavaLog.info(
                "Could not inline call " + call.getCalleeName() + "@line " + call.getLocation().getStartLine() + ": "
                        + message);
    }

    public boolean inline(CallExpr call) {

        // Check that call is inside a function declaration
        if (!call.getAncestorTry(FunctionDecl.class).isPresent()) {
            failureMsg(call, "does not support calls that are not inside function declarations");
            return false;
        }

        // Get definition
        FunctionDecl functionDecl = call.getDefinition().orElse(null);
        if (functionDecl == null) {
            failureMsg(call, "could not find source code of function declaration");
            return false;
        }

        if (!functionDecl.hasBody()) {
            failureMsg(call, "function declaration does not have a body");
            return false;
        }

        // Obtain list of statements to inline
        List<Stmt> functionStmts = functionExtractor.extractFunction(functionDecl).orElse(null);
        if (functionStmts == null) {
            failureMsg(call, "could not extract statements from function");
            return false;
        }

        // Copy statements, variables will be renamed
        List<Stmt> copiedStmts = functionStmts.stream()
                .map(Stmt::copy)
                .collect(Collectors.toList());

        // String stmtsCode = functionStmts.stream().map(Stmt::getCode).collect(Collectors.joining("\n"));
        // if (copiedStmts.size() == 1 && copiedStmts.get(0).getCode().contains("return a + a;")) {
        // System.out.println("ORIGINAL STMT:" + functionStmts.get(0).getCode());
        // System.out.println("COPY STMT:" + copiedStmts.get(0).getCode());
        // System.out.println("TREE:" + copiedStmts.get(0).toTree());
        //
        // copiedStmts.get(0).getDescendants(DeclRefExpr.class).get(0).set(DeclRefExpr.DECL_NAME, "newName");
        // System.out.println("ORIGINAL STMT AFTER:" + functionStmts.get(0).getCode());
        // System.out.println("COPY STMT AFTER:" + copiedStmts.get(0).getCode());
        //
        // }

        // Inline calls inside the statements (function extractor guarantees that has no recursive calls)
        // TODO

        // Collect used names, for checking if any of the renames clashes with already existing names
        // TODO can be cached also?
        Set<String> usedNames = getUsedNames(call);

        // if (stmtsCode.contains("(*vx)[x][5]")) {
        // System.out.println("ORIGINAL STMTS:\n");
        // System.out.println(functionStmts.stream().map(Stmt::toTree).collect(Collectors.joining("\n")));
        // }

        // if (stmtsCode.contains("(*vx)[x][5]")) {
        // System.out.println("COPY STMTS:\n");
        // System.out.println(copiedStmts.stream().map(Stmt::toTree).collect(Collectors.joining("\n")));
        // }

        // Build rename map
        InlineRenamer inlineRenamer = new InlineRenamer(call, functionDecl, copiedStmts, usedNames);
        // if (stmtsCode.contains("(*vx)[x][5]")) {
        // System.out.println("ORIGINAL STMTS BEFORE:\n");
        // System.out.println(functionStmts.stream().map(Stmt::getCode).collect(Collectors.joining("\n")));
        // }

        List<Stmt> modifiedStmts = inlineRenamer.apply();

        // if (stmtsCode.contains("(*vx)[x][5]")) {
        // System.out.println("ORIGINAL STMTS AFTER:\n");
        // System.out.println(functionStmts.stream().map(Stmt::getCode).collect(Collectors.joining("\n")));
        // }

        // Insert all stmts before call
        Stmt callStmt = call.getAncestor(Stmt.class);
        modifiedStmts.stream().forEach(stmt -> NodeInsertUtils.insertBefore(callStmt, stmt));

        // If parent of call is an ExprStmt, call is no longer needed and can be removed
        ClavaNode callParent = call.getParent();
        if (callParent instanceof ExprStmt) {
            NodeInsertUtils.delete(callParent);
            return true;
        }

        // At this point, there must be a replacement expression
        Expr callReplacement = inlineRenamer.getCallReplacement()
                .orElseThrow(() -> new RuntimeException(
                        "Parent of call is not an ExprStmt, expected replacement expression to exist. Call parent:\n"
                                + callParent));

        // Replace call
        NodeInsertUtils.replace(call, callReplacement);
        return true;
        /*        
        System.out.println("CALL PARENT:" + call.getParent());
        Expr callReplacement = inlineRenamer.getCallReplacement().orElse(null);
        if (callReplacement != null) {
        
            return true;
        }
        
        // If there is a replacement node, use it instead of call; otherwise, just remove it
        
        Stmt callParent = call.getAncestor(Stmt.class);
        // If no replacement node, call must be inside a ExprStmt
        Preconditions.checkArgument(callParent instanceof ExprStmt,
                "Expected parent statement of call to be an ExprStmt, it is a " + callParent.getNodeName());
        
        // NodeInsertUtils.delete(callParent);
        
        return true;
        */
    }

    private Set<String> getUsedNames(CallExpr call) {
        // Get declaration where this call is
        FunctionDecl functionDecl = call.getAncestor(FunctionDecl.class);

        Set<String> usedNames = functionDecl.getDescendantsStream()
                .filter(node -> node instanceof VarDecl || node instanceof DeclRefExpr)
                .map(node -> {
                    if (node instanceof VarDecl) {
                        return ((VarDecl) node).getDeclName();
                    }

                    if (node instanceof DeclRefExpr) {
                        return ((DeclRefExpr) node).getRefName();
                    }

                    throw new RuntimeException("Case not implemented: " + node.getClass());
                })
                .collect(Collectors.toSet());

        return usedNames;
    }

}
