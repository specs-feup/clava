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
    super(graph);
    this.#nodes = nodes;
    this.#startNode = startNode;
    this.#endNode = endNode;
  }

  /**
   * Builds the control flow graph
   * 
   * @param {joinpoint} $jp
   * @param {boolean} [deterministicIds = false] If true, uses deterministic ids for the graph ids (e.g. id_0, id_1...). Otherwise, uses $jp.astId whenever possible
   * @param {Object} [options = {}] An object containing configuration options for the cfg
   * @param {boolean} [options.splitInstList = false] If true, statements of each instruction list must be split
   * @param {boolean} [options.removeGotoNodes = false] If true, the nodes that correspond to goto statements will be excluded from the resulting graph
   * @param {boolean} [options.removeLabelNodes = false] If true, the nodes that correspond to label statements will be excluded from the resulting graph
   * @param {boolean} [options.keepTemporaryScopeStmts = true]  If true, the temporary scope statements will be kept in the resulting graph
   * @returns {ControlFlowGraph} a new instance of the ControlFlowGraph class
  */
  static build($jp, deterministicIds = false, options = {}) {
    const builderResult = new CfgBuilder($jp, deterministicIds, options).build();
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
