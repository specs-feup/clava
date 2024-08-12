/**************************************************************
* 
*                       checkForSafeFunctionCall
* 
**************************************************************/
aspectdef CheckForSafeFunctionCall

	var new_safefunctionCallslist = [];
	
	select file.function end
	apply
		if ( new_safefunctionCallslist.indexOf($function.name) === -1)
			new_safefunctionCallslist.push($function.name);
	end
	condition $function.params.length > 0 end 

	select file.function.call end
	apply
		new_safefunctionCallslist.splice(new_safefunctionCallslist.indexOf($function.name),1);
	end
	condition new_safefunctionCallslist.indexOf($function.name) !== -1  end


	select file.function.body.arrayAccess end
	apply
		if($arrayAccess.use.indexOf('write') === -1) {
			continue;
		}
		
		var currentRegion = $arrayAccess.arrayVar.getDescendantsAndSelf('varref')[0].vardecl.currentRegion;
		if((currentRegion !== undefined && currentRegion.joinPointType === 'file')
			|| $arrayAccess.arrayVar.getDescendantsAndSelf('varref')[0].vardecl.isParam) {
			
			new_safefunctionCallslist.splice(new_safefunctionCallslist.indexOf($function.name),1);
		}
		
/*
		if (
			$arrayAccess.use.indexOf('write') !== -1 && 
				(
					$arrayAccess.arrayVar.getDescendantsAndSelf('varref')[0].vardecl.currentRegion.joinPointType === 'file'
					||
					$arrayAccess.arrayVar.getDescendantsAndSelf('varref')[0].vardecl.isParam
				)
			)
			new_safefunctionCallslist.splice(new_safefunctionCallslist.indexOf($function.name),1);
*/			
	end
	condition new_safefunctionCallslist.indexOf($function.name) !== -1 end

	select file.function.body.memberAccess end
	apply
		if($memberAccess.use.indexOf('write') === -1) {
			continue;
		}
		
		var currentRegion = $memberAccess.getDescendantsAndSelf('varref')[0].vardecl.currentRegion;
		if(currentRegion !== undefined && currentRegion.joinPointType === 'file') {
			new_safefunctionCallslist.splice(new_safefunctionCallslist.indexOf($function.name),1);
		}
		
	/*
		if (
			$memberAccess.use.indexOf('write') !== -1 &&
			$memberAccess.getDescendantsAndSelf('varref')[0].vardecl.currentRegion.joinPointType === 'file'
			)
			new_safefunctionCallslist.splice(new_safefunctionCallslist.indexOf($function.name),1);		
	*/
	end
	condition new_safefunctionCallslist.indexOf($function.name) !== -1 end


	select file.function.body.varref end
	apply
		if($varref.useExpr.use.indexOf('write') === -1){
			continue;
		}
	
		var currentRegion = $varref.vardecl.currentRegion;
		if(currentRegion !== undefined && currentRegion.joinPointType === 'file') {
			new_safefunctionCallslist.splice(new_safefunctionCallslist.indexOf($function.name),1);
		}
		/*
		if (
			$varref.useExpr.use.indexOf('write') !== -1 &&
			$varref.vardecl.currentRegion.joinPointType === 'file'
			)
			new_safefunctionCallslist.splice(new_safefunctionCallslist.indexOf($function.name),1);		
		*/
	end
	condition new_safefunctionCallslist.indexOf($function.name) !== -1 end

	select file end
	apply
		$file.insertBegin('//new_safefunctionCallslist : ' + new_safefunctionCallslist.join(' , '));
	end

	if (new_safefunctionCallslist.length > 0)
		safefunctionCallslist = safefunctionCallslist.concat(new_safefunctionCallslist);

end