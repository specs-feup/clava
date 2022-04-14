laraImport("clava.graphs.CfgNodeType");
laraImport("lara.graphs.GraphNode");
laraImport("clava.ClavaJoinPoints");

/**
 * The data of a CFG node.
 */
class CfgNode extends GraphNode {
	
	stmts;
	type;
	
	constructor($stmt) {
		//super($stmt.location);		
		super();
		
		this.stmts = [];
		this.stmts.push($stmt);
		this.type = CfgNodeType.UNDEFINED;
	}

	addStmt($stmt) {
		this.stmts.push($stmt);
	}

	toString() {
		let code = "";
		
		// TODO: Each type of statement needs its "toString", for instance, a for only prints its header
		for(let $stmt of this.stmts) {
			if($stmt.instanceOf("loop")) {
				$stmt = $stmt.copy();
				const $emptyScope = ClavaJoinPoints.scope();
				$emptyScope.naked = true;
				$stmt.body = $emptyScope;
			}
			
			code += $stmt.code.replaceAll("\n", "\\l").replaceAll("\r", "");
			code += "\\l";
		}
		
		return code;
	}
}