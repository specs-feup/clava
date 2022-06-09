laraImport("weaver.Query");
laraImport("clava.code.StatementDecomposer");
laraImport("clava.pass.SimplifyLoops");
laraImport("clava.pass.DecomposeVarDeclarations");
laraImport("clava.pass.DecomposeDeclStmt");
laraImport("clava.pass.SimplifySelectionStmts");
laraImport("clava.code.SimplifyAssignment");

function NormalizeToSubset($startJp) {
  const declStmt = new DecomposeDeclStmt();
  const varDecls = new DecomposeVarDeclarations();
  const statementDecomposer = new StatementDecomposer();
  const simplifyLoops = new SimplifyLoops(statementDecomposer);
  const simplifyIfs = new SimplifySelectionStmts(statementDecomposer);

  simplifyLoops.apply($startJp);
  simplifyIfs.apply($startJp);

  declStmt.apply($startJp);
  varDecls.apply($startJp);

  for (const $assign of Query.searchFrom($startJp, "binaryOp", {
    self: (self) => self.isAssignment && self.operator !== "=",
  })) {
    SimplifyAssignment($assign);
  }
}
