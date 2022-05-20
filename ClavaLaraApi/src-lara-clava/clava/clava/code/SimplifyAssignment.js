laraImport("clava.ClavaJoinPoints");

class SimplifyAssignment {
  /**
   * Non-assignment counterparts of complex assignment operators (lookup table)
   */
  static #ops = new Map([
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

  static apply($complexAssignment) {
    // early return if current node is not suitable for this transform
    if (
      !$complexAssignment.instanceOf("binaryOp") ||
      !this.#ops.has($complexAssignment.operator)
    ) {
      return;
    }

    const $lValue = $complexAssignment.left;
    const $rValue = $complexAssignment.right;

    const $binaryOp = ClavaJoinPoints.binaryOp(
      this.#ops.get($complexAssignment.operator),
      $lValue.copy(),
      $rValue,
      $complexAssignment.type
    );
    $complexAssignment.replaceWith(ClavaJoinPoints.assign($lValue, $binaryOp));
  }
}
