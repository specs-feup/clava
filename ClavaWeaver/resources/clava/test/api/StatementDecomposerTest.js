import StatementDecomposer from "@specs-feup/clava/api/clava/code/StatementDecomposer.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

//setDebug();
var decomposer = new StatementDecomposer();

for (var $stmt of Query.search("function", "foo").search("statement")) {
  decomposer.decomposeAndReplace($stmt);
}

println(Query.search("function", "foo").first().code);
