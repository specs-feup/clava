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

package pt.up.fe.specs.clang.transforms;

import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.MSPropertyDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.expr.CUDAKernelCallExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.MSPropertyRefExpr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Some CUDA properties can have different names that in the source (e.g. __fetch_builtin_x, instead of x), this pass
 * tries to revert those cases.
 * 
 * @author João Bispo
 *
 */
public class ProcessCudaNodes implements SimplePostClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        // TODO: Use Rule that stops if finds a TranslationUnit that is not a CUDA file
        var isCudaFile = node.getAncestorTry(TranslationUnit.class)
                .map(tu -> tu.isCUDAFile())
                .orElse(false);

        if (!isCudaFile) {
            return;
        }
        // if (get(DECL_NAME).equals("__cuda_builtin_threadIdx_t")) {
        // System.out.println("DECL: " + this.getClass());
        // }
        if (node instanceof CXXRecordDecl) {
            if (node.get(CXXRecordDecl.DECL_NAME).equals("__cuda_builtin_threadIdx_t")) {
                System.out.println("ASDADASDASDASD");
            }
        }

        apply(node);
    }

    private void apply(ClavaNode node) {

        if (node instanceof MSPropertyRefExpr) {
            process((MSPropertyRefExpr) node);
            return;
        }

        if (node instanceof NamedDecl) {
            var declName = node.get(NamedDecl.DECL_NAME);
            var processedDeclName = processName(declName);
            node.set(NamedDecl.DECL_NAME, processedDeclName);

            // Remove qualified prefix
            processQualifiedPrefix((NamedDecl) node);

            if (node instanceof CXXMethodDecl) {
                apply(node.get(CXXMethodDecl.RECORD));
            }

            return;
        }

        if (node instanceof CallExpr) {
            node.get(CallExpr.DIRECT_CALLEE).ifPresent(function -> apply(function));

            // CUDAKernelCall does not have DIRECT_CALLEE set
            // If not set, mean that it is the child, so it is in the AST and will be traversed
            if (node instanceof CUDAKernelCallExpr) {
                var directCallee = node.get(CallExpr.DIRECT_CALLEE);

                if (directCallee.isPresent()) {
                    return;
                }
                // System.out.println("NO DIRECT CALLEE:: " + node);

                var callExpr = (CallExpr) node;
                // System.out.println("TRY: " + callExpr.getCalleeDeclRefTry());
                // System.out.println("CALLEE: " + callExpr.getCallee());

                var callee = callExpr.getCallee();
                if (!(callee instanceof DeclRefExpr)) {
                    return;
                }

                var decl = callee.get(DeclRefExpr.DECL);

                if (!(decl instanceof FunctionDecl)) {
                    return;
                }

                node.set(CallExpr.DIRECT_CALLEE, Optional.of((FunctionDecl) decl));
                // System.out.println("FDECL: " + fDecl);
                // var decl = callExpr.getCalleeDeclRefTry()
                // .flatMap(declRef -> declRef.getDecl().map(fdecl -> (FunctionDecl) fdecl));
                // System.out.println("DECK; " + decl);

            }

            return;
        }
    }

    private void processQualifiedPrefix(NamedDecl node) {
        var prefix = node.get(NamedDecl.QUALIFIED_PREFIX);

        var processedPrefix = processName(prefix);

        // No diff, maintain
        if (processedPrefix.equals(prefix)) {
            return;
        }

        // If diff, remove
        node.set(NamedDecl.QUALIFIED_PREFIX, "");
    }

    private void process(MSPropertyRefExpr node) {
        // var isCudaFile = node.getAncestor(TranslationUnit.class).isCUDAFile();
        //
        // if (!isCudaFile) {
        // return;
        // }

        process(node.get(MSPropertyRefExpr.PROPERTY_DECL));
    }

    private void process(MSPropertyDecl node) {
        // Check properties
        process(node, MSPropertyDecl.GETTER_NAME);
        process(node, MSPropertyDecl.SETTER_NAME);
    }

    private void process(MSPropertyDecl node, DataKey<Optional<String>> propKey) {
        var prop = node.get(propKey);

        if (prop.isEmpty()) {
            return;
        }

        var name = prop.get();

        // Process built-ins
        name = processName(name);

        node.set(propKey, Optional.of(name));
    }

    private String processName(String name) {
        if (name.startsWith("__fetch_builtin_")) {
            name = name.substring("__fetch_builtin_".length());
        }

        if (name.startsWith("__cuda_builtin_")) {
            name = name.substring("__cuda_builtin_".length());
        }

        return name;
    }

}
