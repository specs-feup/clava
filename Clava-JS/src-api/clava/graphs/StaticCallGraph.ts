import DotFormatter from "lara-js/api/lara/graphs/DotFormatter.js";
import Graph from "lara-js/api/lara/graphs/Graph.js";
import Graphs from "lara-js/api/lara/graphs/Graphs.js";
import cytoscape from "lara-js/api/libs/cytoscape-3.26.0.js";
import { FunctionJp, Joinpoint } from "../../Joinpoints.js";
import ScgNodeData from "./scg/ScgNodeData.js";
import StaticCallGraphBuilder from "./scg/StaticCallGraphBuilder.js";

export default class StaticCallGraph extends Graph {
  private static dotFormatterInstance: DotFormatter | undefined = undefined;

  // Maps functions to graph nodes
  private functionMap: Record<string, cytoscape.CollectionReturnValue>;

  constructor(
    graph: cytoscape.Core,
    functions: Record<string, cytoscape.CollectionReturnValue>
  ) {
    super(graph);
    this.functionMap = functions;
  }

  /**
   *
   * @param $jp -
   * @param visitCalls - If true, recursively visits the functions of each call, building a call graph of the available code
   * @returns
   */
  static build($jp: Joinpoint, visitCalls: boolean = true) {
    const builder = new StaticCallGraphBuilder();

    const graph = builder.build($jp, visitCalls);

    return new StaticCallGraph(graph, builder.nodes);
  }

  get functions() {
    return this.functionMap;
  }

  getNode($function: FunctionJp) {
    // Normalize function
    return this.functionMap[$function.canonical.astId];
  }

  static get dotFormatter() {
    if (StaticCallGraph.dotFormatterInstance === undefined) {
      StaticCallGraph.dotFormatterInstance = new DotFormatter();
      StaticCallGraph.dotFormatterInstance.addNodeAttribute(
        "style=dashed",
        (node) =>
          Graphs.isLeaf(node) &&
          !(node.data() as ScgNodeData).hasImplementation()
      );
      StaticCallGraph.dotFormatterInstance.addNodeAttribute(
        "style=filled",
        (node) => Graphs.isLeaf(node) && (node.data() as ScgNodeData).hasCalls()
      );
    }

    return StaticCallGraph.dotFormatterInstance;
  }

  toDot() {
    return super.toDot(StaticCallGraph.dotFormatter);
  }
}
