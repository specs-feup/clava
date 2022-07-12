laraImport("lara.graphs.Graphs");
laraImport("weaver.Query");
laraImport("lara.util.StringSet");

laraImport("clava.graphs.scg.StaticCallGraphBuilder");

class StaticCallGraph {
  // The static call graph
  #graph;

  // Maps functions to graph nodes
  #functions;

  constructor(graph, functions) {
    this.#graph = graph;
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

  get graph() {
    return this.#graph;
  }

  get functions() {
    return this.#functions;
  }

  getNode($function) {
    // Normalize function
    return this.#functions[$function.canonical.astId];
  }
}
