laraImport("lara.graphs.Graphs");
laraImport("lara.graphs.DotFormatter");
laraImport("lara.util.StringSet");
laraImport("lara.graphs.Graph");

laraImport("weaver.Query");

laraImport("clava.graphs.scg.StaticCallGraphBuilder");

class StaticCallGraph extends Graph {
  static #dotFormatter = undefined;

  // Maps functions to graph nodes
  #functions;

  constructor(graph, functions) {
    super(graph.json());
    this.#functions = functions;
  }

  /**
   *
   * @param {$jp} $jp
   * @param {boolean} [visitCalls = true] - If true, recursively visits the functions of each call, building a call graph of the available code
   * @returns
   */
  static build($jp, visitCalls = true) {
    const builder = new StaticCallGraphBuilder();

    const graph = builder.build($jp, visitCalls);

    return new StaticCallGraph(graph, builder.nodes);
  }

  get functions() {
    return this.#functions;
  }

  getNode($function) {
    // Normalize function
    return this.#functions[$function.canonical.astId];
  }

  static get dotFormatter() {
    if (StaticCallGraph.#dotFormatter === undefined) {
      StaticCallGraph.#dotFormatter = new DotFormatter();
      StaticCallGraph.#dotFormatter.addNodeAttribute(
        "style=dashed",
        (node) => Graphs.isLeaf(node) && !node.data().hasImplementation()
      );
      StaticCallGraph.#dotFormatter.addNodeAttribute(
        "style=filled",
        (node) => Graphs.isLeaf(node) && node.data().hasCalls()
      );
    }

    return StaticCallGraph.#dotFormatter;
  }

  toDot() {
    return super.toDot(StaticCallGraph.dotFormatter);
  }
}
