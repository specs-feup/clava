laraImport("lara.graphs.Graphs")
laraImport("clava.graphs.CfgNode")
laraImport("clava.graphs.CfgNodeType");
laraImport("clava.graphs.CfgEdge")
laraImport("clava.graphs.CfgEdgeType");
laraImport("clava.graphs.Cfg");
laraImport("clava.graphs.CfgUtils");
laraImport("lara.Strings")
laraImport("lara.Check")

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
	
	constructor($jp) {
		this.#jp = $jp;
		
		// Load graph library
		Graphs.loadLibrary();
		
		this.#graph = cytoscape({ /* options */ });
		this.#nodes = {};
		
		// Create start and end node
		this.#startNode = Graphs.addNode(this.#graph, new CfgNode(CfgNodeType.START));
		this.#endNode = Graphs.addNode(this.#graph, new CfgNode(CfgNodeType.END));
	}
	
	static buildGraph($jp) {
		return new CfgBuilder($jp).build();
	}
	
	build() {
		this._createNodes();
		this._fillNodes();	
		this._connectNodes();		
		
		return this.#graph;
	}
	
	
	/**
	 * Creates all nodes (except start and end), with only the leader statement
	 */
	_createNodes() {
		//println("Start jp: " + this.#jp.dump);
		
		// Test all statements for leadership
		// If they are leaders, create node
		for(const $stmt of Query.searchFromInclusive(this.#jp, "statement")) {
			//println("Stmt: " + $stmt.code);
			//println("Is leader?: " + CfgUtils.isLeader($stmt));			
			if(CfgUtils.isLeader($stmt)) {
				this._getOrAddNode($stmt);
			}
		}
		
	}


	_fillNodes() {
		// TODO

		// Special case: if the leader is a scope, you should replace the first statement 
		// (which is of type scope) with the first statement of the scope, and add statements
		// until a leader statement appears. 
		// If the first statement of the scope is a leader statement, the graph node should have
		// 0 statements

	}
	
	_connectNodes() {
		// TODO

	}			
	

	
	
	/**
	 * Returns the node corresponding to this statement, or creates a new one if one does not exist yet.
	 */
	_getOrAddNode($stmt) {
		let node = this.#nodes[$stmt];
		
		// If there is not yet a node for this statement, create
		if(node === undefined) {
			const nodeType = CfgUtils.getNodeType($stmt);
			node = Graphs.addNode(this.#graph, new CfgNode(nodeType, $stmt));
			this.#nodes[$stmt] = node;
			
			// Example of how to add an edge:
			//Graphs.addEdge(this.#graph, this.#startNode, node, new CfgEdge(CfgEdgeType.TRUE));
		}
		
		return node;
	}
	
	
}