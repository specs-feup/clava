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
import java.util.HashMap;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.analysis.flow.FlowEdge;
import pt.up.fe.specs.clava.analysis.flow.FlowGraph;
import pt.up.fe.specs.clava.analysis.flow.FlowNode;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockEdge;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockEdgeType;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockNode;
import pt.up.fe.specs.clava.analysis.flow.control.BasicBlockNodeType;
import pt.up.fe.specs.clava.analysis.flow.control.ControlFlowGraph;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CStyleCastExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.ParenExpr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

public class DataFlowGraph extends FlowGraph {
    private ControlFlowGraph cfg;
    private ArrayList<Integer> processed = new ArrayList<>();
    private int tempCounter = 0;
    private boolean tempEnabled = false;
    private int subgraphCounter = 1;
    private ArrayList<DataFlowParam> params = new ArrayList<>();
    private ArrayList<DataFlowNode> subgraphRoots = new ArrayList<>();
    private HashMap<DataFlowNode, DataFlowSubgraph> subgraphs = new HashMap<>();
    private CompoundStmt body;
    private ClavaNode firstStmt;
    public static final DataFlowNode nullNode = new DataFlowNode(DataFlowNodeType.NULL, "", null);

    public DataFlowGraph(FunctionDecl func) {
	this(func.getBody().get());
    }

    public DataFlowGraph(CompoundStmt body) {
	super("Data-flow Graph - " + ((FunctionDecl) body.getParent()).getDeclName(), "n");
	this.body = body;
	this.firstStmt = body.getChild(0);
	this.cfg = new ControlFlowGraph(body);
	CFGConverter.convert(this.cfg);
	this.addNode(nullNode);

	BasicBlockNode start = (BasicBlockNode) cfg.findNode(0);
	buildGraphTopLevel(start);
	findSubgraphs();
	pruneDuplicatedNodes();
	findDuplicatedNodes();
	findFunctionParams();
    }

    private void pruneDuplicatedNodes() {
	this.nodes = (ArrayList<FlowNode>) nodes.stream().distinct().collect(Collectors.toList());
    }

    private void findFunctionParams() {
	for (int i = 0; i < body.getParent().getChildren().size(); i++) {
	    if (body.getParent().getChild(i) instanceof ParmVarDecl) {
		ParmVarDecl paramNode = (ParmVarDecl) body.getParent().getChild(i);
		DataFlowParam param = new DataFlowParam(paramNode);
		params.add(param);
	    }
	}
    }

    private void findDuplicatedNodes() {
	for (DataFlowNode root : subgraphRoots) {
	    HashMap<String, ArrayList<DataFlowNode>> map = subgraphs.get(root).getMultipleVarLoads();
	    map.forEach((key, value) -> {
		mergeNodes(value);
		ClavaLog.info("Merged accesses to variable " + key);
	    });
	}
    }

    private void findSubgraphs() {
	for (FlowNode n : this.nodes) {
	    DataFlowNode node = (DataFlowNode) n;
	    if (node.getSubgraphID() == -1) {
		boolean inEdge = false;
		for (FlowEdge e : node.getInEdges()) {
		    DataFlowEdge edge = (DataFlowEdge) e;
		    if (edge.getType() == DataFlowEdgeType.REPEATING)
			inEdge = true;
		}
		boolean outEdge = true;
		for (FlowEdge e : node.getOutEdges()) {
		    DataFlowEdge edge = (DataFlowEdge) e;
		    if (edge.getType() == DataFlowEdgeType.REPEATING)
			inEdge = false;
		}
		if (inEdge && outEdge) {
		    buildSubgraph(node, subgraphCounter);
		    node.setSubgraphRoot(true);
		    this.subgraphRoots.add(node);
		    this.subgraphs.put(node, new DataFlowSubgraph(node));
		    subgraphCounter++;
		} else {
		    node.setSubgraphID(0);
		}
	    }
	}
    }

    private void buildSubgraph(DataFlowNode node, int id) {
	if (node.getSubgraphID() == id)
	    return;
	node.setSubgraphID(id);
	ArrayList<FlowEdge> edges = node.getInEdges();
	for (FlowEdge e : edges) {
	    DataFlowEdge edge = (DataFlowEdge) e;
	    if (edge.getType() != DataFlowEdgeType.REPEATING) {
		buildSubgraph((DataFlowNode) edge.getSource(), id);
	    }
	}
    }

