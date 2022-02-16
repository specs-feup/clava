laraImport("lara.pass.Pass");
laraImport("clava.ClavaJoinPoints");

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
class DecomposeDeclStmt extends Pass {
  constructor() {
    super("DecomposeDeclStmt");
  }

  _apply_impl($jp) {
    // Find all declaration statements
    for (const $declStmt of Query.searchFromInclusive($jp, "declStmt")) {
      // Ignore statement if it only declares one variable
      if ($declStmt.numChildren <= 1) {
        continue;
      }

      // Create new statement for each declaration
      // Insert it before the old node to preserve the order of declarations
      for (const $varDecl of $declStmt.decls) {
        $declStmt.insertBefore(ClavaJoinPoints.declStmt($varDecl));
      }

      // Remove the old statement
      $declStmt.detach();
    }
  }
}