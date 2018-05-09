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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceAliasDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceDecl;
import pt.up.fe.specs.clava.ast.decl.TypedefDecl;
import pt.up.fe.specs.clava.ast.decl.UsingDirectiveDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Clang AST puts global Decl nodes directly in the tree outside of functions, this transformation encapsulates such
 * Decls inside a DeclStmt.
 * 
 * @author Jo�o Bispo
 *
 */
public class CreateDeclStmts implements SimplePostClavaRule {

    // Create statements for the following Decls
    private static final Set<Class<? extends NamedDecl>> DECL_CLASSES = new HashSet<>(Arrays.asList(
            VarDecl.class,
            TypedefDecl.class,
            UsingDirectiveDecl.class,
            NamespaceAliasDecl.class));

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        // Find TranslationUnits
        boolean isAllowedInstance = node instanceof TranslationUnit || node instanceof CXXRecordDecl;
        if (!isAllowedInstance) {
            return;
        }

        // Test all direct children of translation unit
        createDeclStmts(node.getChildren(), queue);

        // Create Stmt for each Decl of allowed type
        // for (Class<? extends NamedDecl> declClass : DECL_CLASSES) {
        // for (NamedDecl decl : node.getChildrenV2(declClass)) {
        // DeclStmt stmt = ClavaNodeFactory.declStmt(decl.getInfo(), Arrays.asList(decl));
        //
        // queue.replace(decl, stmt);
        // }
        // }
        // for (VarDecl decl : node.getChildrenV2(VarDecl.class)) {
        // DeclStmt stmt = ClavaNodeFactory.declStmt(decl.getInfo(), Arrays.asList(decl));
        //
        // queue.replace(decl, stmt);
        // }

    }

    private void createDeclStmts(List<ClavaNode> children, TransformQueue<ClavaNode> queue) {
        for (ClavaNode child : children) {
            // If NameSpace, create DeclStmts on its children
            if (child instanceof NamespaceDecl) {
                createDeclStmts(child.getChildren(), queue);
                continue;
            }

            if (DECL_CLASSES.contains(child.getClass())) {
                // Nodes on the set are NamedDecls
                NamedDecl namedDecl = (NamedDecl) child;
                DeclStmt stmt = namedDecl.getFactoryWithNode()
                        .declStmt(namedDecl);
                // DeclStmt stmt = ClavaNodeFactory.declStmt(namedDecl.getInfo(), Arrays.asList(namedDecl));

                queue.replace(child, stmt);
            }
        }
    }

}
