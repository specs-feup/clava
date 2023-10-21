/**************************************************************
* 
*                      checkForFunctionCalls
* 
**************************************************************/
aspectdef checkForFunctionCalls
 	input $ForStmt end
 	output FunctionCalls end
 	
 	this.FunctionCalls = [];
 		
	// check for calling functions

	var userSafeFunctions = $ForStmt.data.safeFunctions;
	
	select $ForStmt.call end
	apply				
		var isSafe = (safefunctionCallslist.indexOf($call.name) !== -1) ||
					  (userSafeFunctions !== undefined && userSafeFunctions.indexOf($call.name) !== -1);

		if (!isSafe)	
			this.FunctionCalls.push($call.name + '#' + $call.line + '{' + $call.code +'}');
	end
	condition $call.astName === 'CallExpr' end
	
end
