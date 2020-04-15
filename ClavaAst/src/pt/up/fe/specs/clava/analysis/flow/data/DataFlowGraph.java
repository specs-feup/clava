/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.analysis.flow.data;

import java.util.ArrayList;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowGraph;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockNode;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockNodeType;
import pt.up.fe.specs.clava.analysis.flow.control.ControlFlowGraph;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

public class DataFlowGraph extends FlowGraph {
    private ControlFlowGraph cfg;
    private ArrayList<Integer> processed = new ArrayList<>();
    private int tempCounter = 0;

    public DataFlowGraph(FunctionDecl func, Stmt beginning, Stmt end) {
	this(func.getBody().get(), beginning, end);
    }

    public DataFlowGraph(CompoundStmt body, Stmt beginning, Stmt end) {
	super("Data-flow Graph - " + ((FunctionDecl) body.getParent()).getDeclName());
	this.cfg = new ControlFlowGraph(body);
	// this.beginning = beginning;
	// this.end = end;

	BasicBlockNode start = findStartingRegion();

	buildGraph(start);
    }

    @Override
    protected String buildDot() {
	StringBuilder sb = new StringBuilder();
	String NL = "\n";
	sb.append("Digraph G {").append(NL);
	for (FlowNode node : getNodes()) {
	    sb.append(node.toDot()).append(NL);
	}
	for (FlowEdge edge : edges) {
	    sb.append(edge.toDot()).append(NL);
	}
	sb.append("}").append(NL);
	return sb.toString();
    }

    private BasicBlockNode findStartingRegion() {
	ArrayList<FlowNode> blocks = cfg.getNodes();

	int min = Integer.MAX_VALUE;
	BasicBlockNode start = null;
	for (FlowNode block : blocks) {
	    BasicBlockNode b = (BasicBlockNode) block;
	    if (b.getType() == BasicBlockNodeType.LOOP) {
		if (b.getId() < min) {
		    min = b.getId();
		    start = b;
		}
	    }
	}
	return start;
    }

    private ArrayList<DataFlowNode> buildGraph(BasicBlockNode currBlock) {
	processed.add(currBlock.getId());

	// Build graph for each statement of basic block
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	if (currBlock.getType() == BasicBlockNodeType.LOOP) {
	    nodes.add(buildLoopNode(currBlock));
	} else {
	    for (Stmt statement : currBlock.getStmts()) {
		DataFlowNode node = buildStatement(statement);
		nodes.add(node);
	    }
	}
	DataFlowNode lastNode = nodes.get(nodes.size() - 1);

	// Get subgraphs of children basic blocks and connect them
	for (FlowNode nextBlock : currBlock.getOutNodes()) {
	    ArrayList<DataFlowNode> children = new ArrayList<>();
	    if (!processed.contains(nextBlock.getId())) {
		children = buildGraph((BasicBlockNode) nextBlock);
		for (DataFlowNode child : children) {
		    if (!child.disabled)
			this.addEdge(new DataFlowEdge(child, lastNode, lastNode.getIterations()));
		}
	    }
	}
	return nodes;
    }

    private DataFlowNode buildStatement(Stmt statement) {
	ClavaNode n = statement.getChild(0);
	if (n.getNumChildren() < 2)
	    return DataFlowNode.nullNode;
	ClavaNode rhs = n.getChild(1);
	ClavaNode lhs = n.getChild(0);

	// Build node of assignment operation
	DataFlowNode assignNode = DataFlowNode.nullNode;
	if (n instanceof CompoundAssignOperator) { // x += y
	    CompoundAssignOperator assign = (CompoundAssignOperator) n;
	    String op = assign.getOp().getOpString().replace("=", "");
	    assignNode = new DataFlowNode(DataFlowNodeType.OP_ARITH, op);
	    this.addNode(assignNode);
	}

	// Build LHS
	DataFlowNode lhsNode = buildExpression(lhs);
	if (lhs instanceof DeclRefExpr) {
	    lhsNode.setType(DataFlowNodeType.STORE_ARRAY);
	}
	if (lhs instanceof ArraySubscriptExpr) {
	    lhsNode.setType(DataFlowNodeType.STORE_VAR);
	}

	// Build RHS
	DataFlowNode rhsNode = buildExpression(rhs);

	// Connect edges
	if (n instanceof CompoundAssignOperator) {
	    this.addEdge(new DataFlowEdge(rhsNode, assignNode));
	    this.addEdge(new DataFlowEdge(lhsNode, assignNode));
	    this.addEdge(new DataFlowEdge(assignNode, lhsNode));
	} else
	    this.addEdge(new DataFlowEdge(rhsNode, lhsNode));

	// Change types of storing nodes to remove ambiguity
	if (lhs instanceof ArraySubscriptExpr)
	    lhsNode.setType(DataFlowNodeType.STORE_ARRAY);
	if (lhs instanceof DeclRefExpr)
	    lhsNode.setType(DataFlowNodeType.STORE_VAR);

	return lhsNode;
    }

