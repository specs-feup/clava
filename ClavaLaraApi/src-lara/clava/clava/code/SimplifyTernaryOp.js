import { BinaryOp, TernaryOp } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
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
 * @param $assignmentStmt - Expression statement containing an assignment where the right hand side is a ternary operator
 */
export default function SimplifyTernaryOp($assignmentStmt) {
    // early return if current node is not suitable
    if (!($assignmentStmt.expr instanceof BinaryOp &&
        $assignmentStmt.expr.isAssignment &&
        $assignmentStmt.expr.right instanceof TernaryOp)) {
        return;
    }
    const $assignment = $assignmentStmt.expr;
    const $ternaryOp = $assignment.right;
    const $trueStmt = $assignmentStmt.copy();
    $trueStmt.expr.right = $ternaryOp.trueExpr;
    const $falseStmt = $assignmentStmt.copy();
    $falseStmt.expr.right = $ternaryOp.falseExpr;
    const $ifStmt = ClavaJoinPoints.ifStmt($ternaryOp.cond, ClavaJoinPoints.scope($trueStmt), ClavaJoinPoints.scope($falseStmt));
    $assignmentStmt.replaceWith($ifStmt);
}
//# sourceMappingURL=SimplifyTernaryOp.js.map