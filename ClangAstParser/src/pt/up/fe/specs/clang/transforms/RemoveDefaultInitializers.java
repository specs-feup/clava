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

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.expr.CXXDefaultInitExpr;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Removes CXXCtorInitializer inside CXXConstructorDecls that have a default argument.
 * 
 * @author JoaoBispo
 *
 */
public class RemoveDefaultInitializers implements SimplePostClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof CXXConstructorDecl)) {
            return;
        }

        List<CXXCtorInitializer> initializers = ((CXXConstructorDecl) node).getInitializers();

        boolean hasChanges = false;
        List<CXXCtorInitializer> newInitializers = new ArrayList<>();
        for (CXXCtorInitializer initializer : initializers) {

            // Remove if default initialization
            if (initializer.get(CXXCtorInitializer.INIT_EXPR) instanceof CXXDefaultInitExpr) {
                hasChanges = true;
                // queue.delete(initializer);
                continue;
            }

            newInitializers.add(initializer);
            // if (initializer.getAncestor(TranslationUnit.class).getFilename().equals("BondMap.cpp")) {
            // System.out.println("KIND:" + initializer.getKind());
            // System.out.println("Init Expr:" + initializer.getInitExpr());
            // }
        }

        // If there are changes, set new initializers
        if (hasChanges) {
            node.set(CXXConstructorDecl.CONSTRUCTOR_INITS, newInitializers);
        }
    }

}
