import weaver.Query;

aspectdef ArrayAccess

/*
	select body.stmt end
	apply
		println("STMT:" + $stmt.code);
	end
	*/
/*
	select stmt.arrayAccess end
	apply
		println("Array var: " + $arrayAccess.arrayVar.name);
		println($arrayAccess.code + "->" + $arrayAccess.use);
	end
*/	
	for(var $arrayAccess of Query.search("arrayAccess")) {
		println("Array var: " + $arrayAccess.arrayVar.name);
		println($arrayAccess.code + "->" + $arrayAccess.use);	
	}

end