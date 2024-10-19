import antarex.multi.MultiVersionPointersAspects;

var MultiVersionPointers = function($func, dims, name) {
	
	checkDefined($func, '$func', 'MultiVersionPointers');
	checkDefined(dims, 'dims', 'MultiVersionPointers');
	
	if (name === undefined) {
		name = $func.name + '_pointers';
	}
	
	this._dims = dims,
	
	this._versions = {};
	
	this._name = name;
	
	this._typeName = this._name + '_ptr_t';
	
	this._typedef = 'typedef ';
	
	this._typedef += $func.type.code;
	this._typedef += '(*' + this._typeName + ') (';

	this._typedef += $func.params.map(function($p){
		return $p.code;
	}).join(',');

	this._typedef += ');';
};

MultiVersionPointers.prototype.declare = function($file) {
	
	var d = new _MVP_DeclarePointers_($file, this._name, this._typeName, this._typedef, this._dims);
	call d();
}

MultiVersionPointers.prototype.add = function(funcName, pos) {
	
	this._versions[pos] = funcName;
}

MultiVersionPointers.prototype.init = function($func) {
	
	var code = '';
	
	for(var v in this._versions) {
		
		code += this._name + '[' + v.split(',').join('][') + '] = ' + this._versions[v] + ';';
		code += '\n';
	}
	
	var i = new _MVP_InitPointers_($func, code);
	i.call();
}

MultiVersionPointers.prototype.replaceCall = function($call, dims) {
	
	var newName = this._name + '[' + dims.join('][') + ']';
	$call.exec setName(newName);
}
