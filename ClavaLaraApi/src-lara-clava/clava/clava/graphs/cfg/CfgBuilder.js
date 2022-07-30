laraImport("lara.graphs.Graphs");
laraImport("lara.Strings");
laraImport("lara.Check");
laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("clava.graphs.cfg.CfgEdge");
laraImport("clava.graphs.cfg.CfgEdgeType");
laraImport("clava.graphs.cfg.CfgUtils");
laraImport("clava.graphs.cfg.nodedata.DataFactory");
laraImport("clava.ClavaJoinPoints");

class CfgBuilder {
  /**
   * AST node to process
   */
  #jp;

  /**
   * Graph being built
   */
  #graph;

  /**
   * Maps stmts to graph nodes
   */
  #nodes;

  /**
   * The start node of the graph
   */
  #startNode;

  /**
   * The end node of the graph
   */
  #endNode;

  /**
   * Maps the astId to the corresponding temporary statement
   */
  #temporaryStmts;

  /**
   * {boolean} If true, uses deterministic ids for the graph ids (e.g. id_0, id_1...). Otherwise, uses $jp.astId whenever possible.
   */
  #deterministicIds;

  /**
   * Current id, in case deterministic ids are used
   */
  #currentId;

  /**
   * An instance of DataFactory, for creating graph node data
   */
  #dataFactory;

