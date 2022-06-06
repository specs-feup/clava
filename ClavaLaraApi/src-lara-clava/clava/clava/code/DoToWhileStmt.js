laraImport("clava.ClavaJoinPoints");

function DoToWhileStmt($doStmt) {
  // do statements have an unconditional first iteration
  const firstIterStmts = $doStmt.scopeNodes.map(($stmt) => $stmt.copy());

  $doStmt.insertBefore(ClavaJoinPoints.scope(firstIterStmts));
  const $while = ClavaJoinPoints.whileStmt($doStmt.cond, $doStmt.body);
  $doStmt.replaceWith($while);
  return $while;
}
