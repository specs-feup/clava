import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import Graphs from "lara-js/api/lara/graphs/Graphs.js";
import cytoscape from "lara-js/api/libs/cytoscape-3.26.0.js";
import Query from "lara-js/api/weaver/Query.js";
import { Call, FunctionJp, Joinpoint, Program } from "../../../Joinpoints.js";
import ScgEdgeData from "./ScgEdgeData.js";
import ScgNodeData from "./ScgNodeData.js";

export default class StaticCallGraphBuilder {
  /**
   * The static call graph
   */
  private graph = Graphs.newGraph();

  /**
   * Maps AST nodes to graph nodes
   */
  private nodesMap: Record<string, cytoscape.NodeSingular> = {};

  /**
   * Maps function-\>function relations to edges
   */
  private edgesMap: Record<string, cytoscape.EdgeSingular> = {};

  private nodeCounter: number = 0;

  get nodes() {
    return this.nodesMap;
  }

  private addOrGetNode($function: FunctionJp): cytoscape.NodeSingular {
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

  private newNodeId(): string {
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
  build($jp?: Joinpoint, visitCalls: boolean = true): cytoscape.Core {
    // If undefined, assume root
    if ($jp === undefined) {
      $jp = Query.root() as Program;
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

  static getEdgeId(
    sourceNode: cytoscape.NodeSingular,
    targetNode: cytoscape.NodeSingular
  ): string {
    return (
      (sourceNode.data() as ScgNodeData).function.signature +
      "$" +
      (targetNode.data() as ScgNodeData).function.signature
    );
  }

  private addEdge(
    sourceNode: cytoscape.NodeSingular,
    targetNode: cytoscape.NodeSingular,
    $call: Call
  ): void {
    const edgeId = StaticCallGraphBuilder.getEdgeId(sourceNode, targetNode);

    let edge = this.edgesMap[edgeId];
    if (edge === undefined) {
      const edgeData = new ScgEdgeData();
      edgeData.id = edgeId;
      edge = Graphs.addEdge(this.graph, sourceNode, targetNode, edgeData);
      this.edgesMap[edgeId] = edge;
    }

    // Increment edge value
    (edge.data() as ScgEdgeData).inc($call);
  }

  private getFunctionCallPairs($jp: Joinpoint, visitCalls: boolean) {
    const pairs = [];
    const seenFunctions: Set<string> = new Set<string>();
    let jpsToSearch = [$jp];

    while (jpsToSearch.length > 0) {
      const seenCalls: Call[] = [];

      // Check if any of the jps to search is a call
      for (const $jp of jpsToSearch) {
        if ($jp instanceof Call) {
          seenCalls.push($jp);
        }
      }

      for (const $jpToSearch of jpsToSearch) {
        // Get all function/call pairs for functions that are not yet in the seen functions
        const functionCall = Query.searchFromInclusive(
          $jpToSearch,
          FunctionJp,
          (self: LaraJoinPoint) =>
            !seenFunctions.has((self as FunctionJp).signature)
        )
          .search(Call)
          .chain();

        for (const pair of functionCall) {
          const $function = pair["function"] as FunctionJp;
          const $call = pair["call"] as Call;

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
