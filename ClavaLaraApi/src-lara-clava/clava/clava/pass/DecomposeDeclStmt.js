laraImport("lara.pass.SimplePass");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.pass.results.PassResult");

/**
 * Decomposes composite declaration statements into separate statements for each variable.
 *
 * This means that a declaration like:
 *
 * ```c
 * int a, b = 10, c;
 * ```
 *
 * Will be decomposed to:
 *
 * ```c
 * int a;
 * int b = 10;
 * int c;
 * ```
 */
class DecomposeDeclStmt extends SimplePass {

  /**
   * @return {string} Name of the pass
   * @override
   */
  get name() {
    return "DecomposeDeclStmt";
  }

  matchJoinpoint($jp) {
    if (!$jp.instanceOf("declStmt")) {
      return false;
    }
    if ($jp.numChildren <= 1) {
      return false;
    }
    return true;
  }

  transformJoinpoint($jp) {
    let $firstDeclStmt;
    for (const $decl of $jp.decls) {
      const $singleDeclStmt = ClavaJoinPoints.declStmt($decl);
      if (!$firstDeclStmt) {
        $firstDeclStmt = $singleDeclStmt;
      }
      $jp.insertBefore($singleDeclStmt);
    }
    $jp.detach();
    return new PassResult(this, $firstDeclStmt);
  }
}
