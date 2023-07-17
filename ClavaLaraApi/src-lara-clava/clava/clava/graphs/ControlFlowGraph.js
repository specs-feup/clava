laraImport("lara.graphs.Graph");
laraImport("clava.graphs.cfg.CfgBuilder");

class ControlFlowGraph extends Graph {

  /**
   * Maps stmts to graph nodes
   */
  #nodes;

  /**
   * The start node of the CFG
   */
  #startNode;

  /**
   * The end node of the CFG
   */
  #endNode;

  constructor(graph, nodes, startNode, endNode) {
    super();
    super.graph = graph;
    this.#nodes = nodes;
    this.#startNode = startNode;
    this.#endNode = endNode;
  }

  static build($jp, deterministicIds = false, splitInstList = false) {
    const builderResult = new CfgBuilder($jp, deterministicIds, splitInstList).build();
    return new ControlFlowGraph(...builderResult);
  }

  /**
   * Returns the graph node where the given statement belongs.
   *
   * @param {$stmt|string} $stmt A statement join point, or a string with the astId of the join point
   */
  getNode($stmt) {
    // If string, assume it is astId
    const astId = isString($stmt)
      ? $stmt
      : isJoinPoint($stmt)
      ? $stmt.astId
      : undefined;

    if (astId === undefined) {
      throw new Error("Invalid input, must be either a join point or a string");
    }

    return this.#nodes.get(astId);
  }

  get startNode() {
    return this.#startNode;
  }

  get endNode() {
    return this.#endNode;
  }
}
