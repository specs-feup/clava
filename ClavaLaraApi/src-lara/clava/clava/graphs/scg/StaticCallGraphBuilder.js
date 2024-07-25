import Graphs from "lara-js/api/lara/graphs/Graphs.js";
import Query from "lara-js/api/weaver/Query.js";
import { Call } from "../../../Joinpoints.js";
import ScgEdgeData from "./ScgEdgeData.js";
import ScgNodeData from "./ScgNodeData.js";
export default class StaticCallGraphBuilder {
    /**
     * The static call graph
     */
    graph = Graphs.newGraph();
    /**
     * Maps AST nodes to graph nodes
     */
    nodesMap = {};
    /**
     * Maps function-\>function relations to edges
     */
    edgesMap = {};
    nodeCounter = 0;
    get nodes() {
        return this.nodesMap;
    }
    addOrGetNode($function) {
        let graphNode = this.nodesMap[$function.astId];
        // If not found, create and add it
        if (graphNode === undefined) {
            const nodeData = new ScgNodeData($function);
            nodeData.id = this.newNodeId();
            graphNode = Graphs.addNode(this.graph, nodeData);
            this.nodesMap[$function.astId] = graphNode;
        }
        return graphNode;
    }
    newNodeId() {
        const id = "node_" + this.nodeCounter;
        this.nodeCounter++;
        return id;
    }
    /**
     *
     * @param $jp -
     * @param visitCalls - If true, recursively visits the functions of each call, building a call graph of the available code
     * @returns
     */
    build($jp, visitCalls = true) {
        // If undefined, assume root
        if ($jp === undefined) {
            $jp = Query.root();
        }
        // Get function->call relations
        const pairs = this.getFunctionCallPairs($jp, visitCalls);
        // Add nodes and edges
        for (const pair of pairs) {
            const $sourceFunction = pair["function"];
            const $call = pair["call"];
            const $targetFunction = $call.function; // Already canonical function
            const sourceNode = this.addOrGetNode($sourceFunction);
            const targetNode = this.addOrGetNode($targetFunction);
            this.addEdge(sourceNode, targetNode, $call);
        }
        return this.graph;
    }
    static getEdgeId(sourceNode, targetNode) {
        return (sourceNode.data().function.signature +
            "$" +
            targetNode.data().function.signature);
    }
    addEdge(sourceNode, targetNode, $call) {
        const edgeId = StaticCallGraphBuilder.getEdgeId(sourceNode, targetNode);
        let edge = this.edgesMap[edgeId];
        if (edge === undefined) {
            const edgeData = new ScgEdgeData();
            edgeData.id = edgeId;
            edge = Graphs.addEdge(this.graph, sourceNode, targetNode, edgeData);
            this.edgesMap[edgeId] = edge;
        }
        // Increment edge value
        edge.data().inc($call);
    }
    getFunctionCallPairs($jp, visitCalls) {
        const pairs = [];
        const seenFunctions = new Set();
        let jpsToSearch = [$jp];
        while (jpsToSearch.length > 0) {
            const seenCalls = [];
            // Check if any of the jps to search is a call
            for (const $jp of jpsToSearch) {
                if ($jp instanceof Call) {
                    seenCalls.push($jp);
                }
            }
            for (const $jpToSearch of jpsToSearch) {
                // Get all function/call pairs for functions that are not yet in the seen functions
                const functionCall = Query.searchFromInclusive($jpToSearch, "function", (self) => !seenFunctions.has(self.signature))
                    .search("call")
                    .chain();
                for (const pair of functionCall) {
                    const $function = pair["function"];
                    const $call = pair["call"];
                    seenFunctions.add($function.signature);
                    pairs.push({ function: $function, call: $call });
                    seenCalls.push($call);
                }
            }
            // Calculate new jps to search
            jpsToSearch = [];
            if (visitCalls) {
                for (const $call of seenCalls) {
                    const $functionDef = $call.definition;
                    // Only visit if there is a definition
                    if ($functionDef === undefined) {
                        continue;
                    }
                    // Check if already visited
                    if (seenFunctions.has($functionDef.signature)) {
                        continue;
                    }
                    // Add function
                    jpsToSearch.push($functionDef);
                }
            }
        }
        return pairs;
    }
}
//# sourceMappingURL=StaticCallGraphBuilder.js.map