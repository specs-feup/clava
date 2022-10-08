import clava.Clava;
import clava.util.SingleFile;

/**
 * Utility methods related with the source code.
 *
 * @class
 */
var ClavaCode = {};

/**
 * Writes the code corresponding to the current AST as a single file.
 * 
 */
ClavaCode.toSingleFile = function(fileOrBaseFolder, optionalFile) {
	call SingleFile(fileOrBaseFolder, optionalFile);
}

/**
 * Tries to statically detect if a statement is executed only once. 
 *
 * Restrictions:
 * - Does not take into account runtime execution problems, such as exceptions;
 * - Does not detect if the function is called indirectly through a function pointer;
 *
 * @return true if it could be detected, within the restrictions, that the statement is only executed once.
 */
ClavaCode.isExecutedOnce = function ($statement) {

	if(!$statement.instanceOf('statement')) {
		throw "isExecutedOnce(): function expects a statement, received '" + $statement.joinPointType + "'";
	}

	// Go back until it finds the function body
	var $currentScope = $statement.ancestor('scope');

	while($currentScope !== undefined) {
		var $scopeOwner = $currentScope.owner;

		// If finds a scope that is part of a loop or if/else, return false immediately
		if($scopeOwner.instanceOf('loop') || $scopeOwner.instanceOf('if')) {
			debug("ClavaCode.isExecutedOnce: failed because scope is part of loop or if");
			return false;
		}

		// If function, check if main function
		if($scopeOwner.instanceOf('function')) {
			var $function = $scopeOwner;

			// If main, passes check
			if($function.name === 'main') {
				return true;
			}

			// Verify if function is called only once
			var calls = $function.calls;
			if(calls.length !== 1) {
				debug("ClavaCode.isExecutedOnce: failed because function '"+$function.name+"' is called " + calls.length + " times");			
				return false;
			}
	
			var $singleCall = calls[0];
			
			// Recursively call the function on the call statement
			return ClavaCode.isExecutedOnce($singleCall.ancestor("statement"));
		}
		
		$currentScope = $currentScope.ancestor('scope');
	}
	
	// Could not find the scope of the statement. Is it outside of a function?
	debug("ClavaCode.isExecutedOnce: failed because scope could not be found");			
	return false;
}


/**
 * Returns the function definitions in the program with the given name.
 * @param {string} functionName - The name of the function to find 
 * @param {boolean} isSingleFunction -  If true, ensures there is a single definition with the given name
 * @return {$function[]|$function} An array of function definitions, or a single function is 'isSingleFunction' is true
 */
ClavaCode.getFunctionDefinition = function (functionName, isSingleFunction) {
	// Locate function
	var functionSearch = Clava.getProgram()
		.descendants('function')
		//.filter($f => $f.name.equals('main'))[0];
		.filter(function($f) { return $f.name.equals(functionName) && $f.hasDefinition;});


	// If single function false, return search results
	if(!isSingleFunction) {
		return functionSearch;
	}	
		
	if(functionSearch.length === 0) {
		throw "Could not find main function";
	}
	
	if(functionSearch.length > 1) {
		throw "Found more than one definition for function with name '"+functionName+"'";
	}
	
	return functionSearch[0];
}
