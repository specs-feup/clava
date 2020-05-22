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

import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

public class FlowAnalysisPreprocessing {
    private CompoundStmt body;

    public FlowAnalysisPreprocessing(CompoundStmt body) {
	this.body = body;
    }

    public void applyConstantFolding() {
	ConstantFolding cf = new ConstantFolding();
	TransformQueue<ClavaNode> queue = new TransformQueue<>("Constant Folding Queue");
	int detected = 0;
	do {
	    detected = 0;
	    List<BinaryOperator> ops = body.getDescendants(BinaryOperator.class);
	    for (BinaryOperator op : ops) {
		Expr lhs = op.getLhs();
		Expr rhs = op.getRhs();
		if (isLiteral(lhs) && isLiteral(rhs)) {
		    detected++;
		    cf.apply(op, queue);
		}
	    }
	    queue.apply();
	} while (detected != 0);
    }

    private boolean isLiteral(Expr expr) {
	return ((expr instanceof IntegerLiteral) || (expr instanceof FloatingLiteral));
    }

    public void applyUnwrapSingleLineMultipleDecls() {
	UnwrapSingleLineMultipleDecls unwrap = new UnwrapSingleLineMultipleDecls();
	TransformQueue<ClavaNode> queue = new TransformQueue<>("Unwrapping Decls Queue");
	List<DeclStmt> decls = body.getDescendants(DeclStmt.class);
	for (DeclStmt decl : decls) {
	    if (decl.getNumChildren() > 1) {
		unwrap.apply(decl, queue);
	    }
	}
	queue.apply();
    }
}
