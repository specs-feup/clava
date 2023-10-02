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
    static build($jp, deterministicIds = false, splitInstList = false) {
        const builderResult = new CfgBuilder($jp, deterministicIds, splitInstList).build();
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