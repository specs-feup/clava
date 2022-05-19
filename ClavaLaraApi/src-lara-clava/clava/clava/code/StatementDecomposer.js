/**
 * Decomposes complex statements into several simpler ones.
 */
class StatementDecomposer {

	/**
	 * @param {string} [tempPrefix="decomp_"]
	 * @param {number} [startIndex=0]
	 */
	constructor(tempPrefix = "decomp", startIndex = 0) {	
		this.tempPrefix = tempPrefix ?? "decomp_";
		this.startIndex = startIndex ?? 0;
		
		Check.isString(this.tempPrefix);
		Check.isNumber(this.startIndex);		
	}

	#throwNotImplemented(generalType, specificType) {
		throw new Error(`StatementDecomposer not implemented for ${generalType} of type '${specificType}'`);
	}
	
	#newTempVarname() {
		const varName = `${this.tempPrefix}${this.startIndex}`;
		this.startIndex++;
		return varName;
	}

	/**
	* If the given statement can be decomposed in two or more statements, replaces the statement with the decomposition.
	*
	* @param {$statement} $stmt - A statement that will be decomposed.
	*/
	decomposeAndReplace($stmt) {
		const stmts = this.decompose($stmt);
		
		// No statements to replace
		if(stmts.length === 0) {
			return;
		}
				
		// Insert all statements in order, before original statement
		for(const $newStmt of stmts) {
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
			println(`StatementDecomposer: ${e}`);
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
		
		if($stmt.instanceOf("declStmt")) {
			return this.decomposeDeclStmt($stmt);
		}
	
	
		debug(`StatementDecomposer: not implemented for statement of type ${$stmt.joinPointType}`);
		return [];
	}
	

	decomposeExprStmt($stmt) {
		
		// Statement represents an expression
		const $expr = $stmt.expr;
		
		return this.decomposeExpr($expr).stmts;
	}
	
	decomposeReturnStmt($stmt) {
		
		// Return may contain an expression
		const $expr = $stmt.returnExpr;
		
		if($expr === undefined) {
			return [];
		}
		
		const {stmts, $resultExpr} = this.decomposeExpr($expr);
		const $newReturnStmt = ClavaJoinPoints.returnStmt($resultExpr);
		
		return [...stmts, $newReturnStmt];
	}

	decomposeDeclStmt($stmt) {
		
		// declStmt can have one or more declarations
		const $decls = $stmt.decls;

		return $decls.flatMap(
			($decl) => this.#decomposeDecl($decl)
		);
	}

	#decomposeDecl($decl) {
		if(!$decl.instanceOf("vardecl")) {
			debug(`StatementDecomposer.decomposeDeclStmt: not implemented for decl of type ${$decl.joinPointType}`);
			return [ClavaJoinPoints.declStmt($decl)];
		}

		// If vardecl has init, decompose expression
		if($decl.hasInit) {
			const decomposeResult = this.decomposeExpr($decl.init);				
			expr = newStmts.concat(decomposeResult.stmts);
			$decl.init = decomposeResult.$resultExpr;
			return [...decomposeResult.stmts, ClavaJoinPoints.declStmt($decl)]
		}

		return [ClavaJoinPoints.declStmt($decl)]
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
			return new DecomposeResult([], $expr);
		}

		this.#throwNotImplemented("expressions", $expr.joinPointType);	
	}
	
	decomposeBinaryOp($binaryOp) {
		
		const kind = $binaryOp.kind;
		
		if(kind === "assign") {
			// Get statements of right hand-side
			const rightResult = this.decomposeExpr($binaryOp.right);

			// Add assignment
			const $newAssign = ClavaJoinPoints.assign($binaryOp.left, rightResult.$resultExpr);
			const $assignExpr = ClavaJoinPoints.exprStmt($newAssign);
			
			return new DecomposeResult([...rightResult.stmts, $assignExpr], $binaryOp.left);			
		} 
		// TODO: Not taking into account += and other cases

		// Apply decompose to both sides
		const leftResult = this.decomposeExpr($binaryOp.left);
		const rightResult = this.decomposeExpr($binaryOp.right);
		
		// Create operation with result of decomposition
		const $newExpr = ClavaJoinPoints.binaryOp(
			$binaryOp.kind,
			leftResult.$resultExpr,
			rightResult.$resultExpr,
			$binaryOp.type
		);
		
		// Create declaration statement with result to new temporary variable
		const tempVarname = this.#newTempVarname();
		const tempVarDecl = ClavaJoinPoints.varDecl(tempVarname, $newExpr);

		const stmts = [...leftResult.stmts, ...rightResult.stmts, tempVarDecl.stmt];
		
		return new DecomposeResult(stmts, ClavaJoinPoints.varRefFromDecl(tempVarDecl));					


		//this.#throwNotImplemented("binary operators", kind);	
	}
	
	
}
