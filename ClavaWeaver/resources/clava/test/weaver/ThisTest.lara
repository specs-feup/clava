import weaver.Query;

aspectdef ThisTest

	println("This decl");
	for(var $memberAccess of Query.search("function", "getVolume").search("memberAccess")) {
		println($memberAccess.base.decl.name);
	}


end
