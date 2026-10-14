import { BinaryOp, Expression, OpKind } from "../../Joinpoints.ts";
import ClavaJoinPoints from "../ClavaJoinPoints.ts";

/**
 * Simplifies assignments of the type `a += b` into the equivalent expression `a = a + b`
 * @param $complexAssignment - The expression to simplify
 */
export default function SimplifyAssignment($complexAssignment: BinaryOp): void {
  // early return if current node is not suitable for this transform
  if (!ops.has($complexAssignment.kind)) {
    return;
  }

  const $lValue = $complexAssignment.left;
  const $rValue = $complexAssignment.right;

  const $binaryOp = ClavaJoinPoints.binaryOp(
    ops.get($complexAssignment.kind)!,
    $lValue.copy() as Expression,
    $rValue,
    $complexAssignment.type
  );
  $complexAssignment.replaceWith(ClavaJoinPoints.assign($lValue, $binaryOp));
}

/**
 * Non-assignment counterparts of complex assignment operators (lookup table)
 */
const ops = new Map<OpKind, OpKind>([
  [OpKind.mul_assign, OpKind.mul],
  [OpKind.div_assign, OpKind.div],
  [OpKind.rem_assign, OpKind.rem],
  [OpKind.add_assign, OpKind.add],
  [OpKind.sub_assign, OpKind.sub],
  [OpKind.shl_assign, OpKind.shl],
  [OpKind.shr_assign, OpKind.shr],
  [OpKind.and_assign, OpKind.and],
  [OpKind.xor_assign, OpKind.xor],
  [OpKind.or_assign, OpKind.or],
]);
