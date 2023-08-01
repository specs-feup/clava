laraImport("lara.pass.SimplePass");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.pass.results.PassResult");
laraImport("lara.pass.PassTransformationError");

/**
 * Transforms local static variables into global variables.
 *
 * This means that code like this:
 *
 * ```c
 * int foo() {
 *    static int x = 10;
 *    return x;
 * }
 * ```
 *
 * Will be transformed into:
 *
 * ```c
 * int foo_static_x = 10;
 *
 * int foo() {
 *    return foo_static_x;
 * }
 * ```
 */
class LocalStaticToGlobal extends SimplePass {

  /**
   * @return {string} Name of the pass
   * @override
   */
  get name() {
    return "LocalStaticToGlobal";
  }

  matchJoinpoint($jp) {
    // Only vardecls
    if (!$jp.getInstanceOf("vardecl")) {
      return false;
    }

    // With static storage
    if ($jp.storageClass !== "static") {
      return false;
    }

    // Inside functions - not sure if this is needed
    if ($jp.getAncestor("function") === undefined) {
      return false;
    }

    return true;
  }

  transformJoinpoint($jp) {
    const $function = $jp.getAncestor("function");
    const newName = $function.name + "_static_" + $jp.name;

    $jp.name = newName;
    $jp.storageClass = "none";

    const $declStmt = $jp.parent;
    if (!$declStmt.getInstanceOf("declStmt")) {
      throw new PassTransformationError(
        "Expected declStmt, found '" + $declStmt.joinPointType + "'"
      );
    }

    $jp.detach();
    $function.insertBefore($jp);

    // Remove declStmt if empty
    if ($declStmt.decls.length === 0) {
      $declStmt.detach();
    }

    /*
    let $firstDeclStmt;
    for (const $decl of $jp.decls) {
      const $singleDeclStmt = ClavaJoinPoints.declStmt($decl);
      if (!$firstDeclStmt) {
        $firstDeclStmt = $singleDeclStmt;
      }
      $jp.insertBefore($singleDeclStmt);
    }
    $jp.detach();

    */

    return new PassResult(this, ClavaJoinPoints.emptyStmt());
  }
}
