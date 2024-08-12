import Graph from "lara-js/api/lara/graphs/Graph.js";
import CfgBuilder from "./cfg/CfgBuilder.js";
export default class ControlFlowGraph extends Graph {
    /**
     * Maps stmts to graph nodes
     */
    nodes;
    /**
     * The start node of the CFG
     */
    start;
    /**
     * The end node of the CFG
     */
    end;
    constructor(graph, nodes, startNode, endNode) {
        super(graph);
        this.nodes = nodes;
        this.start = startNode;
        this.end = endNode;
    }
    /**
     * Builds the control flow graph
     *
     * @param deterministicIds - If true, uses deterministic ids for the graph ids (e.g. id_0, id_1...). Otherwise, uses $jp.astId whenever possible
     * @param options - An object containing configuration options for the cfg
     * @returns A new instance of the ControlFlowGraph class
     */
    static build($jp, deterministicIds = false, options = {
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
        const builderResult = new CfgBuilder($jp, deterministicIds, options).build();
        return new ControlFlowGraph(...builderResult);
    }
    /**
     * Returns the graph node where the given statement belongs.
     *
     * @param $stmt - A statement join point, or a string with the astId of the join point
     */
    getNode($stmt) {
        // If string, assume it is astId
        const astId = typeof $stmt === "string" ? $stmt : $stmt.astId;
        return this.nodes.get(astId);
    }
    get startNode() {
        return this.start;
    }
    get endNode() {
        return this.end;
    }
}
//# sourceMappingURL=ControlFlowGraph.js.map