laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("lara.graphs.NodeData");
laraImport("clava.ClavaJoinPoints");

/**
 * The data of a CFG node.
 */
class CfgNodeData extends NodeData {
	
	/**
	 * The statement that originated this node
	 */
	#nodeStmt;

	//stmts;
	#type;

	#name;

	// Internal reference to the node of this data
	//#node

	// For caching
	//#targetTrue
	//#targetFalse
	//#targetUncond	
	
	constructor(cfgNodeType, $stmt, id) {
		// If id defined, give priority to it. Othewise, use stmt astId, if defined
		const _id = id !== undefined ? id : $stmt === undefined ? undefined : $stmt.astId;

		//const id = $stmt === undefined ? undefined : $stmt.astId;
		//println("Id: " + id)
		// Use AST node id as graph node id
		super(_id);

		this.#nodeStmt = $stmt;
		/**if(cfgNodeType === undefined) {
			throw new Error("Must define a cfg node type");
		}*/

		//this.stmts = [];
		
		// If statement defined, add it to list of statements
		/*
		if($stmt !== undefined) {
			this.stmts.push($stmt);			
		}
		*/

		this.#type = cfgNodeType;

		//this.#node = undefined;
	}

	/*
	set node(node) {
		this.#node = node;
	}
	*/

	get type() {
		return this.#type;
	}

	get name() {
		if(this.#name === undefined) {
			const typeName = this.#type.name;
			this.#name = typeName.substring(0,1).toUpperCase() + typeName.substring(1,typeName.length).toLowerCase();
		}

		return this.#name;
		//return this.type.name; 
	}

	/*
	addStmt($stmt) {
		this.stmts.push($stmt);
	}

	getStmts() {
		return this.stmts
	}
	*/

	get nodeStmt() {
		return this.#nodeStmt;
	}

	set nodeStmt($stmt) {
		this.#nodeStmt = $stmt;
	}

	/**
	 * The stmts associated with this CFG node.
	 */
	get stmts() {
		// By default, the list only containts the node statement
		return this.nodeStmt !== undefined ? [this.nodeStmt] : [];
	}

	toString() {

		// By default, content of the node is the name of the type
		return this.name.substring(0,1).toUpperCase() + this.name.substring(1,this.name.length).toLowerCase();

		/*
		// Special cases

		if(this.type === CfgNodeType.START) {
			return "Start";
		}
		
		if(this.type === CfgNodeType.END) {
			return "End";
		}
		
		let code = "";
		
		// TODO: Each type of statement needs its "toString", for instance, a for should only print its header
		for(let $stmt of this.stmts) {
			var nodeType = CfgUtils.getNodeType($stmt);
			
			//println("Node type: " + nodeType)
			//println("Node name: " + nodeType.name)
			
			//const showStatements = nodeType === CfgNodeType.SCOPE || nodeType === CfgNodeType.THEN || nodeType === CfgNodeType.ELSE || nodeType === undefined;

			let stmtCode = nodeType !== undefined ? nodeType.name : $stmt.code;
			//let stmtCode = showStatements ? $stmt.code : nodeType.name;
			
			//println("Code: " + stmtCode);
			
			//code += stmtCode.replaceAll("\n", "\\l").replaceAll("\r", "");
			//code += "\\l";
			
			code += stmtCode + "\n";
		}
		
		return code;
		*/
	}

	/**
	 * 
	 * @returns true if this is a branch node, false otherwise. If this is a branch node, contains two edges, true and false. 
	 * If not, contains only one uncoditional edge (expect if it is the end node, which contains no edges). 
	 */
	isBranch() {
		return false;
	}

	/*
	get targetTrue() {
		if(this.#targetTrue === undefined) {
			this.#targetTrue = this.#getTarget(CfgEdgeType.TRUE);						
		}	

		return this.#targetTrue;
	}

	get targetFalse() {

	}

	get targetUncond() {

	}
	*/	
/*
	#getTarget(edgeType) {
		let target = undefined;
			
		for(const edge of this.#node.connectedEdges()) {
			
			// Only targets of this node
			if(edge.source() !== this.#node) {
				continue;
			}
			
			if(edge.data().type === edgeType) {
				if(target !== undefined) {
					throw new Error("Found duplicated edge of type '"+edgeType+"' in node " + this.#node.data());
				}

				target = edge.target();
			}
		}
			

			//const trueTargets = this.#node.connectedEdges().targets(edge => {printlnObject(edge.data());return true;});
			//const trueTargets = this.#node.connectedEdges().targets(edge => {println("TARGEGTTT: " + edge);return  edge.data().type === CfgEdgeType.TRUE;});
			//const trueTargets = this.#node.edges().targets();			
			//println("LEngth: " + trueTargets.length);
		
			return target;
	}
*/
}

