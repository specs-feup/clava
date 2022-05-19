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

    const $varRef = $complexAssignment.left;
    const $rhs = $complexAssignment.right;

    const $binaryOp = ClavaJoinPoints.binaryOp(
      this.#ops.get($complexAssignment.operator),
      $varRef.copy(),
      $rhs,
      $varRef.decl.type
    );
    $complexAssignment.replaceWith(ClavaJoinPoints.assign($varRef, $binaryOp));
  }
}
