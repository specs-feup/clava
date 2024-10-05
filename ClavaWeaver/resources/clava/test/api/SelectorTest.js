import weaver.Query;

aspectdef SelectorTest

	for(var $chain of Query.search("function", "loops").search("loop").children("scope").children("loop").chain()) {	
		println("Loop:\n"+$chain["loop"].init.code);
	}


	// Test iterator
	var iteratorsStmts = 0;
	for(var $stmt of Query.search("function", "iterators").search("statement")) {	
		iteratorsStmts++;
	}
	println("Stmts in iterators: " + iteratorsStmts);

end
