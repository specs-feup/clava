laraImport("lara.graphs.Graphs")
laraImport("clava.graphs.CfgNode")
laraImport("lara.Strings")

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
	
	_findBbs($scope) {
		
		// Iterate over all statements in the scope
		for(var $stmt of $scope.stmts) {
			// 1) first stmt of the scope
            if ($scope.firstStmt.equals($stmt)) {
                this._getOrAddNode($stmt);
                continue;
            }
		}
		
		/**
		    for (Stmt stmt : body.getChildren(Stmt.class)) {

            // 1) first stmt of the scope
            if (body.getChild(0).equals(stmt)) {

                addToMap(stmt);
            }

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
			const nodeData = new CfgNode($stmt);
			//println("Node data id: " + nodeData.id)
			node = this.#graph.add({ group: 'nodes', data: nodeData});
			//println("Node id: " + node.id())
			this.#nodes[$stmt] = node;
		}
		
		return node;
	}

	
}