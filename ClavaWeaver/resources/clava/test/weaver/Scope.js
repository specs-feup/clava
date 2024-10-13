import Query from "@specs-feup/lara/api/weaver/Query.js";

// Count statements
const $body = Query.search("function", "numStatements").first().body;
println("numStatements (depth): " + $body.getNumStatements());
println("numStatements (flat): " + $body.getNumStatements(true));
