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

package pt.up.fe.specs.clang.transforms;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.stmt.ClangLabelStmt;
import pt.up.fe.specs.clava.ast.stmt.LabelStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Extracts the sub-statement from ClangLabelStmt nodes and replaces the nodes with LabelStmt nodes.
 * 
 * @author JoaoBispo
 *
 */
public class ReplaceClangLabelStmt implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
	if (!(node instanceof ClangLabelStmt)) {
	    return;
	}

	ClangLabelStmt clangLabel = (ClangLabelStmt) node;
	Stmt subStmt = clangLabel.getSubStmt();

	// Remove sub-statement from label
	Preconditions.checkArgument(clangLabel.removeChild(subStmt) != -1);

	LabelStmt labelStmt = ClavaNodeFactory.labelStmt(clangLabel.getLabel(), clangLabel.getInfo());

	// Replace ClangLabel
	queue.replace(clangLabel, labelStmt);

	// Insert sub-statement after label
	queue.moveAfter(labelStmt, subStmt);
    }

}
