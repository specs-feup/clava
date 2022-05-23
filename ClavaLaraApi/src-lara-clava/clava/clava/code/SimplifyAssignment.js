laraImport("clava.ClavaJoinPoints");

/**
 * Simplifies assignments of the type `a += b` into the equivalent expression `a = a + b`
 * @param {$binaryOp} $complexAssignment The expression to simplify
 */
function SimplifyAssignment($complexAssignment) {
  // early return if current node is not suitable for this transform
  if (
    !$complexAssignment ||
    !$complexAssignment.instanceOf("binaryOp") ||
    !SimplifyAssignment._ops.has($complexAssignment.operator)
  ) {
    return;
  }

  const $lValue = $complexAssignment.left;
  const $rValue = $complexAssignment.right;

  const $binaryOp = ClavaJoinPoints.binaryOp(
    SimplifyAssignment._ops.get($complexAssignment.operator),
    $lValue.copy(),
    $rValue,
    $complexAssignment.type
  );
  $complexAssignment.replaceWith(ClavaJoinPoints.assign($lValue, $binaryOp));
}

/**
 * Non-assignment counterparts of complex assignment operators (lookup table)
 */
SimplifyAssignment._ops = new Map([
  ["*=", "*"],
  ["/=", "/"],
  ["%=", "%"],
  ["+=", "+"],
  ["-=", "-"],
  ["<<=", "<<"],
  [">>=", ">>"],
  ["&=", "&"],
  ["^=", "^"],
  ["|=", "|"],
]);
