laraImport("lara.graphs.Graphs")
laraImport("clava.graphs.CfgNode")
laraImport("lara.Strings")
laraImport("lara.Check")

class CfgBuilder {
	
	/**
	 * Scope to process
	 */
	#scope;
	/**
	 * Graph being built
	 */
	#graph;
	/**
	 * Maps stmts to nodes
	 */
	#nodes; 
	
	constructor($scope) {
		this.#scope = $scope;
		
		// Load graph library
		Graphs.loadLibrary();
		
		this.#graph = cytoscape({ /* options */ });
		this.#nodes = {};
	}
	
	static fromScope($jp) {
		return new CfgBuilder($jp).build();
	}
	
	build() {
		this._findBbs(this.#scope);
		
		return this.#graph;
	}
	
	/**
	 * Starts all basic blocks, with only the leader statement
	 */
	_findBbs($jp) {
		
		// Test all statements for leadership
		// If they are leaders, create node
		for(const $stmt of Query.searchFromInclusive($jp, "stmt")) {
			const leaders = this._getLeaders($stmt);
			for(const leader of leaders) {
				this._getOrAddNode(leader);
			}
		}
		
	}
			
	
	/** 
	 * @return {$stmt[]} the leaders obtained from this statement, or undefined if no leader found
	 */
	_getLeaders($stmt, isLeader) {
		
		// Dead with undefined
		if($stmt === undefined) {
			return [];
		}
		
		// If stmt, then and else are leaders
		if($stmt.instanceOf("if")) {
			return _getLeaders($stmt.then).concat(_getLeaders($stmt.else));			
		}
		
		// Loop stmt, body is leader
		if($stmt.instanceOf("loop")) {
			return _getLeaders($stmt.body);
		}
		
		// Goto stmt, target is a leader
		//if($stmt.)
		//_getLeaders();
		
		// Check if this statement is a leader
		
		// If first instruction of scope, is a leader, unless the parent of the scope is another scope
		
		return [];
	}
	
	_findBbsOld($scope) {

		let currentStmts = $scope.stmts;
		let firstStmt = currentStmts.length > 0 ? currentStmts[0] : undefined;
		let scopeNode = undefined;
		
		// Iterate over all statements in the scope
		while(currentStmts.length > 0) {
			const $stmt = currentStmts.shift();
			
			// Anonymous scopes get merged into current scope
			if($stmt.instanceOf("scope")) {
				// Add scope statements to the beginning of current stmts
				currentStmts = $stmt.stmts.concat(currentStmts);
				
				// Fix in case scope is the first statement
				if($stmt.equals(firstStmt)) {
					
					firstStmt = currentStmts[0];
				}
				
				// Jump to next statement
				continue;
			}
			
			// First stmt of the scope
            if (firstStmt.equals($stmt)) {
				Check.isUndefined(scopeNode);
                scopeNode = this._getOrAddNode($stmt);
            }
            
            Check.isDefined(scopeNode);
            

			// Ifs generate one, possible two nodes (then/else) that merge at the end
            if($stmt.instanceOf("if")) {
	            
	            // 2) targets of a jmp
                const then = $stmt.then;
                
                
                /*
                CompoundStmt t = ((IfStmt) stmt).getThen().get();
                Stmt tChild = (Stmt) t.getChild(0);
                addToMap(tChild);

                Optional<CompoundStmt> e = ((IfStmt) stmt).getElse();
                if (e.isPresent()) {
                    Stmt eChild = (Stmt) e.get().getChild(0);
                    addToMap(eChild);
                }
                */
			}
            
            // gotos?
            
            // Any other statement, just add to current node
            scopeNode.data().addStmt($stmt);
		}
		
		/**
		    for (Stmt stmt : body.getChildren(Stmt.class)) {

            if (stmt instanceof IfStmt) {

                // 2) targets of a jmp
                CompoundStmt t = ((IfStmt) stmt).getThen().get();
                Stmt tChild = (Stmt) t.getChild(0);
                addToMap(tChild);

                Optional<CompoundStmt> e = ((IfStmt) stmt).getElse();
                if (e.isPresent()) {
                    Stmt eChild = (Stmt) e.get().getChild(0);
                    addToMap(eChild);
                }

                // 3) stmt following a jmp
                List<ClavaNode> ifSiblings = stmt.getRightSiblings();
                if (!ifSiblings.isEmpty()) {

                    Stmt ifSibling = (Stmt) ifSiblings.get(0);
                    addToMap(ifSibling);
                } else {

                    // TODO: check if this is actually needed (isn't the parent loop always a
                    // candidate?
                    // if it has no siblings, it's either the last stmt
                    // of the function or the last stmt of an intermediate scope
                    Optional<ClavaNode> possibleParentLoop = stmt.getAscendantsStream()
                            .filter(a -> a instanceof LoopStmt).findFirst();

                    if (possibleParentLoop.isPresent()) {
                        Stmt parentLoop = (Stmt) possibleParentLoop.get();
                        addToMap(parentLoop);
                    }
                }

                // recursively get the leaders from the if scope
                findBBs(t);
                if (e.isPresent()) {
                    findBBs(e.get());
                }
            }

            if (stmt instanceof LoopStmt) {

                CompoundStmt b = ((LoopStmt) stmt).getBody();

                // 2) targets of a jmp
                Stmt bChild = (Stmt) b.getChild(0);
                addToMap(stmt);
                addToMap(bChild);

                // 3) stmt following a jmp
                List<ClavaNode> loopSiblings = stmt.getRightSiblings();
                if (!loopSiblings.isEmpty()) {

                    Stmt loopSibling = (Stmt) loopSiblings.get(0);
                    addToMap(loopSibling);
                } else {

                    // TODO: check if this is actually needed (isn't the parent loop always a
                    // candidate?
                    // if it has no siblings, it's either the last stmt
                    // of the function or the last stmt of an intermediate scope
                    Optional<ClavaNode> possibleParentLoop = stmt.getAscendantsStream()
                            .filter(a -> a instanceof LoopStmt).findFirst();

                    if (possibleParentLoop.isPresent()) {
                        Stmt parentLoop = (Stmt) possibleParentLoop.get();
                        addToMap(parentLoop);
                    }
                }

                // recursively get the leaders from the if scope
                findBBs(b);
            }
        }
		
		 */
	}
	
	/**
	 * Returns the node corresponding to this statement, or creates a new one if one does not exist yet.
	 */
	_getOrAddNode($stmt) {
		let node = this.#nodes[$stmt];
		
		if(node === undefined) {
			//const nodeData = new CfgNode($stmt);
			//println("Node data id: " + nodeData.id)
			//node = this.#graph.add({ group: 'nodes', data: nodeData});
			//node = Graphs.addNode(this.#graph, new CfgNode($stmt));
			//node = this._addNode($stmt);
			node = Graphs.addNode(this.#graph, new CfgNode($stmt));
			//println("Node id: " + node.id())
			this.#nodes[$stmt] = node;
		}
		
		return node;
	}

/*
	_addNode($stmt) {
		return Graphs.addNode(this.#graph, new CfgNode($stmt));
	}
*/	
}