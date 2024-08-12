/**************************************************************
* 
*                      checkForInvalidStmts
* 
**************************************************************/
aspectdef checkForInvalidStmts
 	input $ForStmt end
 	output InvalidStmts end
 	
 	// Initialize output
 	this.InvalidStmts = [];

 	// check for [exit] at any subregion
	select $ForStmt.call{'exit'} end
	apply
		this.InvalidStmts.push($call.name + '#' + $call.line);
		return;
	end

	// check for [break,return]
	select $ForStmt.body.stmt end
	apply
		if (
			$stmt.astName === 'ReturnStmt'  ||
			($stmt.astName === 'BreakStmt' && $stmt.getAstAncestor('ForStmt').line === $ForStmt.line)
			)
			{
				this.InvalidStmts.push($stmt.astName + '#' + $stmt.line);
				return;
			}
	end
	condition $stmt.astName === 'ReturnStmt' || $stmt.astName === 'BreakStmt' end
end