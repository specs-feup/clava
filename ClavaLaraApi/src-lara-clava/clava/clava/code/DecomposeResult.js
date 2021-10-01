class DecomposeResult {

	constructor(stmts, $resultExpr) {
		//println("ADASDASD");
		//this.stmts = stmts !== undefined ? stmts : [];
		this.stmts = stmts;		
		this.$resultExpr = $resultExpr;		
	}

/*
	get stmts() {
		return this.stmts;
	}
	
	get $resultExpr() {
		return this.$resultExpr;
	}	
*/
	toString() {
		//println("STMTS: " + this.stmts);
		//println("EXPR: " + this.$resultExpr);		
		//return this.stmts + " | " + this.$resultExpr;
		return this.stmts.map(stmt => stmt.code).join(" ") + " -> " + this.$resultExpr.code;
	}
}