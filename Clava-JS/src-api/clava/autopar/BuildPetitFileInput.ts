/**************************************************************
* 
*                       BuildPetitFileInput
* 
**************************************************************/
import clava.autopar.get_varTypeAccess;

aspectdef BuildPetitFileInput
	input $ForStmt end

	var replace_vars = [];
	var loopindex = GetLoopIndex($ForStmt);

	if (LoopOmpAttributes[loopindex].msgError.length !== 0)
		return;

	LoopOmpAttributes[loopindex].ForStmtToPetit = [];
	LoopOmpAttributes[loopindex].petit_variables = [];
	LoopOmpAttributes[loopindex].petit_arrays = {};
	LoopOmpAttributes[loopindex].petit_loop_indices = [];

	LoopOmpAttributes[loopindex].petit_variables.push('petit_tmp');

	var varreflist = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {varTypeAccess : 'varref'});
	for(var i = 0; i < varreflist.length; i++)
	{	
		LoopOmpAttributes[loopindex].petit_variables.push(varreflist[i].name);
		if (varreflist[i].name[0] === '_')
			replace_vars.push(varreflist[i].name);
	}
	var loopsControlVarname = [];
	loopsControlVarname.push(LoopOmpAttributes[loopindex].loopControlVarname);
	if (LoopOmpAttributes[loopindex].innerloopsControlVarname !== undefined )
		loopsControlVarname = loopsControlVarname.concat(LoopOmpAttributes[loopindex].innerloopsControlVarname);
	for(var i = 0; i < loopsControlVarname.length; i++)
		LoopOmpAttributes[loopindex].petit_loop_indices.push(loopsControlVarname[i]);



	var $cloneJPForStmt = $ForStmt.copy();

	var tabOP = [];

	call o : CovertLoopToPetitForm($ForStmt, tabOP);
	LoopOmpAttributes[loopindex].ForStmtToPetit.push({line: LoopOmpAttributes[loopindex].start, str : o.loopPetitForm});
	LoopOmpAttributes[loopindex].ForStmtToPetit.push({line: LoopOmpAttributes[loopindex].end, str : tabOP.join('')  + 'endfor'});
	
	select $ForStmt.body.loop end
	apply
		var innerloopindex = GetLoopIndex($loop);

		tabOP = [];
		for(var i = 0; i < $loop.rank.length-1; i++)
			tabOP.push('\t');

		call o : CovertLoopToPetitForm($loop, tabOP);
		LoopOmpAttributes[loopindex].ForStmtToPetit.push({line: LoopOmpAttributes[innerloopindex].start, str : o.loopPetitForm});
		LoopOmpAttributes[loopindex].ForStmtToPetit.push({line: LoopOmpAttributes[innerloopindex].end, str : tabOP.join('') + 'endfor'});
	end
	condition $loop.astName === 'ForStmt' end

	var candidateArraylist = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {usedInClause : false, hasDescendantOfArrayAccess : true});

	var oder = 0;
	for(var i = 0; i < candidateArraylist.length; i++)
	{
		var varObj = candidateArraylist[i];
		
		if ( varObj.use.indexOf('W') === -1 || varObj.sendtoPetit === false)			
			continue;
		
		for(var j = 0; j < varObj.varUsage.length; j++)
			if (varObj.varUsage[j].isInsideLoopHeader === false)
			{

				var tabOP = Array(varObj.varUsage[j].parentlooprank.length).join('\t');
				if (varObj.varUsage[j].use === 'R')
				{
					LoopOmpAttributes[loopindex].ForStmtToPetit.push({
						line : varObj.varUsage[j].line,
						order : oder++,
						parentlooprank : varObj.varUsage[j].parentlooprank.join('_'),
						IsdependentCurrentloop : varObj.varUsage[j].IsdependentCurrentloop,
						IsdependentInnerloop : varObj.varUsage[j].IsdependentInnerloop,
						IsdependentOuterloop : varObj.varUsage[j].IsdependentOuterloop,
						str : tabOP + 'petit_tmp = ' + varObj.varUsage[j].code
							});
				}
				else if (varObj.varUsage[j].use === 'W')
				{
					LoopOmpAttributes[loopindex].ForStmtToPetit.push({
						line : varObj.varUsage[j].line,
						order : oder++,
						parentlooprank : varObj.varUsage[j].parentlooprank.join('_'),
						IsdependentCurrentloop : varObj.varUsage[j].IsdependentCurrentloop,
						IsdependentInnerloop : varObj.varUsage[j].IsdependentInnerloop,	
						IsdependentOuterloop : varObj.varUsage[j].IsdependentOuterloop,					
						str : tabOP + varObj.varUsage[j].code + ' = petit_tmp'
							});
				}
				else if (varObj.varUsage[j].use === 'RW')
				{
					LoopOmpAttributes[loopindex].ForStmtToPetit.push({
						line : varObj.varUsage[j].line,
						order : oder++,
						parentlooprank : varObj.varUsage[j].parentlooprank.join('_'),
						IsdependentCurrentloop : varObj.varUsage[j].IsdependentCurrentloop,
						IsdependentInnerloop : varObj.varUsage[j].IsdependentInnerloop,
						IsdependentOuterloop : varObj.varUsage[j].IsdependentOuterloop,						
						str : tabOP + 'petit_tmp = ' + varObj.varUsage[j].code
							});					
					LoopOmpAttributes[loopindex].ForStmtToPetit.push({
						line : varObj.varUsage[j].line,
						order : oder++,
						parentlooprank : varObj.varUsage[j].parentlooprank.join('_'),
						IsdependentCurrentloop : varObj.varUsage[j].IsdependentCurrentloop,
						IsdependentInnerloop : varObj.varUsage[j].IsdependentInnerloop,
						IsdependentOuterloop : varObj.varUsage[j].IsdependentOuterloop,					
						str : tabOP + varObj.varUsage[j].code + ' = petit_tmp'
							});					
				}			
			}
			
	}

	
	for(var i=0; i < LoopOmpAttributes[loopindex].ForStmtToPetit.length ; i++)
	{
		LoopOmpAttributes[loopindex].ForStmtToPetit[i].str = LoopOmpAttributes[loopindex].ForStmtToPetit[i].str.moveBracketsToEnd3(LoopOmpAttributes[loopindex].petit_arrays);
	}

	var j = -6;
	for ( var key in LoopOmpAttributes[loopindex].petit_arrays)
	{
		LoopOmpAttributes[loopindex].ForStmtToPetit.push({line : j, str : 'integer ' + 
				LoopOmpAttributes[loopindex].petit_arrays[key].name + 
				LoopOmpAttributes[loopindex].petit_arrays[key].size });
		j--;
		LoopOmpAttributes[loopindex].ForStmtToPetit.push({line : j, str : '!------ ' + 
				LoopOmpAttributes[loopindex].petit_arrays[key].name + 
				' -> ' +key});
		j--;
	}
	LoopOmpAttributes[loopindex].ForStmtToPetit.push({line : j, str : '!' + Array(50).join('-') +' arrays'});

	LoopOmpAttributes[loopindex].ForStmtToPetit.push({line : -5, str : '!' + Array(50).join('-') +' loop indices'});
	LoopOmpAttributes[loopindex].ForStmtToPetit.push({line : -4, str : 'integer ' + LoopOmpAttributes[loopindex].petit_loop_indices.join(',')});

	LoopOmpAttributes[loopindex].ForStmtToPetit.push({line : -3, str : '!' + Array(50).join('-') +' variables'});
	LoopOmpAttributes[loopindex].ForStmtToPetit.push({line : -2, str : 'integer ' + LoopOmpAttributes[loopindex].petit_variables.join(',')});
	
	LoopOmpAttributes[loopindex].ForStmtToPetit.push({line : -1, str : '!' + Array(50).join('-') +' body code'});

	LoopOmpAttributes[loopindex].ForStmtToPetit = LoopOmpAttributes[loopindex].ForStmtToPetit.sort(
				function(obj1, obj2)
				{
					if(obj1.line !== obj2.line)
						return obj1.line - obj2.line;
					else
						return obj1.order - obj2.order;
				});


	var count = 1;
	var replaceloopindices = {};

	for(var loopindices of LoopOmpAttributes[loopindex].petit_loop_indices)
		if (loopindices.length > 5)
		{
			replaceloopindices[loopindices] = {};
			replaceloopindices[loopindices].rep = 'tmp' + count.toString();;
			count = count + 1;
		}


	for(var i=0; i < LoopOmpAttributes[loopindex].ForStmtToPetit.length ; i++)
	{
		for (var key in replaceloopindices)
			if (LoopOmpAttributes[loopindex].ForStmtToPetit[i].str.indexOf(key) !== -1)				
			{
				LoopOmpAttributes[loopindex].ForStmtToPetit[i].str = Strings.replacer(LoopOmpAttributes[loopindex].ForStmtToPetit[i].str,key,replaceloopindices[key].rep);
			}
		
	}

	for(var replace_var of replace_vars)
		for(var i=0; i < LoopOmpAttributes[loopindex].ForStmtToPetit.length ; i++)
			LoopOmpAttributes[loopindex].ForStmtToPetit[i].str = Strings.replacer(LoopOmpAttributes[loopindex].ForStmtToPetit[i].str,replace_var,replace_var.substr(1));

