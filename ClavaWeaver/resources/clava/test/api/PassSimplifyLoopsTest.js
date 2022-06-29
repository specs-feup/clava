laraImport("clava.Clava");
laraImport("clava.pass.SimplifyLoops");
laraImport("clava.code.StatementDecomposer");
laraImport("clava.pass.DecomposeVarDeclarations");
laraImport("weaver.Query");

const statementDecomposer = new StatementDecomposer();
const pass = new SimplifyLoops(statementDecomposer);

pass.apply(Query.root());

println(Query.root().code);
