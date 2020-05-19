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
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.enums.FloatKind;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

public class ConstantFolding implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
	if (!(node instanceof BinaryOperator))
	    return;
	BinaryOperator op = (BinaryOperator) node;
	// Assumes that check about whether children are both literals is made elsewhere
	Expr lhs = op.getLhs();
	Expr rhs = op.getRhs();

	if ((lhs instanceof FloatingLiteral) && (rhs instanceof FloatingLiteral)) {
	    float l1 = Float.parseFloat(((FloatingLiteral) lhs).getLiteral());
	    float l2 = Float.parseFloat(((FloatingLiteral) rhs).getLiteral());
	    queue.replace(node, performFloatOperation(l1, l2, op));
	}
	if ((lhs instanceof FloatingLiteral) && (rhs instanceof IntegerLiteral)) {
	    float l1 = Float.parseFloat(((FloatingLiteral) lhs).getLiteral());
	    float l2 = Integer.parseInt(((IntegerLiteral) rhs).getLiteral());
	    queue.replace(node, performFloatOperation(l1, l2, op));
	}
	if ((lhs instanceof IntegerLiteral) && (rhs instanceof FloatingLiteral)) {
	    float l1 = Integer.parseInt(((IntegerLiteral) lhs).getLiteral());
	    float l2 = Float.parseFloat(((FloatingLiteral) rhs).getLiteral());
	    queue.replace(node, performFloatOperation(l1, l2, op));
	}
	if ((lhs instanceof IntegerLiteral) && (rhs instanceof IntegerLiteral)) {
	    int l1 = Integer.parseInt(((IntegerLiteral) lhs).getLiteral());
	    int l2 = Integer.parseInt(((IntegerLiteral) rhs).getLiteral());
	    queue.replace(node, performIntegerOperation(l1, l2, op));
	}
    }

    private FloatingLiteral performFloatOperation(float l1, float l2, BinaryOperator op) {
	String operator = op.getOperatorCode();
	float res = 0;
	switch (operator) {
	case "-": {
	    res = l1 - l2;
	    break;
	}
	case "+": {
	    res = l1 + l2;
	    break;
	}
	case "/": {
	    res = l1 / l2;
	    break;
	}
	case "*": {
	    res = l1 * l2;
	    break;
	}
	}
	FloatingLiteral lit = op.getFactory().floatingLiteral(FloatKind.FLOAT, res);
	return lit;
    }

    private IntegerLiteral performIntegerOperation(int l1, int l2, BinaryOperator op) {
	String operator = op.getOperatorCode();
	int res = 0;
	switch (operator) {
	case "-": {
	    res = l1 - l2;
	    break;
	}
	case "+": {
	    res = l1 + l2;
	    break;
	}
	case "/": {
	    res = l1 / l2;
	    break;
	}
	case "*": {
	    res = l1 * l2;
	    break;
	}
	}
	IntegerLiteral lit = op.getFactory().integerLiteral(res);
	return lit;
    }
}
