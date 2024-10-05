laraImport("weaver.Query");
laraImport("clava.pass.DecomposeVarDeclarations");

const result = new DecomposeVarDeclarations().apply();
console.log("Result: " + result);
console.log(Query.root().code);