    private ArrayList<DataFlowNode> getSubgraphNodes(int id) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();
	for (FlowNode n : this.nodes) {
	    DataFlowNode node = (DataFlowNode) n;
	    if (node.getSubgraphID() == id)
		nodes.add(node);
	}
	return nodes;
    }

    public int getSubgraphCounter() {
	return subgraphCounter;
    }

    public DataFlowSubgraph getSubgraph(DataFlowNode root) {
	return this.subgraphs.get(root);
    }

    public DataFlowSubgraph getSubgraph(int id) {
	DataFlowNode node = this.subgraphRoots.get(id);
	return this.subgraphs.get(node);
    }

    @Override
    protected String buildDot() {
	StringBuilder sb = new StringBuilder();
	String NL = "\n";
	sb.append("Digraph G {").append(NL).append("node [penwidth=2.5]").append(NL);

	for (int i = subgraphCounter - 1; i >= 0; i--) {
	    ArrayList<DataFlowNode> sub = getSubgraphNodes(i);
	    sb.append("subgraph cluster").append(i).append("{").append(NL);
	    for (DataFlowNode node : sub)
		sb.append(node.toDot()).append(NL);
	    sb.append("}").append(NL);
	}

	for (FlowEdge edge : edges) {
	    sb.append(edge.toDot()).append(NL);
	}

	sb.append("labelloc=\"t\"").append(NL).append("label=\"").append(name).append("\"").append(NL).append("}");
	return sb.toString();
    }

    private void buildGraphTopLevel(BasicBlockNode topBlock) {
	// Get top basic blocks
	ArrayList<BasicBlockNode> blocks = new ArrayList<>();
	while (topBlock.hasOutEdges()) {
	    if (blocks.contains(topBlock))
		break;
	    blocks.add(topBlock);
	    for (FlowEdge e : topBlock.getOutEdges()) {
		BasicBlockEdge edge = (BasicBlockEdge) e;
		if (edge.getType() == BasicBlockEdgeType.UNCONDITIONAL || edge.getType() == BasicBlockEdgeType.NOLOOP)
		    topBlock = (BasicBlockNode) edge.getDest();
	    }
	}
	if (blocks.size() == 0)
	    blocks.add(topBlock);

	// Build and connect the dataflows of each block
	DataFlowNode previous = DataFlowGraph.nullNode;
	for (BasicBlockNode block : blocks) {
	    processed.add(block.getId());
	    if (block.getType() == BasicBlockNodeType.NORMAL || block.getType() == BasicBlockNodeType.EXIT) {
		for (Stmt statement : block.getStmts()) {
		    DataFlowNode node = buildStatement(statement);
		    node.setTopLevel(true);
		    if (previous == DataFlowGraph.nullNode) {
			previous = node;
		    } else {
			this.addEdge(new DataFlowEdge(previous, node, DataFlowEdgeType.REPEATING));
			previous = node;
		    }
		}
	    }
	    if (block.getType() == BasicBlockNodeType.LOOP) {
		DataFlowNode node = buildLoopNode(block);
		node.setTopLevel(true);

		// Build and attach loop children to loop node
		ArrayList<DataFlowNode> descendants = buildGraphLoop(block);
		for (DataFlowNode child : descendants)
		    this.addEdge(new DataFlowEdge(node, child, node.getIterations()));

		// Connect loop to other control nodes
		if (previous == DataFlowGraph.nullNode) {
		    previous = node;
		} else {
		    this.addEdge(new DataFlowEdge(previous, node, DataFlowEdgeType.REPEATING));
		    previous = node;
		}
	    }
	}
    }

    private ArrayList<DataFlowNode> buildGraphLoop(BasicBlockNode loopBlock) {
	ArrayList<DataFlowNode> nodes = new ArrayList<>();

	BasicBlockNode topBlock = null;
	for (FlowEdge e : loopBlock.getOutEdges()) {
	    BasicBlockEdge edge = (BasicBlockEdge) e;
	    if (edge.getType() == BasicBlockEdgeType.LOOP)
		topBlock = (BasicBlockNode) edge.getDest();
	}

	// Get top basic blocks
	ArrayList<BasicBlockNode> blocks = new ArrayList<>();
	while (topBlock != loopBlock) {
	    blocks.add(topBlock);
	    for (FlowEdge e : topBlock.getOutEdges()) {
		BasicBlockEdge edge = (BasicBlockEdge) e;
		if (edge.getType() == BasicBlockEdgeType.UNCONDITIONAL || edge.getType() == BasicBlockEdgeType.NOLOOP) {
		    topBlock = (BasicBlockNode) edge.getDest();
		}
	    }
	}

	// Build the dataflow of each block
	for (BasicBlockNode block : blocks) {
	    if (block.getType() == BasicBlockNodeType.NORMAL) {
		for (Stmt statement : block.getStmts()) {
		    DataFlowNode node = buildStatement(statement);
		    nodes.add(node);
		}
	    }
	    if (block.getType() == BasicBlockNodeType.LOOP) {
		DataFlowNode node = buildLoopNode(block);

		// Build and attach loop children to loop node
		ArrayList<DataFlowNode> descendants = buildGraphLoop(block);
		for (DataFlowNode child : descendants)
		    this.addEdge(new DataFlowEdge(node, child, node.getIterations()));
		nodes.add(node);
	    }
	    processed.add(block.getId());
	}
	return nodes;
    }

    private DataFlowNode buildStatement(Stmt statement) {
	ClavaNode n = statement.getChild(0);
	DataFlowNode node = nullNode;

	if (n instanceof VarDecl) {
	    node = buildVarDecl((VarDecl) n);
	}
	if ((n instanceof BinaryOperator) || (n instanceof CompoundAssignOperator)) {
	    node = buildBinaryOp(n);
	}
	if (n instanceof CallExpr) {
	    node = buildCallNode((CallExpr) n);
	}
	return node;
    }

    private DataFlowNode buildBinaryOp(ClavaNode n) {
	ClavaNode rhs = n.getChild(1);
	ClavaNode lhs = n.getChild(0);

	// Build node of assignment operation
	DataFlowNode assignNode = nullNode;
	if (n instanceof CompoundAssignOperator) { // x += y
	    CompoundAssignOperator assign = (CompoundAssignOperator) n;
	    String op = assign.getOp().getOpString().replace("=", "");
	    assignNode = new DataFlowNode(DataFlowNodeType.OP_ARITH, op, n);
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

    private DataFlowNode buildVarDecl(VarDecl decl) {
	DataFlowNode lhsNode = new DataFlowNode(DataFlowNodeType.STORE_VAR, decl.getDeclName(), decl);
	this.addNode(lhsNode);
	if (decl.getNumChildren() > 0) {
	    ClavaNode rhs = decl.getChild(0);
	    DataFlowNode rhsNode = buildExpression(rhs);
	    this.addNode(rhsNode);
	    this.addEdge(new DataFlowEdge(rhsNode, lhsNode));
	}
	return lhsNode;
    }

    private DataFlowNode buildExpression(ClavaNode n) {
	DataFlowNode node = nullNode;

	if (n instanceof IntegerLiteral) {
	    node = buildIntegerLitNode((IntegerLiteral) n);
	}
	if (n instanceof FloatingLiteral) {
	    node = buildFloatingLitNode((FloatingLiteral) n);
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
	if (n instanceof CStyleCastExpr) {
	    node = buildExpression(n.getChild(0));
	}
	if (n instanceof ParenExpr) {
	    node = buildExpression(n.getChild(0));
	}
	if (node == nullNode)
	    ClavaLog.warning("Unsupported note type for dfg: " + n.toString());
	return node;
    }

    private DataFlowNode buildIntegerLitNode(IntegerLiteral intL) {
	String label = intL.getLiteral();
	DataFlowNode constNode = new DataFlowNode(DataFlowNodeType.CONSTANT, label, intL);
	this.addNode(constNode);
	return constNode;
    }

    private DataFlowNode buildFloatingLitNode(FloatingLiteral floatL) {
	String label = floatL.getLiteral();
	DataFlowNode constNode = new DataFlowNode(DataFlowNodeType.CONSTANT, label, floatL);
	this.addNode(constNode);
	return constNode;
    }

    private DataFlowNode buildDeclRefNode(DeclRefExpr var) {
	String label = var.getName();
	DataFlowNode varNode = new DataFlowNode(DataFlowNodeType.LOAD_VAR, label, var);
	this.addNode(varNode);
	return varNode;
    }

    private DataFlowNode buildArraySubExprNode(ArraySubscriptExpr arr) {
	if (arr.getChild(0) instanceof ArraySubscriptExpr)
	    return buildArraySubExprNode((ArraySubscriptExpr) arr.getChild(0));

	String label = ((DeclRefExpr) arr.getChild(0)).getName();
	DataFlowNode arrNode = new DataFlowNode(DataFlowNodeType.LOAD_ARRAY, label, arr);
	this.addNode(arrNode);
	DataFlowNode indexNode = buildExpression(arr.getChild(1));
	this.addEdge(new DataFlowEdge(indexNode, arrNode, DataFlowEdgeType.INDEX));
	return arrNode;
    }

    private DataFlowNode buildBinaryOperationNode(BinaryOperator op) {
	String label = op.getOp().getOpString();
	DataFlowNode opNode = new DataFlowNode(DataFlowNodeType.OP_ARITH, label, op);
	this.addNode(opNode);
	DataFlowNode lhsNode = buildExpression(op.getChild(0));
	DataFlowNode rhsNode = buildExpression(op.getChild(1));
	this.addEdge(new DataFlowEdge(lhsNode, opNode));
	this.addEdge(new DataFlowEdge(rhsNode, opNode));
	if (tempEnabled) {
	    String tempLabel = "temp_" + this.tempCounter;
	    this.tempCounter += 1;
	    DataFlowNode tempNode = new DataFlowNode(DataFlowNodeType.TEMP, tempLabel, op);
	    this.addNode(tempNode);
	    this.addEdge(new DataFlowEdge(opNode, tempNode));
	    return tempNode;
	} else
	    return opNode;
    }

    private DataFlowNode buildCallNode(CallExpr call) {
	DeclRefExpr fun = (DeclRefExpr) call.getChild(0);
	String funName = fun.getName();
	DataFlowNode callNode = new DataFlowNode(DataFlowNodeType.OP_CALL, funName, call);
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
	int numIter = Integer.MAX_VALUE;
	String counterName = "";

	// Loop counter
	// Case "int i = 0"
	if (root.getChild(0).getChild(0) instanceof VarDecl) {
	    VarDecl counter = (VarDecl) root.getChild(0).getChild(0);
	    counterName = counter.getDeclName();
	    if (root.getChild(0).getChild(0).getChild(0) instanceof IntegerLiteral) {
		IntegerLiteral init = (IntegerLiteral) root.getChild(0).getChild(0).getChild(0);
		initVal = init.getValue().intValue();
	    }
	}
	// Case "i = 0"
	if (root.getChild(0).getChild(0) instanceof BinaryOperator) {
	    BinaryOperator op = (BinaryOperator) root.getChild(0).getChild(0);
	    DeclRefExpr ref = (DeclRefExpr) op.getLhs();
	    counterName = ref.getName();
	    if (op.getRhs() instanceof IntegerLiteral) {
		IntegerLiteral init = (IntegerLiteral) op.getRhs();
		initVal = init.getValue().intValue();
	    }
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
	DataFlowNode node = new DataFlowNode(DataFlowNodeType.LOOP, "loop " + counterName, null);
	node.setIterations(numIter);
	this.addNode(node);
	return node;
    }

    @Override
    protected ArrayList<FlowNode> getSources() {
	return new ArrayList<FlowNode>();
    }

    @Override
    protected ArrayList<FlowNode> getSinks() {
	ArrayList<FlowNode> sinks = new ArrayList<>();
	for (FlowNode node : nodes) {
	    for (int i = 0; i < node.getOutEdges().size(); i++) {
		DataFlowEdge edge = (DataFlowEdge) node.getOutEdges().get(i);
		if (edge.getType() == DataFlowEdgeType.REPEATING) {
		    sinks.add(node);
		    break;
		}
	    }
	}
	return sinks;
    }

    public ArrayList<DataFlowNode> getSubgraphRoots() {
	return subgraphRoots;
    }

    public void mergeNodes(ArrayList<DataFlowNode> nodes) {
	DataFlowNode master = nodes.get(0);
	for (int i = 1; i < nodes.size(); i++) {
	    DataFlowNode node = nodes.get(i);
	    if (node.getSubgraphID() != master.getSubgraphID())
		continue;
	    for (FlowEdge inEdge : node.getInEdges()) {
		FlowNode inNode = inEdge.getSource();
		inNode.removeOutEdge(inEdge);
		inEdge.setDest(master);
		inNode.addOutEdge(inEdge);
		master.addInEdge(inEdge);
	    }
	    for (FlowEdge outEdge : node.getOutEdges()) {
		FlowNode outNode = outEdge.getDest();
		outNode.removeInEdge(outEdge);
		outEdge.setSource(master);
		outNode.addInEdge(outEdge);
		master.addOutEdge(outEdge);
	    }
	    node.clear();
	    removeNode(node);
	}
    }

    public CompoundStmt getBody() {
	return body;
    }

    public ArrayList<DataFlowParam> getParams() {
	return params;
    }

    public ClavaNode getFirstStmt() {
	return firstStmt;
    }
}
