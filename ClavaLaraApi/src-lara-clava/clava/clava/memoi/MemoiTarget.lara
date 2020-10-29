import clava.memoi.MemoiUtils;

import weaver.Query;

function MemoiTarget(sig, $func, isUser, numInputs, numOuputs, inputTypes, outputTypes, numCallSites) {
	
	this._sig = MemoiUtils.normalizeSig(sig);
	this._$func = $func;
	this._isUser = isUser;
	this._numInputs = numInputs === undefined ? $func.params.length : numInputs;
	this._numOutputs = numOuputs === undefined? 1 : numOuputs;
	
	if(inputTypes === undefined || outputTypes === undefined) {
		
		this._findDataTypes();
	} else {
		this._inputTypes = inputTypes;
		this._outputTypes = outputTypes;
	}
	
	if(numCallSites === undefined) {
		this._findNumCallSites();
	} else {
		this._numCallSites = numCallSites;
	}
	
	this._checkDataTypes();
}

MemoiTarget.fromFunction = function($func) {
	
	var sig = MemoiUtils.normalizeSig($func.signature);
	var isUser = !MemoiUtils.isWhiteListed(sig);
	var numInputs = $func.params.length;
	var numOutputs = 1;
	
	// input types, output types, and num of call sites are found in the constructor
	return new MemoiTarget(sig, $func, isUser, numInputs, numOutputs, undefined, undefined);
}

MemoiTarget.fromCall = function($call) {
	
	var $func = $call.function;
	if($func === undefined) {
		throw "Could not find function of call '" + $call.code + "'";
	}
	
	return MemoiTarget.fromFunction($func);
}

MemoiTarget.fromSig = function(sig) {
	
	sig = MemoiUtils.normalizeSig(sig);
	
	var $func = Query.search("function",{"signature": signature => sig === MemoiUtils.normalizeSig(signature)}).first();
	if($func === undefined) {
		
		var $call = Query.search("call",{"signature": signature => sig === MemoiUtils.normalizeSig(signature)}).first();
		if($call === undefined) {
		
			throw "Could not find function of sig '" + sig + "'";	
		}
		
		return MemoiTarget.fromCall($call);
	}

	return MemoiTarget.fromFunction($func);
}

MemoiTarget.prototype._findNumCallSites = function() {
	
	var numCallSites = 0;
	
	for(var $call of Query.search("call", {"signature": signature => this._sig === MemoiUtils.normalizeSig(signature)})) {
		numCallSites++;
	}
	
	this._numCallSites = numCallSites;
}

MemoiTarget.prototype._findDataTypes = function() {
	
	inputTypes = [];
	outputTypes = [];
	
	var $functionType = this._$func.functionType;
	
	if(this._numOutputs == 1) {
		
		outputTypes.push($functionType.returnType.code);
		$functionType.paramTypes.forEach(function(e){inputTypes.push(e.code);});

	} else {

		var typeCodes = $functionType.paramTypes.map(function(e){return e.code;});
		
		typeCodes.forEach(function(e, i){
			if(i < this._numInputs) {
				inputTypes.push(e);
			} else {
				outputTypes.push(e);
			}
		});
	}
	
	this._inputTypes = inputTypes;
	this._outputTypes = outputTypes;
}

MemoiTarget.prototype._checkDataTypes = function() {
	
	var normalTypes = ["double", "float", "int"];
	var pointerTypes = ["double *", "float *", "int *"];
	var outputTestArray;
	
	var inputsInvalid = this._inputTypes.some(function(e){return !MemoiUtils.arrayContains(normalTypes, e);});

	if(inputsInvalid) {
		throw "The inputs of the target function '" + this._sig + "' are not supported.";
	}

	outputTestArray = this._numOutputs == 1 ? normalTypes : pointerTypes;
	var outputsInvalid = this._outputTypes.some(function(e){return !MemoiUtils.arrayContains(outputTestArray, e);});
	
	if(outputsInvalid) {
		throw "The outputs of the target function '" + this._sig + "' are not supported.";
	}	

	// if the output are valid, drop the pointer from the type
	this._outputTypes = this._outputTypes.map(function(e){return Strings.replacer(e, /\*/, '').trim();});
}
