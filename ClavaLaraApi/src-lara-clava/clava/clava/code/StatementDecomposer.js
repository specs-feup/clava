class StatementDecomposer {

	constructor(tempPrefix, startIndex) {	
		this.tempPrefix = tempPrefix !== undefined ? tempPrefix : "decomp_";
		this.startIndex = startIndex !== undefined ? startIndex : 0;
		
		Check.isString(this.tempPrefix);
		Check.isNumber(this.startIndex);		
	}

	#throwNotImplemented(generalType, specificType) {
		throw new Error("StatementDecomposer not implemented for "+generalType+" of type '"+specificType+"'");
	}
	
	#newTempVarname() {
		let varName = this.tempPrefix + this.startIndex;
		this.startIndex++;
		return varName;
	}

	/**
	* If the given statement can be decomposed in two or more statements, replaces the statement with the decomposition.
	*
	* @param {$statement} $stmt - A statement that will be decomposed.
	*/
	decomposeAndReplace($stmt) {
		let stmts = this.decompose($stmt);
		
		// No statements to replace
		if(stmts.length === 0) {
			return;
		}
				
		// Insert all statements in order, before original statement
		for(let $newStmt of stmts) {
			$stmt.insertBefore($newStmt);
		}
		
		// Remove original statement
		$stmt.detach();
	}

	/**
	* @param {$statement} $stmt - A statement that will be decomposed.
	* @return {$statement[]} An array with the new statements, or an empty array if no decomposition could be made
	*/
	decompose($stmt) {
		try {
			return this.decomposeStmt($stmt);
		} catch(e) {
			println("StatementDecomposer: " + e);
			return [];
		}
  	}

	decomposeStmt($stmt) {
		// Unsupported
		if($stmt.instanceOf("scope") || $stmt.joinPointType === 'statement') {
			return [];
		}
		
		if($stmt.instanceOf("exprStmt")) {
			return this.decomposeExprStmt($stmt);
		}

		if($stmt.instanceOf("returnStmt")) {
			return this.decomposeReturnStmt($stmt);
		}
	
	
		debug("StatementDecomposer: not implemented for statement of type " + $stmt.joinPointType);
		return [];
	}
	

	decomposeExprStmt($stmt) {
		
		// Statement represents an expression
		let $expr = $stmt.expr;
		
		return this.decomposeExpr($expr).stmts;
	}
	
	decomposeReturnStmt($stmt) {
		
		// Return may contain an expression
		let $expr = $stmt.returnExpr;
		
		if($expr === undefined) {
			return [];
		}
		
		let decomposeResult = this.decomposeExpr($expr);

		let newStmts = decomposeResult.stmts;
		let $newReturnStmt = ClavaJoinPoints.returnStmt(decomposeResult.$resultExpr);
		
		newStmts.push($newReturnStmt);
		
		return newStmts;
	}
	
	/**
	 * @return {DecomposeResult}
	 */
	decomposeExpr($expr) {
		
		if($expr.instanceOf("binaryOp")) {
			return this.decomposeBinaryOp($expr);
		}
		
		if($expr.numChildren === 0) {
		//if($expr.instanceOf("varref") || $expr.instanceOf("literal")) {
			let stmts = [];
			let dec = new DecomposeResult(stmts, $expr);

			return new DecomposeResult(stmts, $expr);
		}

		this.#throwNotImplemented("expressions", $expr.joinPointType);	
	}
	
	decomposeBinaryOp($binaryOp) {
		
		let kind = $binaryOp.kind;
		
		if(kind === "assign") {
			let stmts = [];
			
			// Get statements of right hand-side
			let rightResult = this.decomposeExpr($binaryOp.right);
			stmts = stmts.concat(rightResult.stmts);

			// Add assignment
			let $newAssign = ClavaJoinPoints.assign($binaryOp.left, rightResult.$resultExpr);
			stmts.push(ClavaJoinPoints.exprStmt($newAssign));
			
			return new DecomposeResult(stmts, $binaryOp.left);			
		} 
		// TODO: Not taking into account += and other cases
		else {
			// Apply decompose to both sides
			let leftResult = this.decomposeExpr($binaryOp.left);

			let rightResult = this.decomposeExpr($binaryOp.right);
			
			let stmts = [];
			stmts = stmts.concat(leftResult.stmts);
			stmts = stmts.concat(rightResult.stmts);
			
			// Create operation with result of decomposition
			let $newExpr = ClavaJoinPoints.binaryOp($binaryOp.kind, leftResult.$resultExpr, rightResult.$resultExpr, $binaryOp.type);
			
			// Create declaration statement with result to new temporary variable
			let tempVarname = this.#newTempVarname();
			let tempVarDecl = ClavaJoinPoints.varDecl(tempVarname, $newExpr);
			stmts.push(tempVarDecl.stmt);

			return new DecomposeResult(stmts, ClavaJoinPoints.varRefFromDecl(tempVarDecl));					
		}

		this.#throwNotImplemented("binary operators", kind);	
	}
	
	
}