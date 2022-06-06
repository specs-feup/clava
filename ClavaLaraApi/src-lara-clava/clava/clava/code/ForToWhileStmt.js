laraImport("clava.ClavaJoinPoints");

function ForToWhileStmt($forStmt) {
  const $scope = ClavaJoinPoints.scope([
    $forStmt.init,
    ClavaJoinPoints.whileStmt(
      $forStmt.cond,
      ClavaJoinPoints.scope([...$forStmt.scopeNodes, $forStmt.step])
    ),
  ]);

  $forStmt.replaceWith(scope);

  return $scope;
}
