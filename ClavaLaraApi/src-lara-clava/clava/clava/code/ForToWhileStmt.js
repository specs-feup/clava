laraImport("clava.ClavaJoinPoints");

function ForToWhileStmt($forStmt) {
  $forStmt.replaceWith(
    ClavaJoinPoints.scope([
      $forStmt.init,
      ClavaJoinPoints.whileStmt(
        $forStmt.cond,
        ClavaJoinPoints.scope([...$forStmt.scopeNodes, $forStmt.step])
      ),
    ])
  );
}
