laraImport("lara.pass.Pass");
laraImport("clava.ClavaJoinPoints");

/**
 * Decomposes the vardecl nodes that are reachable from the given join point.
 *
 * E.g. transforms int i = 0; into int i; i = 0;
 */
class DecomposeVarDeclarations extends Pass {
	
	constructor() {
		super("DecomposeVarDeclarations");
	}
	
	_apply_impl($jp) {
		
		let appliedPass = false;
		
		// Find all var decls
		for(var $vardecl of Query.searchFromInclusive($jp, "vardecl")) {
			
			// Ignore vardecl if no initialization
			if(!$vardecl.hasInit) {
				continue;
			}
			
			// Found vardecl to decompose
			appliedPass = true;
			
			// Get initialization
			let $init = $vardecl.init;

			// Remove initialization from vardecl
			$vardecl.removeInit();
			
			// If vardecl is of type auto, replace with initialization type
			if($vardecl.type.isAuto) {
				$vardecl.type = $init.type;
			}

			
			// Create varref and assignment
			let $varref = ClavaJoinPoints.varRef($vardecl);
			let $assign = ClavaJoinPoints.assign($varref, $init);

			// Insert assignment			
			$vardecl.insertAfter($assign);
		}
		
		return this._new_result($jp, appliedPass);
		
	}
	
	_new_result($jp, appliedPass) {
		var result = new PassResult(this.name);
		result.isUndefined = false;
		result.appliedPass = appliedPass;
		result.insertedLiteralCode = false;
		result.location = $jp.location;
		return result;
	}
}