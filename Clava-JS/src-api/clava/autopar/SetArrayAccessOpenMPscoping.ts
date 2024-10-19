/**************************************************************
* 
*                       SetArrayAccessOpenMPscoping
* 
**************************************************************/
import clava.autopar.FindReductionArrays;


aspectdef SetArrayAccessOpenMPscoping
	input $ForStmt end

	// Get id of the loop
	var loopindex = GetLoopIndex($ForStmt);
	
	// If there are errors, return
	if (LoopOmpAttributes[loopindex].msgError.length !== 0)
		return;

	
	// TODO: This 'for' needs to be commented in order for array
	// variables to appear as first private. However, commenting it
	// triggers pragma generation bugs (e.g., ] in NAS CG and NAS UA)

	// Ignore array uses in the loop whose pattern is 'R'
	for(var arrayAccessObj of LoopOmpAttributes[loopindex].varAccess)
		if (
			arrayAccessObj.varTypeAccess === 'arrayAccess' &&
			arrayAccessObj.usedInClause === false &&
			arrayAccessObj.use === 'R'
			)
		{
			if (LoopOmpAttributes[loopindex].firstprivateVars.indexOf(arrayAccessObj.name) === -1)
			{
				LoopOmpAttributes[loopindex].firstprivateVars.push(arrayAccessObj.name);				
				//LoopOmpAttributes[loopindex].firstprivateVars.push(arrayAccessObj.name);								
			}
			arrayAccessObj.usedInClause = true;
		}
	
	

	//var arrayAccesslist = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {varTypeAccess : 'arrayAccess', usedInClause : false});
	// Get list of array uses
	var arrayAccesslist = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {varTypeAccess : 'arrayAccess'});


	//print_obj(arrayAccesslist, '\n\n\n\n\n check 1 arrayAccesslist for For#' + $ForStmt.line+ '\taspectdef SetArrayAccessOpenMPscoping');

	// If no array uses, return
	if (arrayAccesslist.length ===0 )
		return;


	var arrayAccessNamelist = [];
	for (var obj of arrayAccesslist)
		if (arrayAccessNamelist.indexOf(obj.name) === -1)
			arrayAccessNamelist.push(obj.name);

	for(var depObj of LoopOmpAttributes[loopindex].PetitFoundDependency)
		if (depObj.canbeignored === false && arrayAccessNamelist.indexOf(depObj.varName) !== -1 && depObj.cannotbesolved === false)
		{
			var arrayAccessObj = SearchStruct(arrayAccesslist, {name : depObj.varName})[0];
			
			if (arrayAccessObj.usedInClause === true)
			{
				arrayAccessNamelist.splice(arrayAccessNamelist.indexOf(depObj.varName),1);
				continue;
			}

			if (
					depObj.IsdependentInnerloop_src === true && 
					depObj.IsdependentCurrentloop_src === false && 
					depObj.IsdependentOuterloop_src === true
				)
			{
				// can be ignored				
				arrayAccessObj.usedInClause = true;
			}

			var check_depVector = true; //should be (0)*1  0,0,1 0,1  1 BUT NOT 1,0

			if (
				depObj.varref_line_src === depObj.varref_line_dst && 
				depObj.src_usge === depObj.dst_usge &&
				check_depVector === true
				)				
			{
				call o : FindReductionArrays($ForStmt, depObj.varName, depObj.varref_line_src, depObj.IsdependentInnerloop_src, depObj.IsdependentCurrentloop_src, depObj.IsdependentOuterloop_src);
				if (o.isReductionArrays === true)
				{
					arrayAccessNamelist.splice(arrayAccessNamelist.indexOf(depObj.varName),1);
					arrayAccessObj.usedInClause = true;
				}
			}

		}

	for(var arrayAccessObj of arrayAccesslist)
		if (arrayAccessObj.usedInClause === false)
		{
			arrayAccessObj.usedInClause = true;
			for(var depObj of LoopOmpAttributes[loopindex].PetitFoundDependency)
				if (depObj.varName === arrayAccessObj.name)
					arrayAccessObj.usedInClause = arrayAccessObj.usedInClause && depObj.canbeignored && (depObj.cannotbesolved === false);

			if (
				arrayAccessObj.usedInClause === true && // TODO: Unnecessary?
				arrayAccessObj.use.indexOf('W') === -1 &&				
				LoopOmpAttributes[loopindex].firstprivateVars.indexOf(arrayAccessObj.name) === -1				
				)
			{
				LoopOmpAttributes[loopindex].firstprivateVars.push(arrayAccessObj.name);
			}

		}

	for(var arrayAccessObj of arrayAccesslist)
		for(var depObj of LoopOmpAttributes[loopindex].PetitFoundDependency)
			if (
					depObj.varName === arrayAccessObj.name && 
					arrayAccessObj.use === 'WR' &&
					depObj.IsdependentCurrentloop_dst === false
				)
				{
					if (LoopOmpAttributes[loopindex].firstprivateVars.indexOf(arrayAccessObj.name) === -1&&
						LoopOmpAttributes[loopindex].Reduction_listVars.indexOf(arrayAccessObj.name) === -1
						)
						{
							LoopOmpAttributes[loopindex].firstprivateVars.push(arrayAccessObj.name);
							arrayAccessObj.usedInClause = true;
						}

				}

	for(var arrayAccessObj of LoopOmpAttributes[loopindex].varAccess)
		if (
			arrayAccessObj.varTypeAccess === 'arrayAccess' &&			
			arrayAccessObj.usedInClause === false
			)
		{
			Add_msgError(LoopOmpAttributes, $ForStmt,'unsolved dependency for arrayAccess ' + arrayAccessObj.name + '\t use : ' + arrayAccessObj.use );
		}

end	