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

import java.math.BigDecimal;
import java.math.BigInteger;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.enums.FloatKind;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Transformation that applies constant folding to an arbitrary AST Works with
 * both integers and floating point
 * 
 * @author Tiago
 *
 */
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
	    String l1 = ((FloatingLiteral) lhs).getLiteral();
	    String l2 = ((FloatingLiteral) rhs).getLiteral();
	    l1 = l1.replaceAll("(?i)f", "");
	    l2 = l2.replaceAll("(?i)f", "");
	    queue.replace(node, performFloatOperation(new BigDecimal(l1), new BigDecimal(l2), op));
	}
	if ((lhs instanceof FloatingLiteral) && (rhs instanceof IntegerLiteral)) {
	    String l1 = ((FloatingLiteral) lhs).getLiteral();
	    String l2 = ((IntegerLiteral) rhs).getLiteral();
	    l1 = l1.replaceAll("(?i)f", "");
	    queue.replace(node, performFloatOperation(new BigDecimal(l1), new BigDecimal(l2), op));
	}
	if ((lhs instanceof IntegerLiteral) && (rhs instanceof FloatingLiteral)) {
	    String l1 = ((IntegerLiteral) lhs).getLiteral();
	    String l2 = ((FloatingLiteral) rhs).getLiteral();
	    l2 = l2.replaceAll("(?i)f", "");
	    queue.replace(node, performFloatOperation(new BigDecimal(l1), new BigDecimal(l2), op));
	}
	if ((lhs instanceof IntegerLiteral) && (rhs instanceof IntegerLiteral)) {
	    String l1 = ((IntegerLiteral) lhs).getLiteral();
	    String l2 = ((IntegerLiteral) rhs).getLiteral();
	    queue.replace(node, performIntegerOperation(new BigInteger(l1), new BigInteger(l2), op));
	}
    }

    /**
     * Creates a FloatingIntegral by calculating an expression involving two
     * floating point numbers and an arithmetic operation
     * 
     * @param l1
     * @param l2
     * @param op
     * @return a FloatingLiteral node with the result
     */
    private FloatingLiteral performFloatOperation(BigDecimal l1, BigDecimal l2, BinaryOperator op) {
	String operator = op.getOperatorCode();
	BigDecimal res = new BigDecimal("0");
	switch (operator) {
	case "-": {
	    res = l1.subtract(l2);
	    break;
	}
	case "+": {
	    res = l1.add(l2);
	    break;
	}
	case "/": {
	    res = l1.divide(l2);
	    break;
	}
	case "*": {
	    res = l1.multiply(l2);
	    break;
	}
	}
	FloatingLiteral lit = op.getFactory().floatingLiteral(FloatKind.LONG_DOUBLE, res.doubleValue());
	return lit;
    }

    /**
     * Creates an IntegerIntegral by calculating an expression involving two
     * integers and an arithmetic operation
     * 
     * @param l1
     * @param l2
     * @param op
     * @return an IntegerIntegral node with the result
     */
    private IntegerLiteral performIntegerOperation(BigInteger l1, BigInteger l2, BinaryOperator op) {
	String operator = op.getOperatorCode();
	BigInteger res = new BigInteger("0");
	switch (operator) {
	case "-": {
	    res = l1.subtract(l2);
	    break;
	}
	case "+": {
	    res = l1.add(l2);
	    break;
	}
	case "/": {
	    res = l1.divide(l2);
	    break;
	}
	case "*": {
	    res = l1.multiply(l2);
	    break;
	}
	}
	IntegerLiteral lit = op.getFactory().integerLiteral(res);
	return lit;
    }
}
