/**************************************************************
* 
*                   additionalConditionsCheck
* 
**************************************************************/
import clava.autopar.checkForFunctionCalls;

aspectdef additionalConditionsCheck	
	input $ForStmt end

	var loopindex = GetLoopIndex($ForStmt);
	if (LoopOmpAttributes[loopindex].msgError.length !== 0)
		return;


	var conterNum = null;

	try
	{
		conterNum = eval($ForStmt.endValue + '-' +  $ForStmt.initValue);
	}
	catch(e)
	{
	}


	if (
		Number($ForStmt.endValue) !== NaN &&
		conterNum !== null &&
		Math.abs(Number(conterNum)) < 50
		)
	{
		Add_msgError(LoopOmpAttributes, $ForStmt, ' Loop Iteration number is too low');
		return;
	}		

	call o : checkForFunctionCalls($ForStmt);

	if (o.FunctionCalls.length > 0)
	{
		Add_msgError(LoopOmpAttributes, $ForStmt, 'Variables Access as passed arguments Can not be traced inside of function calls : \n' + o.FunctionCalls.join('\n'));
		return;
	}

	// check non-affine array subscript A[B[j]].... ????
	//------------------------------------------------------------
	// check for usage of array access as the subscript A[B[.]]
	//------------------------------------------------------------
	select $ForStmt.arrayAccess.subscript end
	apply
		if ($subscript.getDescendantsAndSelf('arrayAccess').length > 0)
		{
			Add_msgError(LoopOmpAttributes, $ForStmt,' Array access ' + $arrayAccess.code + ' which is used for writing has subscript of arrayType ' + $subscript.code);
			return;
		}
	end
	condition $arrayAccess.use.indexOf('write') !== -1 end

end