laraImport("weaver.Query")

for(const $emptyStmt of Query.search("function", "main").search("emptyStmt")) {
	console.log("EmptyStmt: " + $emptyStmt.line)
}

console.log(Query.root().code)