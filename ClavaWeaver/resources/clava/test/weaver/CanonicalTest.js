laraImport("weaver.Query");

// Canonical functions
for(const $function of Query.search("function", {isCanonical: true})) {
	println("Canonical function " + $function.signature + " at line " + $function.line);
}