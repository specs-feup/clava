/**************************************************************
* 
*                       get_varTypeAccess
* 
**************************************************************/
aspectdef get_varTypeAccess
	input $varJP, op end
	output varTypeAccess, varUse, varName, vardecl end


	op = (typeof op !== 'undefined') ? op : 'all';

	this.varTypeAccess = null;
	this.varUse = null;
	this.varName = null;
	this.vardecl = null;

	if (
			$varJP.joinPointType === 'arrayAccess' && 
			['BuiltinType','QualType','TypedefType'].indexOf($varJP.type.astName) !== -1
			//$varJP.type.astName !== 'RecordType' && 
			//$varJP.type.astName === 'BuiltinType'
		)
		{
			if ($varJP.arrayVar.joinPointType === 'memberAccess')
			{
				this.varTypeAccess = 'memberArrayAccess';
			}
			else if ($varJP.arrayVar.joinPointType === 'varref')
			{
				this.varTypeAccess = 'arrayAccess';
			}
		}

	if (
			$varJP.joinPointType === 'memberAccess' && 
			['BuiltinType','QualType'].indexOf($varJP.type.astName) !== -1
			//$varJP.type.astName === 'BuiltinType'
		)
		{
			this.varTypeAccess = 'memberAccess';
		}

	if (
			$varJP.joinPointType === 'varref' && 
			['BuiltinType','QualType','PointerType','TypedefType'].indexOf($varJP.type.astName) !== -1
			//$varJP.type.astName === 'BuiltinType'
		)
		{
			this.varTypeAccess = 'varref';
		}


	if (op !== 'all')
		return;

	if (this.varTypeAccess === 'memberArrayAccess')
	{
		this.varUse = $varJP.use;
		//varName = $varJP.arrayVar.name;
		this.varName = normalizeVarName($varJP.code);
	}
	else if (this.varTypeAccess === 'arrayAccess')
	{
		this.vardecl = $varJP.arrayVar.vardecl;
		if(this.vardecl === undefined) {
			debug("autopar.get_varTypeAccess: Could not find vardecl of arrayVar@" + $varJP.arrayVar.location);
			this.vardecl = null;
		}
		this.varUse = $varJP.use;
		//varName = $varJP.arrayVar.name;
		this.varName = normalizeVarName($varJP.code);
	}
	else if (this.varTypeAccess === 'memberAccess')
	{
		this.varUse = $varJP.use;
		//varName = $varJP.name;
		this.varName = normalizeVarName($varJP.code);
	}
	else if (this.varTypeAccess === 'varref')
	{
		this.varUse = $varJP.useExpr.use;
		this.vardecl = $varJP.vardecl;
		if(this.vardecl === undefined) {
			debug("autopar.get_varTypeAccess: Could not find vardecl of var@" + $varJP.location);
			this.vardecl = null;
		}
		
		//varName = $varJP.name;
		this.varName = normalizeVarName($varJP.code);
	}

end

