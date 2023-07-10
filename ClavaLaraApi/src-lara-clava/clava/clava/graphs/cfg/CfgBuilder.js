laraImport("lara.graphs.Graphs");
laraImport("lara.Strings");
laraImport("lara.Check");
laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("clava.graphs.cfg.CfgEdge");
laraImport("clava.graphs.cfg.CfgEdgeType");
laraImport("clava.graphs.cfg.CfgUtils");
laraImport("clava.graphs.cfg.NextCfgNode");
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

  /**
   * Calculates what node is unconditionally executed after a given statement
   */
  #nextNodes;

  /**
   * Creates a new instance of the CfgBuilder class
   * @param {joinpoint} $jp 
   * @param {boolean} deterministicIds If true, uses deterministic ids for the graph ids (e.g. id_0, id_1...). Otherwise, uses $jp.astId whenever possible
   */
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
    this.#nextNodes = new NextCfgNode(this.#jp, this.#nodes, this.#endNode);
  }

  /**
   * Returns the next id that will be used to identify a graph node
   */
  #nextId() {
    const nextId = "id_" + this.#currentId;
    this.#currentId++;
    return nextId;
  }

  /**
   * Connects two nodes using a directed edge
   * @param {Cytoscape.node} source starting point from which the edge originates
   * @param {Cytoscape.node} target destination point to which the edge points
   * @param {CfgEdgeType} edgeType the edge label that connects
   */
  #addEdge(source, target, edgeType) {
    // Add edge
    Graphs.addEdge(this.#graph, source, target, new CfgEdge(edgeType));
  }

  /**
   * Builds the control flow graph
   * @returns {Array} a array that includes the built graph, the nodes, the start and end nodes
   */
  build() {
    this._addAuxComments();
    this._createNodes();

    //this._fillNodes();
    this._connectNodes();

    this._cleanCfg();

    // TODO: Check graph invariants
    // 1. Each node has either one unconditional outgoing edge,
    // or two outgoing edges that must be a pair true/false,
    // or if there is no outgoing edge must be the end node

    return [this.#graph, this.#nodes, this.#startNode, this.#endNode];
  }

  /**
   * Inserts comments that specify the beginning and end of a scope
   */
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
      if (CfgUtils.isLeader($stmt)) {
        this._getOrAddNode($stmt, true);
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

  /**
   * Connects a node associated with a statement that is an instance of a "if" statement.
   * @param {Cytoscape.node} node node whose type is "IF"
   */
  #connectIfNode(node) {
    const ifStmt = node.data().if;

    const thenStmt = ifStmt.then;
    const thenNode = this.#nodes.get(thenStmt.astId);

    this.#addEdge(node, thenNode, CfgEdgeType.TRUE);

    const elseStmt = ifStmt.else;

    if (elseStmt !== undefined) {
      const elseNode = this.#nodes.get(elseStmt.astId);
      this.#addEdge(node, elseNode, CfgEdgeType.FALSE);
    } else {
      // Usually there should always be a sibling, because of inserted comments
      // However, if an arbitary statement is given as the starting point,
      // sometimes there might not be nothing after. In this case, connect to the
      // end node.
      const afterNode = this.#nextNodes.nextExecutedNode(ifStmt);

      // Add edge
      this.#addEdge(node, afterNode, CfgEdgeType.FALSE);
    }
  }

  /**
   * Connects a node associated with a statement that is an instance of a "loop" statement.
   * @param {Cytoscape.node} node node whose type is "LOOP"
   */
  #connectLoopNode(node) {
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

    const afterNode = this.#nodes.get(afterStmt.astId) ?? this.#endNode;

    this.#addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
  }

   /**
   * Connects a node associated with a statement that is part of a loop header and corresponds to the loop condition
   * @param {Cytoscape.node} node node whose type is "COND"
   */
  #connectCondNode(node) {
     // Get kind of loop
     const $condStmt = node.data().nodeStmt;
     const $loop = $condStmt.parent;
     isJoinPoint($loop, "loop");

     const kind = $loop.kind;
     // True - first stmt of the loop body
     const trueNode = this.#nodes.get($loop.body.astId) ?? this.#endNode;
     this.#addEdge(node, trueNode, CfgEdgeType.TRUE);

     // False - next stmt of the loop
     const falseNode = this.#nextNodes.nextExecutedNode($loop);

     // Create edge
     this.#addEdge(node, falseNode, CfgEdgeType.FALSE);
  }

   /**
   * Connects a node associated with a statement that is an instance of a "break" statement.
   * @param {Cytoscape.node} node node whose type is "BREAK"
   */
  #connectBreakNode(node) {
    const $breakStmt = node.data().nodeStmt;
    const $loop = $breakStmt.ancestor("loop");
    const $switch = $breakStmt.ancestor("switch");
    const loopDepth = ($loop !== undefined) ? $loop.depth : -1;
    const switchDepth = ($switch !== undefined) ? $switch.depth : -1;
    let afterNode = undefined;

    if (loopDepth > switchDepth)  // Statement is used to terminate a loop
      afterNode = this.#nextNodes.nextExecutedNode($loop);

    else  // Statement is used to exit a switch block
      afterNode = this.#nextNodes.nextExecutedNode($switch);

    this.#addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL); 
  }

   /**
   * Connects a node associated with a statement that is an instance of a "continue" statement.
   * @param {Cytoscape.node} node node whose type is "CONTINUE"
   */
  #connectContinueNode(node) {
    const $continueStmt = node.data().nodeStmt;
    const $loop = $continueStmt.ancestor("loop");
    
    const $afterStmt = ($loop.kind === "for") ? $loop.step : $loop.cond;
    const afterNode = this.#nodes.get($afterStmt.astId) ?? this.#endNode; 

    this.#addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
  }

  /**
   * Connects a node associated with a statement that is an instance of a "switch" statement.
   * @param {Cytoscape.node} node node whose type is "SWITCH"
   */
  #connectSwitchNode(node) {
    const $switchStmt = node.data().switch;
    let firstReachedCase = undefined;

    // The first reached case is the first non-default case. 
    // If the switch only has one case statement, and it is the default case, then this default case will be the first reached case
    for(const $case of $switchStmt.cases) { 
      firstReachedCase = this.#nodes.get($case.astId);
      
      if(!$case.isDefault)
        break;
    }
    this.#addEdge(node, firstReachedCase, CfgEdgeType.UNCONDITIONAL);
  }

  /**
   * Connects a node associated with a statement that is an instance of a "case" statement.
   * @param {Cytoscape.node} node node whose type is "CASE"
   */
  #connectCaseNode(node) {
    const $caseStmt = node.data().case;
    const $switchStmt = $caseStmt.ancestor("switch");
    const firstExecutedInst = this.#nextNodes.nextExecutedNode($caseStmt);
    const postSwitchNode = this.#nextNodes.nextExecutedNode($switchStmt);

    const $nextCaseStmt = CfgUtils.getNextCaseStmt($caseStmt, this.#nodes);
    const nextCaseNode = ($nextCaseStmt !== undefined) ? this.#nodes.get($nextCaseStmt.astId) : undefined;
    
    // Connect the node to the first instruction to be executed
    if ($caseStmt.isDefault) 
      this.#addEdge(node, firstExecutedInst, CfgEdgeType.UNCONDITIONAL); 
    else
      this.#addEdge(node, firstExecutedInst, CfgEdgeType.TRUE); 


    // Connect the node to the next case to be tested if the current case statement is false
    // or to the statement following the switch
    if (nextCaseNode !== undefined) { // Not the last case
      const $nextNextCaseStmt = CfgUtils.getNextCaseStmt($nextCaseStmt, this.#nodes);
      const nextNextCaseNode = ($nextNextCaseStmt !== undefined) ? this.#nodes.get($nextNextCaseStmt.astId) : undefined;

      if ($nextCaseStmt.isDefault && nextNextCaseNode !== undefined) // Next case is an intermediate default case
        this.#addEdge(node, nextNextCaseNode, CfgEdgeType.FALSE); // Connect node to the CASE node following the intermediate default 
      else if (!$caseStmt.isDefault) // Not an intermediate default case
        this.#addEdge(node, nextCaseNode, CfgEdgeType.FALSE);
    }
    else if (!$caseStmt.isDefault) {
      if ($switchStmt.hasDefaultCase) { // Switch statement has an intermediate default case
        const defaultCaseNode = this.#nodes.get($caseStmt.getDefaultCase.astId);
        this.#addEdge(node, defaultCaseNode, CfgEdgeType.FALSE);
      }
      else
        this.#addEdge(node, postSwitchNode, CfgEdgeType.FALSE);
    }
  }
  
  /**
   * Connects a node associated with a statement that is part of a loop header and corresponds to the loop initialization
   * @param {Cytoscape.node} node node whose type is "INIT"
   */
  #connectInitNode(node) {
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

    const afterNode = this.#nodes.get($condStmt.astId) ?? this.#endNode;
    this.#addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
  }

  /**
   * Connects a node associated with a statement that is part of a loop header and corresponds to the loop step
   * @param {Cytoscape.node} node node whose type is "STEP"
   */
  #connectStepNode(node) {
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

    const afterNode = this.#nodes.get($condStmt.astId) ?? this.#endNode;
    this.#addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
  }

  /**
   * @param {Cytoscape.node} node node whose type is "INST_LIST"
   */
  #connectInstListNode(node) {
    const $lastStmt = node.data().getLastStmt();

    const afterNode = this.#nextNodes.nextExecutedNode($lastStmt);
    this.#addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
  }

  /**
   * Connects a node associated with a statement that is an instance of a "return" statement.
   * @param {Cytoscape.node} node node whose type is "RETURN"
   */
  #connectReturnNode(node) {
    this.#addEdge(node, this.#endNode, CfgEdgeType.UNCONDITIONAL);
  }

  /**
   * Connects a node associated with a statement that is an instance of a "scope" statement.
   * @param {Cytoscape.node} node node whose type is "SCOPE", "THEN" or "ELSE"
   */
  #connectScopeNode(node) {
    const $scope = node.data().scope;

    // Scope connects to its own first statement that will be an INST_LIST
    let afterNode = this.#nodes.get($scope.firstStmt.astId);
    this.#addEdge(node, afterNode, CfgEdgeType.UNCONDITIONAL);
  }

  /**
   * Connects the leader statement nodes according to their type
   */
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
    this.#addEdge(this.#startNode, afterNode, CfgEdgeType.UNCONDITIONAL);

    for (const astId of this.#nodes.keys()) {
      const node = this.#nodes.get(astId);

      // Only add connections for astIds of leader statements
      if (node.data().nodeStmt.astId !== astId)
        continue;

      const nodeType = node.data().type;

      if (nodeType === undefined) {
        throw new Error("Node type is undefined: ");
        //continue;
      }

      switch(nodeType) {
        case CfgNodeType.IF:
          this.#connectIfNode(node); 
        break;
        case CfgNodeType.LOOP:
          this.#connectLoopNode(node); 
        break;
        case CfgNodeType.COND:
          this.#connectCondNode(node); 
        break;
        case CfgNodeType.BREAK:
          this.#connectBreakNode(node); 
        break;
        case CfgNodeType.CONTINUE:
          this.#connectContinueNode(node); 
        break;
        case CfgNodeType.SWITCH:
          this.#connectSwitchNode(node); 
        break;
        case CfgNodeType.CASE:
          this.#connectCaseNode(node); 
        break;
        case CfgNodeType.INIT:
          this.#connectInitNode(node); 
        break;
        case CfgNodeType.STEP:
          this.#connectStepNode(node); 
        break;
        case CfgNodeType.INST_LIST:
          this.#connectInstListNode(node); 
        break;
        case CfgNodeType.RETURN:
          this.#connectReturnNode(node); 
        break;
        case CfgNodeType.SCOPE:
        case CfgNodeType.THEN:
        case CfgNodeType.ELSE:
          this.#connectScopeNode(node); 
        break;
      }
    }
  }

  _cleanCfg() {
    // Remove temporary instructions from the code
    for (const stmtId in this.#temporaryStmts) {
      this.#temporaryStmts[stmtId].detach();
    }

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
          this.#nodes.delete($stmt.astId);
        }
      }

      if (filteredStmts.length !== node.data().stmts.length) {
        node.data().stmts = filteredStmts;
      }
    }

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

      // Remove node, replacing the connections with a new connection of the same type and the incoming edge
      // of the node being removed
      Graphs.removeNode(
        this.#graph,
        node,
        (incoming, outgoing) => new CfgEdge(incoming.data().type)
      );
    }

    // Remove nodes that have no incoming edge and are not start
    for (const node of this.#graph.nodes()) {
      // Only nodes that are not start
      if (node.data().type === CfgNodeType.START) {
        continue;
      }

      // Ignore nodes with incoming edges
      if (node.incomers().length > 0) {
        continue;
      }

      // Remove node
      debug(
        "[CfgBuilder] Removing statement that is not executed (e.g. is after a return): " +
          node.stmts
      );

      Graphs.removeNode(this.#graph, node);
    }
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

        this.#nodes.set($nodeStmt.astId, node);
      }
    } else {
      throw new Error("No node for statement at line " + $stmt.line);
    }

    return node;
  }
}
