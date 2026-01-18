import Query from "@specs-feup/lara/api/weaver/Query.js";

// Count statements
const $body = Query.search("function", "numStatements").first().body;
console.log("numStatements (depth): " + $body.getNumStatements());
console.log("numStatements (flat): " + $body.getNumStatements(true));
