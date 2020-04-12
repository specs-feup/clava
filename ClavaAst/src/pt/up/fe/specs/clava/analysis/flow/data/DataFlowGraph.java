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
import pt.up.fe.specs.clava.analysis.flow.Edge;
import pt.up.fe.specs.clava.analysis.flow.FlowGraph;
import pt.up.fe.specs.clava.analysis.flow.Node;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlock;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockType;
import pt.up.fe.specs.clava.analysis.flow.control.ControlFlowGraph;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

public class DataFlowGraph extends FlowGraph {
    private ControlFlowGraph cfg;
    private int loopN;
    private ArrayList<String> processed = new ArrayList<>();
    private int tempCounter = 0;

    public DataFlowGraph(FunctionDecl func, int loopN) {
	this(func.getBody().get(), loopN);
    }

    public DataFlowGraph(CompoundStmt body, int loopN) {
	super("graph");
	this.cfg = new ControlFlowGraph(body);
	this.loopN = loopN;

	BasicBlock start = findStartingRegion();

	buildGraph(start);
    }

    @Override
    protected String buildDot() {
	StringBuilder sb = new StringBuilder();
	String NL = "\n";
	sb.append("Digraph G {").append(NL);
	for (Node node : nodes) {
	    sb.append(node.toDot()).append(NL);
	}
	for (Edge edge : edges) {
	    sb.append(edge.toDot()).append(NL);
	}
	sb.append("}").append(NL);
	return sb.toString();
    }

    private BasicBlock findStartingRegion() {
	ArrayList<BasicBlock> bb = cfg.getBasicBlocks();

	int min = Integer.MAX_VALUE;
	BasicBlock start = null;
	for (BasicBlock b : bb) {
	    if (b.getType() == BasicBlockType.LOOP) {
		if (b.getOrder() < min) {
		    min = b.getOrder();
		    start = b;
		}
	    }
	}
	return start;
    }

    private ArrayList<DataFlowNode> buildGraph(BasicBlock currBlock) {
	processed.add(currBlock.getId());

	// Build graph for each statement of basic block
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	if (currBlock.getType() == BasicBlockType.LOOP) {
	    nodes.add(buildLoopNode(currBlock));
	} else {
	    for (Stmt statement : currBlock.getStmts()) {
		DataFlowNode node = buildStatement(statement);
		nodes.add(node);
	    }
	}
	DataFlowNode lastNode = nodes.get(nodes.size() - 1);

	// Get subgraphs of children basic blocks and connect them
	for (BasicBlock nextBlock : currBlock.getOutEdges()) {
	    ArrayList<DataFlowNode> children = new ArrayList<>();
	    if (!processed.contains(nextBlock.getId())) {
		children = buildGraph(nextBlock);
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
	    assignNode = new DataFlowNode(DataFlowNodeType.OP, op);
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
	if (n instanceof IntegerLiteral) {
	    IntegerLiteral intN = (IntegerLiteral) n;
	    String label = intN.getLiteral();
	    DataFlowNode constNode = new DataFlowNode(DataFlowNodeType.CONSTANT, label);
	    this.addNode(constNode);
	    return constNode;
	}
	if (n instanceof DeclRefExpr) {
	    DeclRefExpr var = (DeclRefExpr) n;
	    String label = var.getName();
	    DataFlowNode varNode = new DataFlowNode(DataFlowNodeType.LOAD_VAR, label);
	    this.addNode(varNode);
	    return varNode;
	}
	if (n instanceof ArraySubscriptExpr) {
	    ArraySubscriptExpr arr = (ArraySubscriptExpr) n;
	    String label = ((DeclRefExpr) arr.getChild(0)).getName();
	    DataFlowNode arrNode = new DataFlowNode(DataFlowNodeType.LOAD_ARRAY, label);
	    this.addNode(arrNode);
	    DataFlowNode indexNode = buildExpression(arr.getChild(1));
	    this.addEdge(new DataFlowEdge(indexNode, arrNode, DataFlowEdgeType.INDEX));
	    return arrNode;
	}
	if (n instanceof BinaryOperator) {
	    BinaryOperator op = (BinaryOperator) n;
	    String label = op.getOp().getOpString();
	    DataFlowNode opNode = new DataFlowNode(DataFlowNodeType.OP, label);
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
	System.out.println(n.toString());
	return DataFlowNode.nullNode;
    }

    private DataFlowNode buildLoopNode(BasicBlock block) {
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
