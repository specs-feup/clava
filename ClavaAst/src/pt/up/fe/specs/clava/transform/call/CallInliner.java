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
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.data.IdNormalizer;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.Preconditions;
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
        System.out.println("TRYING TO INLINE");

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

        // Inline calls inside the statements (function extractor guarantees that has no recursive calls)
        // TODO

        // Collect used names, for checking if any of the renames clashes with already existing names
        // TODO can be cached also?
        Set<String> usedNames = getUsedNames(call);

        // Build rename map
        InlineRenamer inlineRenamer = new InlineRenamer(call, functionDecl, copiedStmts, usedNames);
        List<Stmt> modifiedStmts = inlineRenamer.apply();

        // Insert all stmts before call
        Stmt callStmt = call.getAncestor(Stmt.class);
        modifiedStmts.stream().forEach(stmt -> NodeInsertUtils.insertBefore(callStmt, stmt));

        // If there is a replacement node, use it instead of call; otherwise, just remove it
        Expr callReplacement = inlineRenamer.getCallReplacement().orElse(null);
        if (callReplacement != null) {
            NodeInsertUtils.replace(call, callReplacement);
        } else {
            Stmt callParent = call.getAncestor(Stmt.class);
            // If no replacement node, call must be inside a ExprStmt
            Preconditions.checkArgument(callParent instanceof ExprStmt,
                    "Expected parent statement of call to be an ExprStmt, it is a " + callParent.getNodeName());

            NodeInsertUtils.delete(callParent);
        }

        return true;
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
