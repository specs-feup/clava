laraImport("clava.graphs.CfgNodeType");
laraImport("lara.graphs.GraphNode");

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

	toString() {
		let code = "";
		
		// TODO: Each type of statement needs its "toString", for instance, a for only prints its header
		for(let $stmt of this.stmts) {
			//if($stmt.instanceOf("loop")) {
			//	$stmt = $stmt.copy();
			//	$stmt.body.detach();
			//}
			
			code += $stmt.code.replaceAll("\n", "\\l").replaceAll("\r", "");
		}
		
		return code;
	}
}