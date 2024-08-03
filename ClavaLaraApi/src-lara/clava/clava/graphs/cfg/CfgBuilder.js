import { debug } from "lara-js/api/lara/core/LaraCore.js";
import Graphs from "lara-js/api/lara/graphs/Graphs.js";
import Query from "lara-js/api/weaver/Query.js";
import { FunctionJp, Loop, Scope, Statement, } from "../../../Joinpoints.js";
import ClavaJoinPoints from "../../ClavaJoinPoints.js";
import CfgEdge from "./CfgEdge.js";
import CfgEdgeType from "./CfgEdgeType.js";
import CfgNodeType from "./CfgNodeType.js";
import CfgUtils from "./CfgUtils.js";
import NextCfgNode from "./NextCfgNode.js";
import DataFactory from "./nodedata/DataFactory.js";
export default class CfgBuilder {
    /**
     * AST node to process
     */
    jp;
    /**
     * Graph being built
     */
    graph;
    /**
     * Maps stmts to graph nodes
     */
    nodes;
    /**
     * The start node of the graph
     */
    startNode;
    /**
     * The end node of the graph
     */
    endNode;
    /**
     * Maps the astId to the corresponding temporary statement
     */
    temporaryStmts;
    /**
     * If true, uses deterministic ids for the graph ids (e.g. id_0, id_1...). Otherwise, uses $jp.astId whenever possible.
     */
    deterministicIds;
    /**
     * Current id, in case deterministic ids are used
     */
    currentId;
    /**
     * An instance of DataFactory, for creating graph node data
     */
    dataFactory;
    /**
     * Calculates what node is unconditionally executed after a given statement
     */
    nextNodes;
    /**
     * If true, each instruction list should be split
     */
    splitInstList;
    /**
     * If true, the goto nodes should be excluded from the graph
     */
    removeGotoNodes;
    /**
     * If true, the label nodes should be excluded from the graph
     */
    removeLabelNodes;
    /**
     * If true, the temporary scope statements should not be removed from the graph
     */
    keepTemporaryScopeStmts;
    /**
     * Creates a new instance of the CfgBuilder class
     * @param $jp -
     * @param deterministicIds - If true, uses deterministic ids for the graph ids (e.g. id_0, id_1...). Otherwise, uses $jp.astId whenever possible
     * @param options - An object containing configuration options for the cfg
     */
    constructor($jp, deterministicIds = false, options = {
        /**
         * If true, statements of each instruction list must be split
         */
        splitInstList: false,
        /**
         * If true, the nodes that correspond to goto statements will be excluded from the resulting graph
         */
        removeGotoNodes: false,
        /**
         * If true, the nodes that correspond to label statements will be excluded from the resulting graph
         */
        removeLabelNodes: false,
        /**
         * If true, the temporary scope statements will be kept in the resulting graph
         */
        keepTemporaryScopeStmts: false,
    }) {
        this.jp = $jp;
        this.splitInstList = options.splitInstList ?? false;
        this.removeGotoNodes = options.removeGotoNodes ?? false;
        this.removeLabelNodes = options.removeLabelNodes ?? false;
        this.keepTemporaryScopeStmts = options.keepTemporaryScopeStmts ?? false;
        this.deterministicIds = deterministicIds;
        this.currentId = 0;
        this.dataFactory = new DataFactory(this.jp);
        this.graph = Graphs.newGraph();
        this.nodes = new Map();
        // Create start and end nodes
        // Do not add them to #nodes, since they have no associated statements
        this.startNode = Graphs.addNode(this.graph, this.dataFactory.newData(CfgNodeType.START, undefined, "start", this.splitInstList));
        this.endNode = Graphs.addNode(this.graph, this.dataFactory.newData(CfgNodeType.END, undefined, "end", this.splitInstList));
        this.temporaryStmts = {};
        this.nextNodes = new NextCfgNode(this.jp, this.nodes, this.endNode);
    }
    /**
     * Returns the next id that will be used to identify a graph node
     */
    nextId() {
        const nextId = "id_" + this.currentId;
        this.currentId++;
        return nextId;
    }
    /**
     * Connects two nodes using a directed edge
     * @param source - Starting point from which the edge originates
     * @param target - Destination point to which the edge points
     * @param edgeType - The edge label that connects
     */
    addEdge(source, target, edgeType) {
        Graphs.addEdge(this.graph, source, target, new CfgEdge(edgeType));
    }
    /**
     * Builds the control flow graph
     * @returns An array that includes the built graph, the nodes, the start and end nodes
     */
    build() {
        this.addAuxComments();
        this.createNodes();
        //this._fillNodes();
        this.connectNodes();
        this.cleanCfg();
        // TODO: Check graph invariants
        // 1. Each node has either one unconditional outgoing edge,
        // or two outgoing edges that must be a pair true/false,
        // or if there is no outgoing edge must be the end node
        return [this.graph, this.nodes, this.startNode, this.endNode];
    }
    /**
     * Inserts comments that specify the beginning and end of a scope
     */
    addAuxComments() {
        for (const $currentJp of this.jp.descendants) {
            if ($currentJp instanceof Scope) {
                const $scopeStart = $currentJp.insertBegin(ClavaJoinPoints.comment("SCOPE_START"));
                this.temporaryStmts[$scopeStart.astId] = $scopeStart;
                const $scopeEnd = $currentJp.insertEnd(ClavaJoinPoints.comment("SCOPE_END"));
                this.temporaryStmts[$scopeEnd.astId] = $scopeEnd;
            }
        }
    }
    /**
     * Creates all nodes (except start and end), with only the leader statement
     */
    createNodes() {
        // Test all statements for leadership
        // If they are leaders, create node
        for (const $jp of Query.searchFromInclusive(this.jp, "statement")) {
            const $stmt = $jp;
            if (CfgUtils.isLeader($stmt)) {
                if (this.splitInstList &&
                    CfgUtils.getNodeType($stmt) === CfgNodeType.INST_LIST) {
                    this.getOrAddNode($stmt, true, CfgNodeType.INST_LIST);
                    for (const $rightJp of $stmt.siblingsRight) {
                        const $right = $rightJp;
                        if (!CfgUtils.isLeader($right))
                            this.getOrAddNode($right, true, CfgNodeType.INST_LIST);
                        else
                            break;
                    }
                }
                else
                    this.getOrAddNode($stmt, true);
            }
        }
        // Special case: if starting node is a statement and a graph node was not created for it (e.g. creating a graph starting from an arbitrary statement),
        // create one with the type INST_LIST
        if (this.jp instanceof Statement &&
            this.nodes.get(this.jp.astId) === undefined) {
            this.getOrAddNode(this.jp, true, CfgNodeType.INST_LIST);
        }
    }
    /**
     * Connects a node associated with a statement that is an instance of a "if" statement.
     * @param node - Node whose type is "IF"
     */
    connectIfNode(node) {
        const ifStmt = node.data().if;
        if (ifStmt === undefined) {
            throw new Error("If statement is undefined");
        }
        const thenStmt = ifStmt.then;
        if (thenStmt === undefined) {
            throw new Error("Then statement is undefined");
        }
        const thenNode = this.nodes.get(thenStmt.astId);
        if (thenNode === undefined) {
            throw new Error("Node for then statement is undefined: " + thenStmt.astId);
        }
        this.addEdge(node, thenNode, CfgEdgeType.TRUE);
        const elseStmt = ifStmt.else;
        if (elseStmt !== undefined) {
            const elseNode = this.nodes.get(elseStmt.astId);
            if (elseNode === undefined) {
                throw new Error("Node for else statement is undefined: " + elseStmt.astId);
            }
            this.addEdge(node, elseNode, CfgEdgeType.FALSE);
        }
        else {
            // Usually there should always be a sibling, because of inserted comments
            // However, if an arbitary statement is given as the starting point,
            // sometimes there might not be nothing after. In this case, connect to the
            // end node.
            const afterNode = this.nextNodes.nextExecutedNode(ifStmt);
            // Add edge
            this.addEdge(node, afterNode, CfgEdgeType.FALSE);
        }
    }
    /**
     * Connects a node associated with a statement that is an instance of a "loop" statement.
     * @param node - Node whose type is "LOOP"
     */
    connectLoopNode(node) {
        const $loop = node.data().loop;
        if ($loop === undefined) {
            throw new Error("Loop statement is undefined");
        }
        let afterStmt = undefined;
        switch ($loop.kind) {
            case "for":
                afterStmt = $loop.init;
                break;
            case "while":
                afterStmt = $loop.cond;
                break;
            case "dowhile":
                afterStmt = $loop.body;
                break;
            default:
                throw new Error("Case not defined for loops of kind " + $loop.kind);
        }
        const afterNode = this.nodes.get(afterStmt.astId) ?? this.endNode;
        this.addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * Connects a node associated with a statement that is part of a loop header and corresponds to the loop condition
     * @param node - Node whose type is "COND"
     */
    connectCondNode(node) {
        // Get kind of loop
        const $condStmt = node.data().nodeStmt;
        if ($condStmt === undefined) {
            throw new Error("Cond statement is undefined");
        }
        const $loop = $condStmt.parent;
        if ($loop === undefined) {
            throw new Error("Loop is undefined");
        }
        if (!($loop instanceof Loop)) {
            throw new Error("$loop is not an instance of Loop");
        }
        // True - first stmt of the loop body
        const trueNode = this.nodes.get($loop.body.astId) ?? this.endNode;
        this.addEdge(node, trueNode, CfgEdgeType.TRUE);
        // False - next stmt of the loop
        const falseNode = this.nextNodes.nextExecutedNode($loop);
        // Create edge
        this.addEdge(node, falseNode, CfgEdgeType.FALSE);
    }
    /**
     * Connects a node associated with a statement that is an instance of a "break" statement.
     * @param node - Node whose type is "BREAK"
     */
    connectBreakNode(node) {
        const $breakStmt = node.data().nodeStmt;
        if ($breakStmt === undefined) {
            throw new Error("Break statement is undefined");
        }
        const $enclosingStmt = $breakStmt.enclosingStmt;
        const afterNode = this.nextNodes.nextExecutedNode($enclosingStmt);
        this.addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * Connects a node associated with a statement that is an instance of a "continue" statement.
     * @param node - Node whose type is "CONTINUE"
     */
    connectContinueNode(node) {
        const $continueStmt = node.data().nodeStmt;
        if ($continueStmt === undefined) {
            throw new Error("Continue statement is undefined");
        }
        const $loop = $continueStmt.getAncestor("loop");
        if ($loop === undefined) {
            throw new Error("Loop is undefined");
        }
        const $afterStmt = $loop.kind === "for" ? $loop.step : $loop.cond;
        const afterNode = this.nodes.get($afterStmt.astId) ?? this.endNode;
        this.addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * Connects a node associated with a statement that is an instance of a "switch" statement.
     * @param node - Node whose type is "SWITCH"
     */
    connectSwitchNode(node) {
        const $switchStmt = node.data().switch;
        if ($switchStmt === undefined) {
            throw new Error("Switch statement is undefined");
        }
        let firstReachedCase = undefined;
        // The first reached case is the first non-default case.
        // If the switch only has one case statement, and it is the default case, then this default case will be the first reached case
        for (const $case of $switchStmt.cases) {
            firstReachedCase = this.nodes.get($case.astId);
            if (!$case.isDefault) {
                break;
            }
        }
        if (firstReachedCase) {
            this.addEdge(node, firstReachedCase, CfgEdgeType.UNCONDITIONAL);
        }
    }
    /**
     * Connects a node associated with a statement that is an instance of a "case" statement.
     * @param node - Node whose type is "CASE"
     */
    connectCaseNode(node) {
        const $caseStmt = node.data().case;
        if ($caseStmt === undefined) {
            throw new Error("Case statement is undefined");
        }
        const $switchStmt = $caseStmt.getAncestor("switch");
        if ($switchStmt === undefined) {
            throw new Error("Switch statement is undefined");
        }
        const numCases = $switchStmt.cases.length;
        const hasIntermediateDefault = $switchStmt.hasDefaultCase && !$switchStmt.cases[numCases - 1].isDefault;
        // Connect the node to the first instruction to be executed
        const firstExecutedInst = this.nextNodes.nextExecutedNode($caseStmt);
        if ($caseStmt.isDefault)
            this.addEdge(node, firstExecutedInst, CfgEdgeType.UNCONDITIONAL);
        else
            this.addEdge(node, firstExecutedInst, CfgEdgeType.TRUE);
        let falseNode = undefined;
        if ($caseStmt.nextCase !== undefined) {
            // Not the last case
            // If the next case is an intermediate default case, the node should be connected to the CASE node following the default case
            if (hasIntermediateDefault && $caseStmt.nextCase.isDefault)
                falseNode = this.nodes.get($caseStmt.nextCase.nextCase.astId);
            else if (!$caseStmt.isDefault)
                // Else, if it is not an intermediate default case, it should be connected to the next case
                falseNode = this.nodes.get($caseStmt.nextCase.astId);
        }
        else if (!$caseStmt.isDefault) {
            // Last case but not a default case
            // If switch statement has an intermediate default case, connect the current statement to the default case
            if (hasIntermediateDefault)
                falseNode = this.nodes.get($switchStmt.getDefaultCase.astId);
            // Else, connect it to the statement following the switch
            else
                falseNode = this.nextNodes.nextExecutedNode($switchStmt);
        }
        if (falseNode !== undefined)
            this.addEdge(node, falseNode, CfgEdgeType.FALSE);
    }
    /**
     * Connects a node associated with a statement that is part of a loop header and corresponds to the loop initialization
     * @param node - Node whose type is "INIT"
     */
    connectInitNode(node) {
        const $initStmt = node.data().nodeStmt;
        if ($initStmt === undefined) {
            throw new Error("Init statement is undefined");
        }
        const $loop = $initStmt.parent;
        if ($loop === undefined) {
            throw new Error("Loop is undefined");
        }
        if (!($loop instanceof Loop)) {
            throw new Error("$loop is not an instance of Loop");
        }
        if ($loop.kind !== "for") {
            throw new Error("Not implemented for loops of kind " + $loop.kind);
        }
        const $condStmt = $loop.cond;
        if ($condStmt === undefined) {
            throw new Error("Not implemented when for loops do not have a condition statement");
        }
        const afterNode = this.nodes.get($condStmt.astId) ?? this.endNode;
        this.addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * Connects a node associated with a statement that is part of a loop header and corresponds to the loop step
     * @param node - Node whose type is "STEP"
     */
    connectStepNode(node) {
        // Get loop
        const $stepStmt = node.data().nodeStmt;
        if ($stepStmt === undefined) {
            throw new Error("Step statement is undefined");
        }
        const $loop = $stepStmt.parent;
        if ($loop === undefined) {
            throw new Error("Loop is undefined");
        }
        if (!($loop instanceof Loop)) {
            throw new Error("$loop is not an instance of Loop");
        }
        if ($loop.kind !== "for") {
            throw new Error("Not implemented for loops of kind " + $loop.kind);
        }
        const $condStmt = $loop.cond;
        if ($condStmt === undefined) {
            throw new Error("Not implemented when for loops do not have a condition statement");
        }
        const afterNode = this.nodes.get($condStmt.astId) ?? this.endNode;
        this.addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * @param node - Node whose type is "INST_LIST"
     */
    connectInstListNode(node) {
        const $lastStmt = node.data().getLastStmt();
        if ($lastStmt === undefined) {
            throw new Error("Last statement is undefined");
        }
        const afterNode = this.nextNodes.nextExecutedNode($lastStmt);
        this.addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * @param node - Node whose type is "GOTO"
     */
    connectGotoNode(node) {
        const $gotoStmt = node.data().nodeStmt;
        if ($gotoStmt === undefined) {
            throw new Error("Goto statement is undefined");
        }
        const labelName = $gotoStmt.label.name;
        const $labelStmt = Query.searchFromInclusive(this.jp, "labelStmt", {
            decl: (decl) => decl.name == labelName,
        }).first();
        if ($labelStmt === undefined) {
            throw new Error("Label statement is undefined");
        }
        const afterNode = this.nodes.get($labelStmt.astId);
        if (afterNode === undefined) {
            throw new Error("Node for label statement is undefined: " + $labelStmt.astId);
        }
        this.addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * @param node - Node whose type is "LABEL"
     */
    connectLabelNode(node) {
        const $labelStmt = node.data().nodeStmt;
        if ($labelStmt === undefined) {
            throw new Error("Label statement is undefined");
        }
        const afterNode = this.nextNodes.nextExecutedNode($labelStmt);
        this.addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * Connects a node associated with a statement that is an instance of a "return" statement.
     * @param node - Node whose type is "RETURN"
     */
    connectReturnNode(node) {
        this.addEdge(node, this.endNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * Connects a node associated with a statement that is an instance of a "scope" statement.
     * @param node - Node whose type is "SCOPE", "THEN" or "ELSE"
     */
    connectScopeNode(node) {
        const $scope = node.data().scope;
        // Scope connects to its own first statement that will be an INST_LIST
        const afterNode = this.nodes.get($scope.firstStmt.astId);
        if (afterNode === undefined) {
            throw new Error("Node for first statement of scope is undefined: " +
                $scope.firstStmt.astId);
        }
        this.addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
    }
    /**
     * Connects the leader statement nodes according to their type
     */
    connectNodes() {
        // Connect start
        let startAstNode = this.jp;
        if (startAstNode instanceof FunctionJp) {
            startAstNode = startAstNode.body;
        }
        if (!(startAstNode instanceof Statement)) {
            throw new Error("Not defined how to connect the Start node to an AST node of type " +
                this.jp.joinPointType);
        }
        const afterNode = this.nodes.get(startAstNode.astId);
        if (afterNode === undefined) {
            throw new Error("Node for first statement of scope is undefined: " + startAstNode.astId);
        }
        // Add edge
        this.addEdge(this.startNode, afterNode, CfgEdgeType.UNCONDITIONAL);
        for (const astId of this.nodes.keys()) {
            const node = this.nodes.get(astId);
            if (node === undefined) {
                throw new Error("Node is undefined for astId " + astId);
            }
            const nodeData = node.data();
            const nodeStmt = nodeData.nodeStmt;
            if (nodeStmt === undefined) {
                throw new Error("Node statement is undefined");
            }
            // Only add connections for astIds of leader statements
            if (nodeStmt.astId !== astId) {
                continue;
            }
            const nodeType = nodeData.type;
            if (nodeType === undefined) {
                throw new Error("Node type is undefined: ");
            }
            switch (nodeType) {
                case CfgNodeType.IF:
                    this.connectIfNode(node);
                    break;
                case CfgNodeType.LOOP:
                    this.connectLoopNode(node);
                    break;
                case CfgNodeType.COND:
                    this.connectCondNode(node);
                    break;
                case CfgNodeType.BREAK:
                    this.connectBreakNode(node);
                    break;
                case CfgNodeType.CONTINUE:
                    this.connectContinueNode(node);
                    break;
                case CfgNodeType.SWITCH:
                    this.connectSwitchNode(node);
                    break;
                case CfgNodeType.CASE:
                    this.connectCaseNode(node);
                    break;
                case CfgNodeType.INIT:
                    this.connectInitNode(node);
                    break;
                case CfgNodeType.STEP:
                    this.connectStepNode(node);
                    break;
                case CfgNodeType.INST_LIST:
                    this.connectInstListNode(node);
                    break;
                case CfgNodeType.GOTO:
                    this.connectGotoNode(node);
                    break;
                case CfgNodeType.LABEL:
                    this.connectLabelNode(node);
                    break;
                case CfgNodeType.RETURN:
                    this.connectReturnNode(node);
                    break;
                case CfgNodeType.SCOPE:
                case CfgNodeType.THEN:
                case CfgNodeType.ELSE:
                    this.connectScopeNode(node);
                    break;
            }
        }
    }
    cleanCfg() {
        // Remove temporary instructions from the code
        if (!this.keepTemporaryScopeStmts) {
            for (const stmtId in this.temporaryStmts) {
                this.temporaryStmts[stmtId].detach();
            }
        }
        // Remove temporary instructions from the instList nodes and this.nodes
        for (const node of this.nodes.values()) {
            const nodeData = node.data();
            // Only inst lists need to be cleaned
            if (nodeData.type !== CfgNodeType.INST_LIST) {
                const tempStmts = nodeData.stmts.filter(($stmt) => this.temporaryStmts[$stmt.astId] !== undefined);
                if (tempStmts.length > 0) {
                    console.log("Node '" +
                        nodeData.type.toString() +
                        "' has temporary stmts: " +
                        tempStmts.toString());
                }
                continue;
            }
            // Filter stmts that are temporary statements
            if (this.keepTemporaryScopeStmts) {
                continue;
            }
            const filteredStmts = [];
            for (const $stmt of nodeData.stmts) {
                // If not a temporary stmt, add to filtered list
                if (this.temporaryStmts[$stmt.astId] === undefined) {
                    filteredStmts.push($stmt);
                }
                // Otherwise, remove from this.nodes
                else {
                    this.nodes.delete($stmt.astId);
                }
            }
            if (filteredStmts.length !== nodeData.stmts.length) {
                nodeData.stmts = filteredStmts;
            }
        }
        // Remove empty instList CFG nodes
        for (const node of this.graph.nodes()) {
            const nodeData = node.data();
            // Only nodes that are inst lists
            if (nodeData.type !== CfgNodeType.INST_LIST) {
                continue;
            }
            // Only empty nodes
            if (nodeData.stmts.length > 0) {
                continue;
            }
            // Remove node, replacing the connections with a new connection of the same type and the incoming edge
            // of the node being removed
            Graphs.removeNode(this.graph, node, (incoming) => new CfgEdge(incoming.data().type));
        }
        // Remove label nodes
        if (this.removeLabelNodes) {
            for (const node of this.graph.nodes()) {
                const nodeData = node.data();
                // Only nodes whose type is "LABEL"
                if (nodeData.type !== CfgNodeType.LABEL)
                    continue;
                Graphs.removeNode(this.graph, node, (incoming) => new CfgEdge(incoming.data().type));
                if (nodeData.nodeStmt === undefined) {
                    throw new Error("Node statement is undefined");
                }
                this.nodes.delete(nodeData.nodeStmt.astId);
            }
        }
        // Remove goto nodes
        if (this.removeGotoNodes) {
            for (const node of this.graph.nodes()) {
                const nodeData = node.data();
                // Only nodes whose type is "GOTO"
                if (nodeData.type !== CfgNodeType.GOTO)
                    continue;
                Graphs.removeNode(this.graph, node, (incoming) => new CfgEdge(incoming.data().type));
                if (nodeData.nodeStmt === undefined) {
                    throw new Error("Node statement is undefined");
                }
                this.nodes.delete(nodeData.nodeStmt.astId);
            }
        }
        // Remove nodes that have no incoming edge and are not start
        for (const node of this.graph.nodes()) {
            const nodeData = node.data();
            // Only nodes that are not start
            if (nodeData.type === CfgNodeType.START) {
                continue;
            }
            // Ignore nodes with incoming edges
            if (node.incomers().length > 0) {
                continue;
            }
            // Remove node
            debug("[CfgBuilder] Removing statement that is not executed (e.g. is after a return): " +
                nodeData.stmts.toString());
            // Removes nodes. As there are no incoming edges, the edge handler is a dummy as it is not called
            Graphs.removeNode(this.graph, node, () => new CfgEdge(CfgEdgeType.UNCONDITIONAL));
        }
    }
    /**
     * Returns the node corresponding to this statement, or creates a new one if one does not exist yet.
     */
    getOrAddNode($stmt, create = false, forceNodeType) {
        let node = this.nodes.get($stmt.astId);
        // If there is not yet a node for this statement, create
        if (node === undefined && create) {
            const nodeType = forceNodeType ?? CfgUtils.getNodeType($stmt);
            if (nodeType === undefined) {
                throw new Error("Node type is undefined for statement at line " + $stmt.line);
            }
            const nodeId = this.deterministicIds ? this.nextId() : undefined;
            node = Graphs.addNode(this.graph, this.dataFactory.newData(nodeType, $stmt, nodeId, this.splitInstList));
            // Associate all statements of graph node
            for (const $nodeStmt of node.data().stmts) {
                // Check if it has not been already added
                if (this.nodes.get($nodeStmt.astId) !== undefined) {
                    throw new Error("Adding mapping twice for statement " +
                        $nodeStmt.astId +
                        "@" +
                        $nodeStmt.location);
                }
                this.nodes.set($nodeStmt.astId, node);
            }
        }
        else {
            throw new Error("No node for statement at line " + $stmt.line);
        }
        return node;
    }
}
//# sourceMappingURL=CfgBuilder.js.map