laraImport("weaver.Query")

for(const $emptyStmt of Query.search("function", "main").search("emptyStmt")) {
	println("EmptyStmt: " + $emptyStmt.line)
}

println(Query.root().code)