  constructor($jp, deterministicIds = false) {
    this.#jp = $jp;
    this.#deterministicIds = deterministicIds;
    this.#currentId = 0;
    this.#dataFactory = new DataFactory(this.#jp);

    // Load graph library
    Graphs.loadLibrary();

    this.#graph = cytoscape({
      /* options */
    });
    this.#nodes = new Map();

    // Create start and end nodes
    // Do not add them to #nodes, since they have no associated statements
    this.#startNode = Graphs.addNode(
      this.#graph,
      this.#dataFactory.newData(CfgNodeType.START, undefined, "start")
    );
    //this.#nodes.set('START', this.#startNode)
    this.#endNode = Graphs.addNode(
      this.#graph,
      this.#dataFactory.newData(CfgNodeType.END, undefined, "end")
    );
    //this.#nodes.set('END', this.#endNode)

    this.#temporaryStmts = {};
  }

  #nextId() {
    const nextId = "id_" + this.#currentId;
    this.#currentId++;
    return nextId;
  }

  /*
  static buildGraph($jp) {
    return new CfgBuilder($jp).build();
  }
  */

  build() {
    this._addAuxComments();
    this._createNodes();

    //this._fillNodes();
    this._connectNodes();

    this._cleanCfg();

    return [this.#graph, this.#nodes, this.#startNode, this.#endNode];
  }

  _addAuxComments() {
    for (const $currentJp of this.#jp.descendants) {
      if ($currentJp.instanceOf("scope")) {
        const $scopeStart = $currentJp.insertBegin(
          ClavaJoinPoints.comment("SCOPE_START")
        );
        this.#temporaryStmts[$scopeStart.astId] = $scopeStart;

        const $scopeEnd = $currentJp.insertEnd(
          ClavaJoinPoints.comment("SCOPE_END")
        );
        this.#temporaryStmts[$scopeEnd.astId] = $scopeEnd;
      }
    }
  }

  /**
   * Creates all nodes (except start and end), with only the leader statement
   */
  _createNodes() {
    // Test all statements for leadership
    // If they are leaders, create node
    for (const $stmt of Query.searchFromInclusive(this.#jp, "statement")) {
      //println("Is leader?: " + CfgUtils.isLeader($stmt) + ' -> ' +$stmt.line);

      if (CfgUtils.isLeader($stmt)) {
        this._getOrAddNode($stmt, true);
        //println("STMTS: " + newNode.data().stmts);

        // TODO: If INST_LIST, associate all other statements of the INST_LIST to the node?
      }
    }

    // Special case: if starting node is a statement and a graph node was not created for it (e.g. creating a graph starting from an arbitrary statement),
    // create one with the type INST_LIST
    if (
      this.#jp.instanceOf("statement") &&
      this.#nodes.get(this.#jp.astId) === undefined
    ) {
      this._getOrAddNode(this.#jp, true, CfgNodeType.INST_LIST);
    }
  }

  _connectNodes() {
    // Connect start
    let startAstNode = this.#jp;
    if (startAstNode.instanceOf("function")) {
      startAstNode = startAstNode.body;
    }

    if (!startAstNode.instanceOf("statement")) {
      throw new Error(
        "Not defined how to connect the Start node to an AST node of type " +
          this.#jp.joinPointType
      );
    }

    let afterNode = this.#nodes.get(startAstNode.astId);

    // Add edge
    Graphs.addEdge(
      this.#graph,
      this.#startNode,
      afterNode,
      new CfgEdge(CfgEdgeType.UNCONDITIONAL)
    );

    for (const astId of this.#nodes.keys()) {
      const node = this.#nodes.get(astId);

      // Only add connections for astIds of leader statements
      if (node.data().nodeStmt.astId !== astId) {
        continue;
      }

      const nodeType = node.data().type;

      if (nodeType === undefined) {
        //printlnObject( node.data());
        throw new Error("Node type is undefined: ");
        //continue;
      }

      // IF NODE - TODO, adapt to specific node data
      if (nodeType === CfgNodeType.IF) {
        const ifStmt = node.data().if;

        const thenStmt = ifStmt.then;
        const thenNode = this.#nodes.get(thenStmt.astId);

        Graphs.addEdge(
          this.#graph,
          node,
          thenNode,
          new CfgEdge(CfgEdgeType.TRUE)
        );

        const elseStmt = ifStmt.else;

        if (elseStmt !== undefined) {
          const elseNode = this.#nodes.get(elseStmt.astId);
          Graphs.addEdge(
            this.#graph,
            node,
            elseNode,
            new CfgEdge(CfgEdgeType.FALSE)
          );
        } else {
          // Usually there should always be a sibling, because of inserted comments
          // However, if an arbitary statement is given as the starting point,
          // sometimes there might not be nothing after. In this case, connect to the
          // end node.
          const after = CfgUtils.nextExecutedStmt(ifStmt, this.#jp);

          const afterNode =
            after !== undefined ? this.#nodes.get(after.astId) : this.#endNode;

          // Add edge
          Graphs.addEdge(
            this.#graph,
            node,
            afterNode,
            new CfgEdge(CfgEdgeType.FALSE)
          );
        }
      }

      if (nodeType === CfgNodeType.LOOP) {
        const $loop = node.data().loop;

        let afterStmt = undefined;

        switch ($loop.kind) {
          case "for":
            afterStmt = $loop.init;
            break;
          case "while":
            afterStmt = $loop.cond;
            break;
          case "dowhile":
            afterStmt = $loop.body;
            break;
          default:
            throw new Error("Case not defined for loops of kind " + $loop.kind);
        }

        const afterNode = this.#nodes.get(afterStmt.astId);
        Graphs.addEdge(
          this.#graph,
          node,
          afterNode,
          new CfgEdge(CfgEdgeType.UNCONDITIONAL)
        );
      }

      if (nodeType === CfgNodeType.COND) {
        // Get kind of loop
        const $condStmt = node.data().nodeStmt;
        const $loop = $condStmt.parent;
        isJoinPoint($loop, "loop");

        const kind = $loop.kind;
        // True - first stmt of the loop body
        const trueNode = this.#nodes.get($loop.body.astId);
        Graphs.addEdge(
          this.#graph,
          node,
          trueNode,
          new CfgEdge(CfgEdgeType.TRUE)
        );

        // False - next stmt of the loop
        const $nextExecutedStmt = CfgUtils.nextExecutedStmt($loop, this.#jp);

        // If undefined, there is no next statement
        const falseNode =
          $nextExecutedStmt !== undefined
            ? this.#nodes.get($nextExecutedStmt.astId)
            : this.#endNode;

        // Create edge
        Graphs.addEdge(
          this.#graph,
          node,
          falseNode,
          new CfgEdge(CfgEdgeType.FALSE)
        );
      }

      if (nodeType === CfgNodeType.INIT) {
        // Get loop
        const $initStmt = node.data().nodeStmt;
        const $loop = $initStmt.parent;
        isJoinPoint($loop, "loop");
        if ($loop.kind !== "for") {
          throw new Error("Not implemented for loops of kind " + $loop.kind);
        }

        const $condStmt = $loop.cond;
        if ($condStmt === undefined) {
          throw new Error(
            "Not implemented when for loops do not have a condition statement"
          );
        }

        const afterNode = this.#nodes.get($condStmt.astId);

        Graphs.addEdge(
          this.#graph,
          node,
          afterNode,
          new CfgEdge(CfgEdgeType.UNCONDITIONAL)
        );
      }

      if (nodeType === CfgNodeType.STEP) {
        // Get loop
        const $stepStmt = node.data().nodeStmt;
        const $loop = $stepStmt.parent;
        isJoinPoint($loop, "loop");
        if ($loop.kind !== "for") {
          throw new Error("Not implemented for loops of kind " + $loop.kind);
        }

        const $condStmt = $loop.cond;
        if ($condStmt === undefined) {
          throw new Error(
            "Not implemented when for loops do not have a condition statement"
          );
        }

        const afterNode = this.#nodes.get($condStmt.astId);
        Graphs.addEdge(
          this.#graph,
          node,
          afterNode,
          new CfgEdge(CfgEdgeType.UNCONDITIONAL)
        );
      }

      // INST_LIST NODE
      if (nodeType === CfgNodeType.INST_LIST) {
        //const stmts = node.data().getStmts();
        //const $lastStmt = stmts[stmts.length-1];
        const $lastStmt = node.data().getLastStmt();
        const $nextExecutedStmt = CfgUtils.nextExecutedStmt(
          $lastStmt,
          this.#jp
        );

        let afterNode = undefined;
        if ($nextExecutedStmt === undefined) {
          afterNode = this.#endNode;
        } else {
          afterNode = this.#nodes.get($nextExecutedStmt.astId);
          if (afterNode === undefined) {
            afterNode = this.#endNode;
          }
        }

        //println("Adding edge for node " + node.data().id)
        Graphs.addEdge(
          this.#graph,
          node,
          afterNode,
          new CfgEdge(CfgEdgeType.UNCONDITIONAL)
        );
      }

      // SCOPE_NODEs
      if (
        nodeType === CfgNodeType.SCOPE ||
        nodeType === CfgNodeType.THEN ||
        nodeType === CfgNodeType.ELSE
      ) {
        //const stmts = node.data().getStmts();
        const $scope = node.data().scope;

        // Scope connects to its own first statement that will be an INST_LIST
        let afterNode = this.#nodes.get($scope.firstStmt.astId);

        Graphs.addEdge(
          this.#graph,
          node,
          afterNode,
          new CfgEdge(CfgEdgeType.UNCONDITIONAL)
        );
      }
    }
  }

  _cleanCfg() {
    // Remove temporary instructions from the code
    for (const stmtId in this.#temporaryStmts) {
      this.#temporaryStmts[stmtId].detach();
    }

    /*
		println("Keys in nodes before:");
		for(const key of this.#nodes.keys()) {
			println("Key: " + key);
		}
		*/

    // Remove temporary instructions from the instList nodes and this.#nodes
    for (const node of this.#nodes.values()) {
      // Only inst lists need to be cleaned
      if (node.data().type !== CfgNodeType.INST_LIST) {
        const tempStmts = node
          .data()
          .stmts.filter(
            ($stmt) => this.#temporaryStmts[$stmt.astId] !== undefined
          );
        if (tempStmts.length > 0) {
          println(
            "Node '" + node.data().type + "' has temporary stmts: " + tempStmts
          );
        }
        continue;
      }

      // Filter stmts that are temporary statements

      const filteredStmts = [];
      for (const $stmt of node.data().stmts) {
        // If not a temporary stmt, add to filtered list
        if (this.#temporaryStmts[$stmt.astId] === undefined) {
          filteredStmts.push($stmt);
        }
        // Otherwise, remove from this.#nodes
        else {
          //					println("Deleting " + $stmt.astId);
          //					println("Before: " + this.#nodes.get($stmt.astId));
          //					println("Has: " + this.#nodes.has($stmt.astId));
          this.#nodes.delete($stmt.astId);
          //					println("After: " + this.#nodes.get($stmt.astId));
          //					println("Has: " + this.#nodes.has($stmt.astId));
        }
      }

      //node.data().stmts.filter($stmt => this.#temporaryStmts[$stmt.astId] === undefined);

      if (filteredStmts.length !== node.data().stmts.length) {
        //println("Replacing " + node.data().stmts + " with " + filteredStmts);
        node.data().stmts = filteredStmts;
      }

      //println("Node: " + node);
    }

    /*
		println("Keys in nodes after:");
		for(const key of this.#nodes.keys()) {
			println("Key: " + key);
		}
		*/

    // Remove empty instList CFG nodes
    for (const node of this.#graph.nodes()) {
      // Only nodes that are inst lists
      if (node.data().type !== CfgNodeType.INST_LIST) {
        continue;
      }

      // Only empty nodes
      if (node.data().stmts.length > 0) {
        continue;
      }

      // Get edges of node
      const edges = node.connectedEdges();

      // Get target of this node
      let targetNode = undefined;
      for (const edge of edges) {
        if (edge.source().equals(node)) {
          targetNode = edge.target();
          //println("Target node: " + targetNode.data());
        }
      }

      if (targetNode === undefined) {
        throw new Error("Could not find target node of node " + node.data().id);
      }

      for (const edge of edges) {
        // Add new connections for nodes that connect to this node
        if (edge.target().equals(node)) {
          Graphs.addEdge(
            this.#graph,
            edge.source(),
            targetNode,
            new CfgEdge(edge.data().type)
          );
        }

        //println("Edge: " + edge);
        //println("Edge source: " + edge.source().data());
        //println("Edge target: " + edge.target().data());
      }

      // Remove node
      node.remove();
      //println("REMOVE NODE: " + node.data());
    }

    /*
		// Add link to node in data()
		for(const node of this.#graph.nodes()) {
			node.data().node = node;
		}
		*/
  }

  /**
   * Returns the node corresponding to this statement, or creates a new one if one does not exist yet.
   */
  _getOrAddNode($stmt, create, forceNodeType) {
    const _create = create ?? false;
    let node = this.#nodes.get($stmt.astId);

    // If there is not yet a node for this statement, create
    if (node === undefined && _create) {
      const nodeType = forceNodeType ?? CfgUtils.getNodeType($stmt);
      const nodeId = this.#deterministicIds ? this.#nextId() : undefined;

      node = Graphs.addNode(
        this.#graph,
        this.#dataFactory.newData(nodeType, $stmt, nodeId)
      );

      // Associate all statements of graph node
      for (const $nodeStmt of node.data().stmts) {
        // Check if it has not been already added
        if (this.#nodes.get($nodeStmt.astId) !== undefined) {
          throw new Error(
            "Adding mapping twice for statement " +
              $nodeStmt.astId +
              "@" +
              $nodeStmt.location
          );
        }

        //println("Adding " + $nodeStmt.astId + " to node " + node.data().id);
        this.#nodes.set($nodeStmt.astId, node);
      }
      //this.#nodes.set($stmt.astId, node);
    } else {
      throw new Error("No node for statement at line " + $stmt.line);
    }

    return node;
  }
}
