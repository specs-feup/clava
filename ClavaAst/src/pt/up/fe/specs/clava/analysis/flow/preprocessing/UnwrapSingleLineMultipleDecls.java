/**
 *  Copyright 2020 SPeCS.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package pt.up.fe.specs.clava.analysis.flow.preprocessing;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Transformation that unwraps multiple declarations in a single statement,
 * e.g., int x, y, z = 2; becomes int x; int y; int z = 2;
 * 
 * @author Tiago
 *
 */
public class UnwrapSingleLineMultipleDecls implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
	if (!(node instanceof DeclStmt))
	    return;
	DeclStmt decl = (DeclStmt) node;
	ClavaNode parent = decl.getParent();

	for (VarDecl varDecl : decl.getVarDecls()) {
	    DeclStmt newDecl = parent.getFactory().declStmt(varDecl);
	    queue.addChildHead(parent, newDecl);
	}
	queue.delete(decl);
    }

}
