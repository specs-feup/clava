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
import java.util.Iterator;
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
import pt.up.fe.specs.clava.analysis.flow.control.CFGUtils;
import pt.up.fe.specs.clava.analysis.flow.control.ControlFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.preprocessing.FlowAnalysisPreprocessing;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CStyleCastExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.ConditionalOperator;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.ParenExpr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

/**
 * Class that represents a Data Flow Graph (DFG)
 * 
 * @author Tiago
 *
 */
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
    private DataFlowNode interfaceNode;
    private String funName;
    private boolean hasConditionals = false;

    public DataFlowGraph(FunctionDecl func) {
        this(func.getBody().get());
    }

    public DataFlowGraph(CompoundStmt body) {
        super("Data-flow Graph - " + ((FunctionDecl) body.getParent()).getDeclName(), "n");

        // Standardize function code
        FlowAnalysisPreprocessing pre = new FlowAnalysisPreprocessing(body);
        pre.applyConstantFolding();
        pre.applyUnwrapSingleLineMultipleDecls();

        // Build CFG
        this.funName = ((FunctionDecl) body.getParent()).getDeclName();
        this.body = body;
        this.firstStmt = body.getChild(0);
        this.cfg = new ControlFlowGraph(body);
        CFGUtils.convert(this.cfg);

        // Build DFG
        this.addNode(nullNode);
        this.interfaceNode = new DataFlowNode(DataFlowNodeType.INTERFACE, funName, this.firstStmt);
        this.interfaceNode.setShape("box");
        this.addNode(interfaceNode);
        BasicBlockNode start = (BasicBlockNode) cfg.findNode(0);
        if (CFGUtils.hasBasicBlockOfType(cfg, BasicBlockNodeType.IF)) {
            System.out.println(cfg.toDot());
            buildGraphTopLevelConditional(start);
            // Join all top-level flows to the interface node
            for (FlowNode n : this.nodes) {
                DataFlowNode node = (DataFlowNode) n;
                if (node.isTopLevel())
                    this.addEdge(new DataFlowEdge(interfaceNode, node, DataFlowEdgeType.REPEATING));
            }
            this.hasConditionals = true;
            return;
        } else {
            buildGraphTopLevelWithInterface(start);
        }

        // Modify DFG
        findSubgraphs();
        pruneDuplicatedNodes();
        mergeLoadNodes();
        findFunctionParams();
    }

    /**
     * Prunes all duplicated nodes, which may happen due to consistency issues
     */
    private void pruneDuplicatedNodes() {
        this.nodes = (ArrayList<FlowNode>) nodes.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Finds and builds the function parameters
     */
    private void findFunctionParams() {
        for (int i = 0; i < body.getParent().getChildren().size(); i++) {
            if (body.getParent().getChild(i) instanceof ParmVarDecl) {
                ParmVarDecl paramNode = (ParmVarDecl) body.getParent().getChild(i);
                DataFlowParam param = new DataFlowParam(paramNode);
                params.add(param);
            }
        }
        StringBuilder sb = new StringBuilder(interfaceNode.getLabel()).append("\n");
        for (DataFlowParam param : params) {
            sb.append(param.toString()).append("\n");
        }
        interfaceNode.setLabel(sb.toString());
    }

    /**
     * Finds all load nodes that refer to the same access, and merges them in a single node
     */
    private void mergeLoadNodes() {
        // First merge within a subgraph
        for (DataFlowNode root : subgraphRoots) {
            HashMap<String, ArrayList<DataFlowNode>> map = subgraphs.get(root).getMultipleVarLoads();
            map.forEach((key, value) -> {
                mergeNodes(value);
            });
        }

        // TODO: This code segment was doing nothing, DFGUtils.mergeSubgraphs returned an empty map
        // Then merge between same-level subgraphs
        /*
        ArrayList<DataFlowNode> nodes = DFGUtils.getAllNodesOfType(this, DataFlowNodeType.LOOP);
        for (DataFlowNode loop : nodes) {
        
            ArrayList<DataFlowNode> subs = DFGUtils.getSubgraphsOfLoop(loop);
            HashMap<String, ArrayList<DataFlowNode>> map = DFGUtils.mergeSubgraphs(subs);
            map.forEach((key, value) -> {
                mergeNodes(value);
            });
            
        }
        */
    }

    /**
     * Finds all subgraphs of the DFG
     */
    private void findSubgraphs() {
        for (FlowNode n : this.nodes) {
            DataFlowNode node = (DataFlowNode) n;
            if (node.getSubgraphID() == -1) {
                if (DataFlowNodeType.isStore(node.getType())) {
                    buildSubgraph(node, subgraphCounter);
                    this.subgraphRoots.add(node);
                    this.subgraphs.put(node, new DataFlowSubgraph(node, this));
                    subgraphCounter++;
                }
            }
        }
    }

    /**
     * Builds a subgraph recursively
     * 
     * @param node
     * @param id
     */
    private void buildSubgraph(DataFlowNode node, int id) {
        if (node.getSubgraphID() == id)
            return;
        node.setSubgraphID(id);
        ArrayList<FlowEdge> edges = node.getInEdges();
        for (FlowEdge e : edges) {
            DataFlowEdge edge = (DataFlowEdge) e;
            if (!DataFlowEdgeType.isControl(edge.getType())) {
                buildSubgraph((DataFlowNode) edge.getSource(), id);
            }
        }
    }

    /**
     * Gets all nodes of the subgraph with the given ID
     * 
     * @param id
     * @return a list of subgraph nodes
     */
    private ArrayList<DataFlowNode> getSubgraphNodes(int id) {
        ArrayList<DataFlowNode> nodes = new ArrayList<>();
        for (FlowNode n : this.nodes) {
            DataFlowNode node = (DataFlowNode) n;
            if (node.getSubgraphID() == id)
                nodes.add(node);
        }
        return nodes;
    }

    /**
     * Gets the subgraph counter, used to assign subgraph IDs
     * 
     * @return
     */
    public int getSubgraphCounter() {
        return subgraphCounter;
    }

    /**
     * Gets the subgraph of the given root
     * 
     * @param root
     * @return a dataflow subgraph
     */
    public DataFlowSubgraph getSubgraph(DataFlowNode root) {
        return this.subgraphs.get(root);
    }

    /**
     * Gets the subgraph with the given ID
     * 
     * @param id
     * @return a dataflow subgraph
     */
    public DataFlowSubgraph getSubgraph(int id) {
        DataFlowNode node = this.subgraphRoots.get(id);
        return this.subgraphs.get(node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.specs.clava.analysis.flow.FlowGraph#toDot()
     */
    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        String NL = "\n";
        sb.append("Digraph G {").append(NL).append("node [penwidth=2.5]").append(NL);

        // Interface node
        sb.append(interfaceNode.toDot()).append(NL);

        // Data flow nodes
        for (int i = subgraphCounter - 1; i >= 0; i--) {
            ArrayList<DataFlowNode> sub = getSubgraphNodes(i);
            sb.append("subgraph cluster").append(i).append("{").append(NL);
            for (DataFlowNode node : sub)
                sb.append(node.toDot()).append(NL);
            sb.append("}").append(NL);
        }

        // Control flow nodes (e.g. loops, isolated function calls)
        for (FlowNode n : nodes) {
            DataFlowNode node = (DataFlowNode) n;
            if (node.getSubgraphID() == -1)
                sb.append(node.toDot()).append(NL);
        }

        for (FlowEdge edge : edges) {
            sb.append(edge.toDot()).append(NL);
        }

        sb.append("labelloc=\"t\"").append(NL).append("label=\"").append(name).append("\"").append(NL).append("}");
        return sb.toString();
    }

    /**
     * Method that starts building the DFG for a function with conditionals
     * 
     * @param block
     * @return a list with the DFG nodes
     */
    private ArrayList<DataFlowNode> buildGraphTopLevelConditional(BasicBlockNode block) {
        boolean topLevel = CFGUtils.isTopLevel(block);
        int pragmaIter = -1;
        processed.add(block.getId());
        ArrayList<DataFlowNode> nodes = new ArrayList<>();

        // NORMAL BLOCK
        if (block.getType() == BasicBlockNodeType.NORMAL || block.getType() == BasicBlockNodeType.EXIT) {
            for (Stmt statement : block.getStmts()) {
                if (statement.isWrapper()) {
                    pragmaIter = CFGUtils.parsePragma(statement.getCode());
                } else {
                    DataFlowNode node = buildStatement(statement);
                    if (node == nullNode)
                        continue;
                    node.setTopLevel(topLevel);
                    nodes.add(node);
                }
            }
            // Get next BB and connect
            if (block.getOutEdges().size() > 0) {
                BasicBlockNode next = (BasicBlockNode) block.getOutEdges().get(0).getDest();
                if (!processed.contains(next.getId()))
                    nodes.addAll(buildGraphTopLevelConditional(next));
            }
        }

        // LOOP BLOCK
        if (block.getType() == BasicBlockNodeType.LOOP) {

            DataFlowNode node = buildLoopNode(block, pragmaIter);
            node.setTopLevel(topLevel);
            pragmaIter = -1;
            nodes.add(node);

            BasicBlockNode inLoopBlock = null;
            BasicBlockNode noLoopBlock = null;
            for (FlowEdge e : block.getOutEdges()) {
                BasicBlockEdge edge = (BasicBlockEdge) e;
                if (edge.getType() == BasicBlockEdgeType.LOOP)
                    inLoopBlock = (BasicBlockNode) edge.getDest();
                if (edge.getType() == BasicBlockEdgeType.NOLOOP)
                    noLoopBlock = (BasicBlockNode) edge.getDest();
            }

            if (inLoopBlock != null) {
                // Build and attach loop children to loop node
                ArrayList<DataFlowNode> descendants = buildGraphTopLevelConditional(inLoopBlock);
                for (DataFlowNode child : descendants)
                    this.addEdge(new DataFlowEdge(node, child, node.getIterations()));
            }
            if (noLoopBlock != null) {
                if (!processed.contains(noLoopBlock.getId()))
                    nodes.addAll(buildGraphTopLevelConditional(noLoopBlock));
            }
        }

        // IF BLOCK
        if (block.getType() == BasicBlockNodeType.IF) {
            // Get the node with the conditional expression
            DataFlowNode condition = null;
            for (Stmt statement : block.getStmts()) {
                if (statement.isWrapper()) {
                    pragmaIter = CFGUtils.parsePragma(statement.getCode());
                } else {
                    DataFlowNode node = buildStatement(statement);
                    System.out.println(node.toDot());
                    if (node == nullNode)
                        continue;
                    node.setTopLevel(topLevel);
                    nodes.add(node);
                    if (node.getType() == DataFlowNodeType.OP_ARITH)
                        condition = node;
                }
            }
            // Set multiplexer as top level
            if (condition == null) {
                throw new IllegalStateException("Condition node is null in IF block");
            }
            condition.setTopLevel(false);
            DataFlowNode mux = new DataFlowNode(DataFlowNodeType.OP_COND, "mux", condition.getClavaNode());
            this.addNode(mux);
            this.addEdge(new DataFlowEdge(condition, mux));
            mux.setTopLevel(topLevel);
            nodes.add(mux);
            nodes.remove(condition);

            // See if true and false basic blocks are present
            BasicBlockEdge trueEdge = null;
            BasicBlockEdge falseEdge = null;
            for (FlowEdge e : block.getOutEdges()) {
                BasicBlockEdge edge = (BasicBlockEdge) e;
                if (edge.getType() == BasicBlockEdgeType.FALSE)
                    falseEdge = edge;
                if (edge.getType() == BasicBlockEdgeType.TRUE)
                    trueEdge = edge;
            }

            // Handle true BB based on type
            if (trueEdge != null) {
                BasicBlockNode trueBlock = (BasicBlockNode) trueEdge.getDest();
                ArrayList<DataFlowNode> descendants = new ArrayList<>();
                descendants = buildGraphTopLevelConditional(trueBlock);
                for (DataFlowNode child : descendants)
                    this.addEdge(new DataFlowEdge(child, mux));
            }
            // Handle false BB based on type
            if (falseEdge != null) {
                BasicBlockNode falseBlock = (BasicBlockNode) falseEdge.getDest();
                ArrayList<DataFlowNode> descendants = new ArrayList<>();
                descendants = buildGraphTopLevelConditional(falseBlock);
                for (DataFlowNode child : descendants)
                    this.addEdge(new DataFlowEdge(child, mux));
            }
            // Get next BB
            BasicBlockNode next = CFGUtils.getTopLevelIfDescendant(cfg, block);
            if (next != block && !this.processed.contains(next.getId()))
                nodes.addAll(buildGraphTopLevelConditional(next));
        }
        return nodes;
    }

    /**
     * Method that starts building the DFG for a function with no conditionals
     * 
     * @param topBlock
     */
    private void buildGraphTopLevelWithInterface(BasicBlockNode topBlock) {
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
        // Last BB has no edges, so it is not accounted for in the loop; add it here
        if (!blocks.contains(topBlock))
            blocks.add(topBlock);

        // Build and connect the dataflows of each block
        int pragmaIter = -1;
        for (BasicBlockNode block : blocks) {
            processed.add(block.getId());
            if (block.getType() == BasicBlockNodeType.NORMAL || block.getType() == BasicBlockNodeType.EXIT) {
                for (Stmt statement : block.getStmts()) {
                    if (statement.isWrapper()) {
                        pragmaIter = CFGUtils.parsePragma(statement.getCode());
                    } else {
                        DataFlowNode node = buildStatement(statement);
                        if (node == nullNode)
                            continue;
                        node.setTopLevel(true);
                    }
                }
            }
            if (block.getType() == BasicBlockNodeType.LOOP) {
                DataFlowNode node = buildLoopNode(block, pragmaIter);
                node.setTopLevel(true);
                pragmaIter = -1;

                // Build and attach loop children to loop node
                ArrayList<DataFlowNode> descendants = buildGraphLoop(block);
                for (DataFlowNode child : descendants)
                    this.addEdge(new DataFlowEdge(node, child, node.getIterations()));
            }
        }
        for (FlowNode n : this.nodes) {
            DataFlowNode node = (DataFlowNode) n;
            if (node.isTopLevel())
                this.addEdge(new DataFlowEdge(interfaceNode, node, DataFlowEdgeType.REPEATING));
        }
    }

    /**
     * Building step for Loops
     * 
     * @param loopBlock
     * @return
     */
    private ArrayList<DataFlowNode> buildGraphLoop(BasicBlockNode loopBlock) {
        ArrayList<DataFlowNode> nodes = new ArrayList<>();
        int pragmaIter = -1;

        BasicBlockNode topBlock = null;
        for (FlowEdge e : loopBlock.getOutEdges()) {
            BasicBlockEdge edge = (BasicBlockEdge) e;
            if (edge.getType() == BasicBlockEdgeType.LOOP)
                topBlock = (BasicBlockNode) edge.getDest();
        }
        if (topBlock == null) {
            throw new IllegalStateException("Loop block has no top block");
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
                    if (statement.isWrapper()) {
                        pragmaIter = CFGUtils.parsePragma(statement.getCode());
                    } else {
                        DataFlowNode node = buildStatement(statement);
                        nodes.add(node);
                    }
                }
            }
            if (block.getType() == BasicBlockNodeType.LOOP) {
                DataFlowNode node = buildLoopNode(block, pragmaIter);
                pragmaIter = -1;

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

    /**
     * Building step for statements
     * 
     * @param statement
     * @return
     */
    private DataFlowNode buildStatement(Stmt statement) {
        ClavaNode n = statement.getChild(0);
        DataFlowNode node = nullNode;

        if (n instanceof VarDecl) {
            if (n.getNumChildren() > 0)
                node = buildVarDecl((VarDecl) n);
        }
        if ((n instanceof BinaryOperator) || (n instanceof CompoundAssignOperator)) {
            node = buildBinaryOp(n);
        }
        if (n instanceof CallExpr) {
            node = buildCallNode((CallExpr) n);
        }
        if (statement instanceof IfStmt) {
            node = buildExpression(((IfStmt) statement).getCondition());
        }
        return node;
    }

    /**
     * Building step for binary operations
     * 
     * @param n
     * @return
     */
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

    /**
     * Building step for declarations
     * 
     * @param decl
     * @return
     */
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

    /**
     * Building step for expressions
     * 
     * @param n
     * @return
     */
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
        if (n instanceof UnaryOperator) {
            node = buildUnaryOperationNode((UnaryOperator) n);
        }
        if (n instanceof ConditionalOperator) {
            node = buildConditionalOperatorNode((ConditionalOperator) n);
        }
        if (node == nullNode)
            ClavaLog.info("Unsupported note type for dfg: " + n.toString());
        return node;
    }

    /**
     * Building step for conditional operators
     * 
     * @param n
     * @return
     */
    private DataFlowNode buildConditionalOperatorNode(ConditionalOperator n) {
        DeclRefExpr t = null;
        DeclRefExpr f = null;
        ParenExpr cond = null;
        if (n.getChild(0) instanceof ParenExpr)
            cond = (ParenExpr) n.getChild(0);
        else
            return nullNode;
        if (n.getChild(1) instanceof DeclRefExpr)
            t = (DeclRefExpr) n.getChild(1);
        else
            return nullNode;
        if (n.getChild(2) instanceof DeclRefExpr)
            f = (DeclRefExpr) n.getChild(2);
        else
            return nullNode;

        DataFlowNode op = buildExpression(cond.getChild(0));
        DataFlowNode trueNode = buildDeclRefNode(t);
        DataFlowNode falseNode = buildDeclRefNode(f);
        DataFlowNode muxNode = new DataFlowNode(DataFlowNodeType.OP_COND, "mux", n);
        this.addNode(muxNode);
        this.addEdge(new DataFlowEdge(trueNode, muxNode));
        this.addEdge(new DataFlowEdge(falseNode, muxNode));
        this.addEdge(new DataFlowEdge(op, muxNode));
        return muxNode;
    }

    /**
     * Building step for unary operators
     * 
     * @param n
     * @return
     */
    private DataFlowNode buildUnaryOperationNode(UnaryOperator n) {
        UnaryOperatorKind kind = n.getOp();
        switch (kind) {
        case Minus:
        case Plus: {
            DataFlowNode child = buildExpression(n.getChild(0));
            String literal = kind == UnaryOperatorKind.Minus ? "-1" : "1";
            DataFlowNode constant = new DataFlowNode(DataFlowNodeType.CONSTANT, literal, n);
            this.addNode(constant);
            DataFlowNode op = new DataFlowNode(DataFlowNodeType.OP_ARITH, "*", n);
            this.addNode(op);
            this.addEdge(new DataFlowEdge(constant, op));
            this.addEdge(new DataFlowEdge(child, op));
            return op;
        }
        case PreInc:
        case PostInc:
        case PreDec:
        case PostDec: {
            // TODO: store is also load
            DataFlowNode child = buildExpression(n.getChild(0));
            String opSymbol = (kind == UnaryOperatorKind.PostDec || kind == UnaryOperatorKind.PreDec) ? "-" : "+";
            DataFlowNode constant = new DataFlowNode(DataFlowNodeType.CONSTANT, "1", n);
            this.addNode(constant);
            DataFlowNode op = new DataFlowNode(DataFlowNodeType.OP_ARITH, opSymbol, n);
            this.addNode(op);
            this.addEdge(new DataFlowEdge(constant, op));
            this.addEdge(new DataFlowEdge(child, op));
            return op;
        }
        default:
            return nullNode;
        }
    }

    /**
     * Building step for integer literals
     * 
     * @param intL
     * @return
     */
    private DataFlowNode buildIntegerLitNode(IntegerLiteral intL) {
        String label = intL.getLiteral();
        DataFlowNode constNode = new DataFlowNode(DataFlowNodeType.CONSTANT, label, intL);
        this.addNode(constNode);
        return constNode;
    }

    /**
     * Building step for floating literals
     * 
     * @param floatL
     * @return
     */
    private DataFlowNode buildFloatingLitNode(FloatingLiteral floatL) {
        String label = floatL.getLiteral();
        DataFlowNode constNode = new DataFlowNode(DataFlowNodeType.CONSTANT, label, floatL);
        this.addNode(constNode);
        return constNode;
    }

    /**
     * Building step for references
     * 
     * @param var
     * @return
     */
    private DataFlowNode buildDeclRefNode(DeclRefExpr var) {
        String label = var.getName();
        DataFlowNode varNode = new DataFlowNode(DataFlowNodeType.LOAD_VAR, label, var);
        this.addNode(varNode);
        return varNode;
    }

    /**
     * Building step for array access expressions (e.g., the "i+1" part of "a[i+1]")
     * 
     * @param arr
     * @return
     */
    private DataFlowNode buildArraySubExprNode(ArraySubscriptExpr arr) {
        // array variable
        String label = ((DeclRefExpr) arr.getArrayExpr()).getName();
        DataFlowNode arrNode = new DataFlowNode(DataFlowNodeType.LOAD_ARRAY, label, arr);
        this.addNode(arrNode);

        // subscripts
        for (Expr subscript : arr.getSubscripts()) {
            DataFlowNode indexNode = buildExpression(subscript);
            this.addEdge(new DataFlowEdge(indexNode, arrNode, DataFlowEdgeType.DATAFLOW_INDEX));
        }
        return arrNode;
    }

    /**
     * Building step for binary operations
     * 
     * @param op
     * @return
     */
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

    /**
     * Building step for function calls
     * 
     * @param call
     * @return
     */
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

    /**
     * Building step for loops
     * 
     * @param block
     * @param pragmaIter
     * @return
     */
    private DataFlowNode buildLoopNode(BasicBlockNode block, int pragmaIter) {
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
        if (limitVal == -1 && pragmaIter != -1)
            limitVal = pragmaIter;

        // Loop increment (if different than i++)
        if (!(root.getChild(2) instanceof UnaryOperator)) {
            increment = 1; // TODO: not important for now
        }

        if (limitVal != -1 && initVal != -1)
            numIter = (limitVal - initVal) / increment;
        DataFlowNode node = new DataFlowNode(DataFlowNodeType.LOOP, "loop " + counterName, null);
        node.setIterations(numIter);
        node.setClavaNode(root.getBody().getChild(0));
        node.setStmt((Stmt) root.getBody().getChild(0));
        this.addNode(node);
        return node;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.specs.clava.analysis.flow.FlowGraph#getSources()
     */
    @Override
    protected ArrayList<FlowNode> getSources() {
        return new ArrayList<FlowNode>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.specs.clava.analysis.flow.FlowGraph#getSinks()
     */
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

    /**
     * Gets all subgraph roots
     * 
     * @return a list of nodes where every node is a subgraph root
     */
    public ArrayList<DataFlowNode> getSubgraphRoots() {
        return subgraphRoots;
    }

    /**
     * Merges a list of nodes into a single node, using the first node on the list as the pivot
     * 
     * @param nodes
     */
    public void mergeNodes(ArrayList<DataFlowNode> nodes) {
        nodes = (ArrayList<DataFlowNode>) nodes.stream().distinct().collect(Collectors.toList());
        DataFlowNode master = nodes.get(0);
        Iterator<DataFlowNode> nodeIter = nodes.iterator();
        nodeIter.next();
        while (nodeIter.hasNext()) {
            DataFlowNode node = nodeIter.next();
            if (node.getSubgraphID() != master.getSubgraphID())
                continue;

            if (DataFlowNodeType.isArray(master.getType()) && DataFlowNodeType.isArray(node.getType())) {
                boolean isSame = DFGUtils.isSameArrayAccess(master, node);
                if (!isSame)
                    continue;
            }

            Iterator<FlowEdge> iter = node.getInEdges().iterator();
            while (iter.hasNext()) {
                FlowEdge inEdge = iter.next();
                FlowNode inNode = inEdge.getSource();
                inNode.removeOutEdge(inEdge);
                inEdge.setDest(master);
                inNode.addOutEdge(inEdge);
                master.addInEdge(inEdge);
            }
            iter = node.getOutEdges().iterator();
            while (iter.hasNext()) {
                FlowEdge outEdge = iter.next();
                FlowNode outNode = outEdge.getDest();
                outNode.removeInEdge(outEdge);
                outEdge.setSource(master);
                outNode.addInEdge(outEdge);
                master.addOutEdge(outEdge);
            }
            node.clear();
            this.nodes.remove(node);
        }
    }

    /**
     * Gets the body of the function this DFG refers to
     * 
     * @return
     */
    public CompoundStmt getBody() {
        return body;
    }

    /**
     * Gets all function parameters
     * 
     * @return
     */
    public ArrayList<DataFlowParam> getParams() {
        return params;
    }

    /**
     * Gets the first statement of the function this DFG refers to
     * 
     * @return
     */
    public ClavaNode getFirstStmt() {
        return firstStmt;
    }

    /**
     * Gets the name of the function this DFG refers to
     * 
     * @return
     */
    public String getFunctionName() {
        return ((FunctionDecl) body.getParent()).getDeclName();
    }

    /**
     * Checks whether the function has conditionals
     * 
     * @return
     */
    public boolean hasConditionals() {
        return hasConditionals;
    }

    /**
     * Gets the Control Flow Graph (CFG) used to build this DFG
     * 
     * @return
     */
    public ControlFlowGraph getCfg() {
        return this.cfg;
    }
}
