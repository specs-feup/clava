import weaver.Query;

aspectdef CudaQuery

	for(var $jp  of Query.search("arrayAccess")) {
		println("arrayAccess var: " + $jp.name);
	}
/*
	for(var $jp  of Query.search("varref")) {
		println("code: " + $jp.code + " name: " + $jp.name);
	}	
*/
end