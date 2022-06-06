laraImport("clava.ClavaJoinPoints");

function DoToWhileStmt($doStmt) {
  // do statements have an unconditional first iteration
  const firstIterStmts = $doStmt.scopeNodes.map(($stmt) => $stmt.copy());

  $doStmt.insertBefore(ClavaJoinPoints.scope(firstIterStmts));
  $doStmt.replaceWith(ClavaJoinPoints.whileStmt($doStmt.cond, $doStmt.body));
}
