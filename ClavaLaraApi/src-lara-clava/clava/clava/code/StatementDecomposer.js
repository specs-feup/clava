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
		var varName = this.tempPrefix + this.startIndex;
		this.startIndex++;
		return varName;
	}

	/**
	* If the given statement can be decomposed in two or more statements, replaces the statement with the decomposition.
	*
	* @param {$statement} $stmt - A statement that will be decomposed.
	*/
	decomposeAndReplace($stmt) {
		var stmts = this.decompose($stmt);
		
		// No statements to replace
		if(stmts.length === 0) {
			return;
		}
		
		println("Old stmt: " + $stmt.code);
		
		// Insert all statements in order, before original statement
		for(var $newStmt of stmts) {
			println("New stmt: " + $newStmt.code);
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
			//println(e + "\n" + e.stack);
			return [];
		}
//		println("StatementDecomposer: not implemented for statement of type '"+$stmt.joinPointType+"', returning undefined");
//		return undefined;
		
		//println($stmt);
    	//return new StatementDecomposer($stmt).decompose();
  	}

	decomposeStmt($stmt) {
		if($stmt.instanceOf("exprStmt")) {
			return this.decomposeExprStmt($stmt);
		}

		if($stmt.instanceOf("returnStmt")) {
			return this.decomposeReturnStmt($stmt);
		}
	
	
		debug("StatementDecomposer: not implemented for statement of type " + $stmt.joinPointType);
		return [];
		//this.throwNotImplemented("statement", $stmt.joinPointType);	
	}
	

/*
	decompose() {
		if(this.$stmt.instanceOf("exprStmt")) {
			return this.decomposeExprStmt().stmts;
		}
	
		println("StatementDecomposer: not implemented for statement of type '"+this.$stmt.joinPointType+"', returning undefined");
		return undefined;
	}
*/	
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
		
		var decomposeResult = this.decomposeExpr($expr);

		let newStmts = decomposeResult.stmts;
		var $newReturnStmt = ClavaJoinPoints.returnStmt(decomposeResult.$resultExpr);
		
		newStmts.push($newReturnStmt);
		
		return newStmts;
	}
	
	/**
	 * @return {DecomposeResult}
	 */
	decomposeExpr($expr) {
		//println("Decompose expr " + $expr.code);
		
		if($expr.instanceOf("binaryOp")) {
			return this.decomposeBinaryOp($expr);
		}
		
		if($expr.instanceOf("varref") || $expr.instanceOf("literal")) {
		//if(!$expr.hasChildren) {
			let stmts = [];
			let dec = new DecomposeResult(stmts, $expr);
			println("DEC: " + dec);
			return new DecomposeResult(stmts, $expr);
		}

		this.#throwNotImplemented("expressions", $expr.joinPointType);	
		
//		println("StatementDecomposer: not implemented for expressions of type '"+$expr.joinPointType+"', returning undefined");
//		return undefined;
	}
	
	decomposeBinaryOp($binaryOp) {
		//println("Decompose bop " + $binaryOp.code);
	
		let kind = $binaryOp.kind;
		
		if(kind === "assign") {
			let stmts = [];
			
			// Get statements of right hand-side
			let rightResult = this.decomposeExpr($binaryOp.right);
			stmts = stmts.concat(rightResult.stmts);
			println("RIGHT STMTS: " + rightResult.stmts.map(stmt => stmt.code).join(" "));
			// Add assignment
			var $newAssign = ClavaJoinPoints.assign($binaryOp.left, rightResult.$resultExpr);
			stmts.push(ClavaJoinPoints.exprStmt($newAssign));
			
			return new DecomposeResult(stmts, $binaryOp.left);			
		} else {
			// Apply decompose to both sides
			let leftResult = this.decomposeExpr($binaryOp.left);
			//println("LEFT RESULT: " + leftResult);
			let rightResult = this.decomposeExpr($binaryOp.right);
			//println("RIGHT RESULT: " + rightResult);
			
			let stmts = [];
			stmts = stmts.concat(leftResult.stmts);
			stmts = stmts.concat(rightResult.stmts);
			
			// Create operation with result of decomposition
			let $newExpr = ClavaJoinPoints.binaryOp($binaryOp.kind, leftResult.$resultExpr, rightResult.$resultExpr, $binaryOp.type);
			println("NEW EXPR: " + $newExpr.code);
			
			// Create declaration statement with result to new temporary variable
			let tempVarname = this.#newTempVarname();
			let tempVarDecl = ClavaJoinPoints.varDecl(tempVarname, $newExpr);
			stmts.push(tempVarDecl.stmt);

			return new DecomposeResult(stmts, ClavaJoinPoints.varRefFromDecl(tempVarDecl));			
			//	var newTempVar = ClavaJoinPoints.varDecl(this
		
		}

		this.#throwNotImplemented("binary operators", kind);	
	
		//println("StatementDecomposer: not implemented for binary operators of kind '"+kind+"', returning undefined");
		//return undefined;	
	}
	
	
}