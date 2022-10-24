laraImport("clava.code.StatementDecomposer");
laraImport("weaver.Query");

//setDebug();
var decomposer = new StatementDecomposer();

for (var $stmt of Query.search("function", "foo").search("statement")) {
  decomposer.decomposeAndReplace($stmt);
  /*
		var decomposedStmts = decomposer.decompose($stmt);
		
		if(decomposedStmts.length > 0) {
			println("Original stmt:" + $stmt.code);
			for(var $decompStmt of decomposedStmts) {
				println("Decomp: " + $decompStmt.code);
			}		
		}
		*/
}

println(Query.search("function", "foo").first().code);