end


/**************************************************************
* 
*                       CovertLoopToPetitForm
* 
**************************************************************/
aspectdef CovertLoopToPetitForm
	input $ForStmt, tabOP end
	output loopPetitForm end

	this.loopPetitForm = tabOP.join('') + 'for ';
	var loopindex = GetLoopIndex($ForStmt);
	//console.log("DEBUG - Loop Index: " + loopindex); 
	//console.log("DEBUG - For Stmt: " + $ForStmt.location); 	
	//console.log("DEBUG - LoopOmpAttributes[loopindex]: " + LoopOmpAttributes[loopindex]);
	var loopAttributes = LoopOmpAttributes[loopindex];
	if(loopAttributes === undefined) {
		var message = "";
		message += "Could not find the loop attributes of loop " + loopindex + "@" + $ForStmt.location + ". Current attributes:\n";
		for(var key in LoopOmpAttributes) {
			message += key + ": " + LoopOmpAttributes[key];
		}	
		
		throw message;
	}
	
	//var loopControlVarname = LoopOmpAttributes[loopindex].loopControlVarname;
	var loopControlVarname = loopAttributes.loopControlVarname;

	var cloneJP = null;

	select $ForStmt.init end
	apply
		for($cast of $init.getDescendantsAndSelf("vardecl")) // if for(int i = ... )
		{
			cloneJP=$cast.init.copy();
			break #$ForStmt;
		}
		for($cast of $init.getDescendantsAndSelf("binaryOp"))// if for(i = ... )
		{
			cloneJP=$cast.right.copy();
			break #$ForStmt;
		}
	end


	for($cast of cloneJP.getDescendantsAndSelf("cast"))
	{
		var child = $cast.getChild(0);
		$cast.replaceWith(child);
	}

	for($cast of cloneJP.getDescendantsAndSelf("unaryOp"))
	{
		var child = $cast.getChild(0);
		$cast.replaceWith(child);
	}


	var str_init = 	cloneJP.code;

	this.loopPetitForm +=  loopControlVarname + '  =  ' + str_init + '  to  ';

	cloneJP = null;
	var binaryOpleft = null;
	var binaryOpRight = null;
	select $ForStmt.cond.binaryOp end
	apply
		binaryOpleft=$binaryOp.left.copy();
		binaryOpRight=$binaryOp.right.copy();
		break #$ForStmt;
	end

	var foundflag = false;
	for($cast of binaryOpleft.getDescendantsAndSelf("varref"))
		if ($cast.name === loopControlVarname)
		{
			cloneJP=binaryOpRight;
			foundflag = true;
		}

	if (foundflag === false)
		cloneJP=binaryOpleft;



	for($cast of cloneJP.getDescendantsAndSelf("cast"))
	{
		var child = $cast.getChild(0);
		$cast.replaceWith(child);
	}
	for($cast of cloneJP.getDescendantsAndSelf("unaryOp"))
	{
		var child = $cast.getChild(0);
		$cast.replaceWith(child);
	}
	
	var str_cond = 	cloneJP.code;

	for($cast of cloneJP.getDescendantsAndSelf("binaryOp"))
	{
		if (['shr' , 'shl'].indexOf($cast.kind) !==  -1)
			str_cond = '9999';
	}

	this.loopPetitForm +=  str_cond;

 	var stepOp = null;
 	cloneJP = null;
 	select $ForStmt.step.expr end
 	apply
 		stepOp = $expr.kind;
 		
 		if (stepOp === 'assign' || stepOp === 'add' || stepOp === 'sub')
 		{
 			cloneJP=$expr.right.copy();
 		}
 		break #$ForStmt;
 	end
 	condition $expr.joinPointType == 'binaryOp' || $expr.joinPointType == 'unaryOp' end
 	

 	if (stepOp === 'post_inc' || stepOp === 'pre_inc')
 	{
 		this.loopPetitForm += '  do';
 	}
 	else if (stepOp === 'pre_dec' || stepOp === 'post_dec')
 	{
 		this.loopPetitForm += '  by  -1  do';
 	}
 	else if (stepOp === 'assign')
 	{
 		this.loopPetitForm += '  do';
 	}


end	