    private DataFlowNode buildExpression(ClavaNode n) {
	DataFlowNode node = DataFlowNode.nullNode;

	if (n instanceof IntegerLiteral) {
	    node = buildIntegerLitNode((IntegerLiteral) n);
	}
	if (n instanceof DeclRefExpr) {
	    node = buildDeclRefNode((DeclRefExpr) n);
	}
	if (n instanceof ArraySubscriptExpr) {
	    node = buildArraySubExprNode((ArraySubscriptExpr) n);
	}
	if (n instanceof BinaryOperator) {
	    node = buildBinaryOperationNode((BinaryOperator) n);
	}
	if (n instanceof CallExpr) {
	    node = buildCallNode((CallExpr) n);
	}
	return node;
    }

    private DataFlowNode buildIntegerLitNode(IntegerLiteral intN) {
	String label = intN.getLiteral();
	DataFlowNode constNode = new DataFlowNode(DataFlowNodeType.CONSTANT, label);
	this.addNode(constNode);
	return constNode;
    }

    private DataFlowNode buildDeclRefNode(DeclRefExpr var) {
	String label = var.getName();
	DataFlowNode varNode = new DataFlowNode(DataFlowNodeType.LOAD_VAR, label);
	this.addNode(varNode);
	return varNode;
    }

    private DataFlowNode buildArraySubExprNode(ArraySubscriptExpr arr) {
	String label = ((DeclRefExpr) arr.getChild(0)).getName();
	DataFlowNode arrNode = new DataFlowNode(DataFlowNodeType.LOAD_ARRAY, label);
	this.addNode(arrNode);
	DataFlowNode indexNode = buildExpression(arr.getChild(1));
	this.addEdge(new DataFlowEdge(indexNode, arrNode, DataFlowEdgeType.INDEX));
	return arrNode;
    }

    private DataFlowNode buildBinaryOperationNode(BinaryOperator op) {
	String label = op.getOp().getOpString();
	DataFlowNode opNode = new DataFlowNode(DataFlowNodeType.OP_ARITH, label);
	this.addNode(opNode);
	DataFlowNode lhsNode = buildExpression(op.getChild(0));
	DataFlowNode rhsNode = buildExpression(op.getChild(1));
	String tempLabel = "temp_" + this.tempCounter;
	this.tempCounter += 1;
	DataFlowNode tempNode = new DataFlowNode(DataFlowNodeType.TEMP, tempLabel);
	this.addNode(tempNode);
	this.addEdge(new DataFlowEdge(lhsNode, opNode));
	this.addEdge(new DataFlowEdge(rhsNode, opNode));
	this.addEdge(new DataFlowEdge(opNode, tempNode));
	return tempNode;
    }

    private DataFlowNode buildCallNode(CallExpr call) {
	DeclRefExpr fun = (DeclRefExpr) call.getChild(0);
	String funName = fun.getName();
	DataFlowNode callNode = new DataFlowNode(DataFlowNodeType.OP_CALL, funName);
	this.addNode(callNode);
	for (int i = 1; i < call.getNumChildren(); i++) {
	    DataFlowNode argNode = buildExpression(call.getChild(i));
	    this.addEdge(new DataFlowEdge(argNode, callNode));
	}
	return callNode;
    }

    private DataFlowNode buildLoopNode(BasicBlockNode block) {
	ForStmt root = (ForStmt) block.getLeader();
	int initVal = -1;
	int limitVal = -1;
	int increment = 1;
	int numIter = 0;
	String counterName = "";

	// Loop counter
	if (root.getChild(0).getChild(0) instanceof VarDecl) {
	    VarDecl counter = (VarDecl) root.getChild(0).getChild(0);
	    counterName = counter.getDeclName();
	    IntegerLiteral init = (IntegerLiteral) root.getChild(0).getChild(0).getChild(0);
	    initVal = init.getValue().intValue();
	}

	// Loop condition
	if (root.getChild(1).getChild(0).getChild(1) instanceof IntegerLiteral) {
	    IntegerLiteral limit = (IntegerLiteral) root.getChild(1).getChild(0).getChild(1);
	    limitVal = limit.getValue().intValue();
	}

	// Loop increment (if different than i++)
	if (!(root.getChild(2) instanceof UnaryOperator)) {
	    increment = 1; // TODO: not important for now
	}

	if (limitVal != -1 && initVal != -1)
	    numIter = (limitVal - initVal) / increment;
	DataFlowNode node = new DataFlowNode(DataFlowNodeType.LOOP, "loop " + counterName);
	node.setIterations(numIter);
	this.addNode(node);
	return node;
    }
}
