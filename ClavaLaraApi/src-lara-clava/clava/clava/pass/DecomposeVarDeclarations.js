laraImport("lara.pass.Pass");
laraImport("clava.ClavaJoinPoints");

/**
 * 
 */
class DecomposeVarDeclarations extends Pass {
	
	constructor() {
		super("DecomposeVarDeclarations");
	}
	
	_apply_impl($jp) {
	
		// Find all var decls
		for(var $vardecl of Query.searchFromInclusive($jp, "vardecl")) {
			
			// Ignore vardecl if no initialization
			if(!$vardecl.hasInit) {
				continue;
			}
			
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
		
	
	}
}