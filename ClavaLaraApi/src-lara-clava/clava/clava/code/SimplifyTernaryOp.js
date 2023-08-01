laraImport("clava.ClavaJoinPoints");

/**
 * Simplifies a statement like:
 *
 * ```c
 * x = y ? a : b;
 * ```
 *
 * Into:
 *
 * ```c
 * if (y) {
 *    x = a;
 * } else {
 *    x = b;
 * }
 * ```
 *
 * $assignmentStmt must be an expression statement (not an inline expression, such as in a
 * varDecl or within another expression). The expression statement must only contain an assignment
 * operator, and the rvalue must be a ternary operator.
 *
 * Otherwise the function will immediately return.
 *
 * @param {$exprStmt} $assignmentStmt Expression statement containing an assignment where the right hand side is a ternary operator
 */
function SimplifyTernaryOp($assignmentStmt) {
  // early return if current node is not suitable
  const applicable =
    $assignmentStmt.getInstanceOf("exprStmt") &&
    $assignmentStmt.expr.getInstanceOf("binaryOp") &&
    $assignmentStmt.expr.isAssignment &&
    $assignmentStmt.expr.right.getInstanceOf("ternaryOp");
  if (!applicable) {
    return;
  }
  const $assignment = $assignmentStmt.expr;
  const $ternaryOp = $assignment.right;

  const $trueStmt = $assignmentStmt.copy();
  $trueStmt.expr.right = $ternaryOp.trueExpr;

  const $falseStmt = $assignmentStmt.copy();
  $falseStmt.expr.right = $ternaryOp.falseExpr;

  const $ifStmt = ClavaJoinPoints.ifStmt(
    $ternaryOp.cond,
    ClavaJoinPoints.scope([$trueStmt]),
    ClavaJoinPoints.scope([$falseStmt])
  );

  $assignmentStmt.replaceWith($ifStmt);
}
