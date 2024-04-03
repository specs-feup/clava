import DotFormatter from "lara-js/api/lara/graphs/DotFormatter.js";
import Graph from "lara-js/api/lara/graphs/Graph.js";
import Graphs from "lara-js/api/lara/graphs/Graphs.js";
import StaticCallGraphBuilder from "./scg/StaticCallGraphBuilder.js";
export default class StaticCallGraph extends Graph {
    static dotFormatterInstance = undefined;
    /**
     * Maps functions to graph nodes
     */
    functionMap;
    constructor(graph, functions) {
        super(graph);
        this.functionMap = functions;
    }
    /**
     *
     * @param $jp -
     * @param visitCalls - If true, recursively visits the functions of each call, building a call graph of the available code
     * @returns
     */
    static build($jp, visitCalls = true) {
        const builder = new StaticCallGraphBuilder();
        const graph = builder.build($jp, visitCalls);
        return new StaticCallGraph(graph, builder.nodes);
    }
    get functions() {
        return this.functionMap;
    }
    getNode($function) {
        // Normalize function
        return this.functionMap[$function.canonical.astId];
    }
    static get dotFormatter() {
        if (StaticCallGraph.dotFormatterInstance === undefined) {
            StaticCallGraph.dotFormatterInstance = new DotFormatter();
            StaticCallGraph.dotFormatterInstance.addNodeAttribute("style=dashed", (node) => Graphs.isLeaf(node) &&
                !node.data().hasImplementation());
            StaticCallGraph.dotFormatterInstance.addNodeAttribute("style=filled", (node) => Graphs.isLeaf(node) && node.data().hasCalls());
        }
        return StaticCallGraph.dotFormatterInstance;
    }
    toDot() {
        return super.toDot(StaticCallGraph.dotFormatter);
    }
}
//# sourceMappingURL=StaticCallGraph.js.map