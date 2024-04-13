laraImport("clava.code.StatementDecomposer");
laraImport("weaver.Query");

//setDebug();
var decomposer = new StatementDecomposer();

for (var $stmt of Query.search("function", "foo").search("statement")) {
  decomposer.decomposeAndReplace($stmt);
}

println(Query.search("function", "foo").first().code);
