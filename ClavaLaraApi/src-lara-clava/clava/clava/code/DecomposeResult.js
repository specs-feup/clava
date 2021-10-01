class DecomposeResult {

	constructor(stmts, $resultExpr) {
		this.stmts = stmts;		
		this.$resultExpr = $resultExpr;		
	}

	toString() {
		return this.stmts.map(stmt => stmt.code).join(" ") + " -> " + this.$resultExpr.code;
	}
}