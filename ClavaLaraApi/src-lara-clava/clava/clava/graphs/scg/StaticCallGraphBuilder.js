laraImport("lara.graphs.Graphs");
laraImport("lara.util.StringSet");
laraImport("lara.Check");

laraImport("weaver.Query");

laraImport("clava.graphs.scg.ScgNodeData");
laraImport("clava.graphs.scg.ScgEdgeData");

class StaticCallGraphBuilder {
  // The static call graph
  #graph;

  // Maps AST nodes to graph nodes
  #nodeMap;

  // Maps function->function relations to edges
  #edges;

  // Counter for auto-incrementing node ids
  #nodeCounter;

  // Tracker of visited functions
  #markedForVisit;

  // Stack to track which functions still need to be visited
  #toVisit;

  constructor() {
    this.#graph = Graphs.newGraph();
    this.#nodeMap = {};
    this.#edges = {};
    this.#nodeCounter = 0;
  }

  get nodes() {
    return this.#nodeMap;
  }

  #addOrGetNode($function) {
    // Only functions can be nodes
    Check.isJoinPoint($function, "function");

    let graphNode = this.#nodeMap[$function.astId];

    // If not found, create and add it
    if (graphNode === undefined) {
      const nodeData = new ScgNodeData($function);
      nodeData.id = this.#newNodeId();
      graphNode = Graphs.addNode(this.#graph, nodeData);
      this.#nodeMap[$function.astId] = graphNode;
    }

    return graphNode;
  }

  #newNodeId() {
    const id = "node_" + this.#nodeCounter;
    this.#nodeCounter++;
    return id;
  }


  /**
   *
   * @param {$jp} $jp
   * @param {boolean} [visitCalls = true] - If true, recursively visits the functions of each call, building a call graph of the available code
   * @returns
   */
  build($jp, visitCalls = true) {
    // If undefined, assume root
    if ($jp === undefined) {
      $jp = Query.root();
    }

    this.#prefillStackFromScope($jp);

    while (this.#toVisit.length !== 0) {
      this.#visit(this.#toVisit.pop(), visitCalls);
    }

    return this.#graph;

    // Get function->call relations
    // let pairs = this.#getFunctionCallPairs($jp, visitCalls);

    // // Add nodes and edges
    // for (const pair of pairs) {
    //   const $sourceFunction = pair["function"];
    //   const $call = pair["call"];
    //   const $targetFunction = $call.function; // Already canonical function

    //   const sourceNode = this.#addOrGetNode($sourceFunction);
    //   const targetNode = this.#addOrGetNode($targetFunction);

    //   this.#addEdge(sourceNode, targetNode, $call);
    // }

    // return this.#graph;
  }

  /**
   * 
   * @param {JoinPoint} $jp 
   */
  #prefillStackFromScope($jp) {
    this.#toVisit = Query.searchFromInclusive($jp, "function", {"isImplementation": true}).get();
    this.#markedForVisit = new Set(this.#toVisit.map(f => f.signature));
  }

  /**
   * 
   * @param {JoinPoint} $function 
   * @param {*} recursive 
   */
  #visit($function, recursive) {
    println($function.signature);

    const sourceNode = this.#addOrGetNode($function);

    const calls = Query.searchFrom($function, "call");
    for (const $c of calls) {

      if ($c.definition === undefined) {
        println("Undefined");
        continue;
      }

      const targetNode = this.#addOrGetNode($c.definition);
      this.#addEdge(sourceNode, targetNode, $c);

      // Queue function for further analysis
      if (recursive && $c.definition !== undefined && !this.#markedForVisit.has($c.signature)) {
        this.#toVisit.push($c.definition);
        this.#markedForVisit.add($c.signature);
      }
    }
  }



  static getEdgeId(sourceNode, targetNode) {
    return (
      sourceNode.data().function.signature +
      "$" +
      targetNode.data().function.signature
    );
  }

  #addEdge(sourceNode, targetNode, $call) {
    const edgeId = StaticCallGraphBuilder.getEdgeId(sourceNode, targetNode);
    //println("Source->Target Id: " + sourceTargetId);

    let edge = this.#edges[edgeId];
    if (edge === undefined) {
      const edgeData = new ScgEdgeData();
      edgeData.id = edgeId;
      edge = Graphs.addEdge(this.#graph, sourceNode, targetNode, edgeData);
      this.#edges[edgeId] = edge;
    }

    // Increment edge value
    edge.data().inc($call);
  }





















  #getFunctionCallPairs($jp, visitCalls) {
    let pairs = [];
    let seenFunctions = new StringSet();
    let jpsToSearch = [$jp];

    while (jpsToSearch.length > 0) {
      let seenCalls = [];

      // Check if any of the jps to search is a call
      for (const $jp of jpsToSearch) {
        if ($jp.instanceOf("call")) {
          seenCalls.push($jp);
        }
      }

      for (const $jpToSearch of jpsToSearch) {
        // Get all function/call pairs for functions that are not yet in the seen functions
        let functionCall = Query.searchFromInclusive(
          $jpToSearch,
          "function",
          (self) => !seenFunctions.has(self.signature)
        )
          .search("call")
          .chain();

        for (const pair of functionCall) {
          seenFunctions.add(pair["function"].signature);
          pairs.push({ function: pair["function"], call: pair["call"] });
          seenCalls.push(pair["call"]);
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
          //println("Adding function " + $functionDef.signature)
        }
      }
    }

    return pairs;
  }
}
