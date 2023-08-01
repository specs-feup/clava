laraImport("weaver.Query");

// Count statements
const $body = Query.search("function", "numStatements").first().body;
println("numStatements (depth): " + $body.numStatements);
println("numStatements (flat): " + $body.getNumStatements(true));
