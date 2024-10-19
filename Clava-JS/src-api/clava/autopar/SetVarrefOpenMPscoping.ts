/**************************************************************
* 
*                       SetVarrefOpenMPscoping
* 
**************************************************************/
import clava.autopar.checkvarreReduction;

aspectdef SetVarrefOpenMPscoping
	input $ForStmt end

	var loopindex = GetLoopIndex($ForStmt);
	if (LoopOmpAttributes[loopindex].msgError.length !== 0)
		return;

	var varreflist = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {varTypeAccess : 'varref'});


	for(var i = 0; i < varreflist.length; i++)
		if (varreflist[i].declpos === 'inside')
			varreflist[i].usedInClause = true;



			
	//------------------------------------------------------------
	//	add all sub loop control vars to private list
	//------------------------------------------------------------
	var loopsControlVarname = [];
	loopsControlVarname.push(LoopOmpAttributes[loopindex].loopControlVarname);
	if (LoopOmpAttributes[loopindex].innerloopsControlVarname !== undefined )
		loopsControlVarname = loopsControlVarname.concat(LoopOmpAttributes[loopindex].innerloopsControlVarname);
	
	for(var i = 0; i < loopsControlVarname.length; i++)
		if (LoopOmpAttributes[loopindex].privateVars.indexOf(loopsControlVarname[i]) === -1)
			LoopOmpAttributes[loopindex].privateVars.push(loopsControlVarname[i]);
	

	//------------------------------------------------------------
	// add all vars which [use==WR or use==W] and [nextUse==null] and are local function var
	//------------------------------------------------------------
	for(var i = 0; i < varreflist.length; i++)
	{
		var varObj = varreflist[i];
		if (
				varObj.usedInClause === false &&
				varObj.nextUse !== 'R' &&
				(varObj.use === 'WR' || varObj.use === 'W')
			)
		{
			if ( LoopOmpAttributes[loopindex].privateVars.indexOf(varObj.name) === -1 )
				LoopOmpAttributes[loopindex].privateVars.push(varObj.name);
			varObj.usedInClause = true;
		}
	}

	//------------------------------------------------------------
	// find Reduction for variable access
	//------------------------------------------------------------
	call o : checkvarreReduction($ForStmt);
	LoopOmpAttributes[loopindex].reduction = LoopOmpAttributes[loopindex].reduction.concat(o.Reduction);


	//------------------------------------------------------------
	// 						Fill firstprivate and lastprivate
	//	Restrictions : 
	//		- A variable that is part of another variable (as an array or structure element) cannot appear in a private clause
	//		- If a list item appears in both firstprivate and lastprivate clauses, the update required for lastprivate occurs after all the initializations for firstprivate
	//		- A list item that appears in a reduction clause of a parallel construct must not appear in a firstprivate clause
	//		- A variable that appears in a firstprivate clause must not have an incomplete C/C++ type or be a reference to an incomplete type
	//------------------------------------------------------------
	for(var i = 0; i < varreflist.length; i++)
		if (
			varreflist[i].usedInClause === false &&
			varreflist[i].declpos !== 'inside'
			)
		{
			if (varreflist[i].use === 'R' &&
				LoopOmpAttributes[loopindex].firstprivateVars.indexOf(varreflist[i].name) === -1)
				{
					LoopOmpAttributes[loopindex].firstprivateVars.push(varreflist[i].name);
					varreflist[i].usedInClause = true;
				}

			if (varreflist[i].nextUse === 'R' && 
				varreflist[i].use.indexOf('W') !== -1 &&
				varreflist[i].use.indexOf('RW') === -1) // hamid changed for EP benchmark
				{
					LoopOmpAttributes[loopindex].lastprivateVars.push(varreflist[i].name);
					varreflist[i].usedInClause = true;
				}
		}

	//------------------------------------------------------------
	// simple test if all variable used in Clauses are set as usedInClause
	//------------------------------------------------------------
	for(var i = 0; i < varreflist.length; i++)
		if (
			varreflist[i].usedInClause === false &&
				(
				LoopOmpAttributes[loopindex].firstprivateVars.indexOf(varreflist[i].name) !== -1 ||
				LoopOmpAttributes[loopindex].lastprivateVars.indexOf(varreflist[i].name) !== -1 ||
				LoopOmpAttributes[loopindex].privateVars.indexOf(varreflist[i].name) !== -1
				)
			)
			{
				varreflist[i].usedInClause = true;
			}


	for(var i = 0; i < varreflist.length; i++)
		if (varreflist[i].usedInClause === false)
		{
			Add_msgError(LoopOmpAttributes, $ForStmt,'Variable ' + varreflist[i].name + ' could not be categorized into any OpenMP Variable Scope' + 'use : ' + varreflist[i].use);


		}


	select $ForStmt.vardecl end
	apply
		if (LoopOmpAttributes[loopindex].privateVars.indexOf($vardecl.name) !== -1)
			LoopOmpAttributes[loopindex].privateVars.splice(LoopOmpAttributes[loopindex].privateVars.indexOf($vardecl.name), 1);

		if (LoopOmpAttributes[loopindex].firstprivateVars.indexOf($vardecl.name) !== -1)
			LoopOmpAttributes[loopindex].firstprivateVars.splice(LoopOmpAttributes[loopindex].firstprivateVars.indexOf($vardecl.name), 1);

		if (LoopOmpAttributes[loopindex].lastprivateVars.indexOf($vardecl.name) !== -1)
			LoopOmpAttributes[loopindex].lastprivateVars.splice(LoopOmpAttributes[loopindex].lastprivateVars.indexOf($vardecl.name), 1);	
	end
	
	select $ForStmt.varref end
	apply
		if (LoopOmpAttributes[loopindex].privateVars.indexOf($varref.name) !== -1)
			LoopOmpAttributes[loopindex].privateVars.splice(LoopOmpAttributes[loopindex].privateVars.indexOf($varref.name), 1);

		if (LoopOmpAttributes[loopindex].firstprivateVars.indexOf($varref.name) !== -1)
			LoopOmpAttributes[loopindex].firstprivateVars.splice(LoopOmpAttributes[loopindex].firstprivateVars.indexOf($varref.name), 1);

		if (LoopOmpAttributes[loopindex].lastprivateVars.indexOf($varref.name) !== -1)
			LoopOmpAttributes[loopindex].lastprivateVars.splice(LoopOmpAttributes[loopindex].lastprivateVars.indexOf($varref.name), 1);	
			
	end
	condition $varref.type.isPointer === true end

end