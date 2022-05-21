laraImport("clava.Clava");
laraImport("clava.code.SimplifyTernaryOp");
laraImport("weaver.Query");

for ($stmt of Query.search("exprStmt")) {
  SimplifyTernaryOp.apply($stmt);
}

Clava.rebuild();
println(Query.root().code);
