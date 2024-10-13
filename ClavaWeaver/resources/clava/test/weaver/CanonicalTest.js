import Query from "@specs-feup/lara/api/weaver/Query.js";

// Canonical functions
for(const $function of Query.search("function", {isCanonical: true})) {
	console.log("Canonical function " + $function.signature + " at line " + $function.line);
}