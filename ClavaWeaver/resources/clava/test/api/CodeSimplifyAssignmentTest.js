laraImport("clava.Clava");
laraImport("clava.code.SimplifyAssignment");
laraImport("weaver.Query");

for (const $op of Query.search("binaryOp")) {
  SimplifyAssignment($op);
}

Clava.rebuild();
println(Query.root().code);
