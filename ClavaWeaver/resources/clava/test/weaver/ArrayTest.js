import weaver.Query;

aspectdef ArrayTest

	for(var $vardecl of Query.search('function', 'constantArrays').search('vardecl')) {
		println($vardecl.name + " dims: " + $vardecl.type.arrayDims);
		println($vardecl.name + "array size: " + $vardecl.type.arraySize);
	}

end
