import Graph from "lara-js/api/lara/graphs/Graph.js";
import cytoscape from "lara-js/api/libs/cytoscape-3.26.0.js";
import { Statement } from "../../Joinpoints.js";
import CfgBuilder from "./cfg/CfgBuilder.js";

export default class ControlFlowGraph extends Graph {
  /**
   * Maps stmts to graph nodes
   */
  private nodes: Map<string, cytoscape.NodeSingular>;

  /**
   * The start node of the CFG
   */
  private start: cytoscape.NodeSingular;

  /**
   * The end node of the CFG
   */
  private end: cytoscape.NodeSingular;

  constructor(
    graph: cytoscape.Core,
    nodes: Map<string, cytoscape.NodeSingular>,
    startNode: cytoscape.NodeSingular,
    endNode: cytoscape.NodeSingular
  ) {
    super(graph);
    this.nodes = nodes;
    this.start = startNode;
    this.end = endNode;
  }

  static build(
    $jp: Statement,
    deterministicIds = false,
    splitInstList = false
  ) {
    const builderResult = new CfgBuilder(
      $jp,
      deterministicIds,
      splitInstList
    ).build();
    return new ControlFlowGraph(...builderResult);
  }

  /**
   * Returns the graph node where the given statement belongs.
   *
   * @param $stmt - A statement join point, or a string with the astId of the join point
   */
  getNode($stmt: Statement | string) {
    // If string, assume it is astId
    const astId: string = typeof $stmt === "string" ? $stmt : $stmt.astId;

    return this.nodes.get(astId);
  }

  get startNode() {
    return this.start;
  }

  get endNode() {
    return this.end;
  }
}
