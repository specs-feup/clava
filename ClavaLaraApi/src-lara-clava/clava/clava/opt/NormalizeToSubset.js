laraImport("weaver.Query");
laraImport("clava.code.StatementDecomposer");
laraImport("clava.pass.SimplifyLoops");
laraImport("clava.pass.DecomposeVarDeclarations");
laraImport("clava.pass.DecomposeDeclStmt");
laraImport("clava.pass.SimplifySelectionStmts");
laraImport("clava.pass.SimplifyReturnStmts");
laraImport("clava.code.SimplifyAssignment");

/**
 *
 * @param {$jp} $startJp
 * @param {object} options - Object with options. Supported options: 'simplifyLoops' (default: {}), options for pass SimplifyLoops
 */
function NormalizeToSubset($startJp, options) {
  const _options = options ?? {};
  _options["simplifyLoops"] ??= {};

  const declStmt = new DecomposeDeclStmt();
  const varDecls = new DecomposeVarDeclarations();
  const statementDecomposer = new StatementDecomposer();
  const simplifyLoops = new SimplifyLoops(
    statementDecomposer,
    _options["simplifyLoops"]
  );
  const simplifyIfs = new SimplifySelectionStmts(statementDecomposer);
  const simplifyReturns = new SimplifyReturnStmts(statementDecomposer);

  simplifyLoops.apply($startJp);
  simplifyIfs.apply($startJp);
  simplifyReturns.apply($startJp);

  declStmt.apply($startJp);
  varDecls.apply($startJp);

  for (const $assign of Query.searchFrom($startJp, "binaryOp", {
    self: (self) => self.isAssignment && self.operator !== "=",
  })) {
    SimplifyAssignment($assign);
  }
}
