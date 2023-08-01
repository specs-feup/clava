laraImport("lara.pass.SimplePass");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.pass.results.PassResult");

/**
 * Decomposes the vardecl nodes that are reachable from the given join point.
 *
 * E.g. transforms int i = 0; into int i; i = 0;
 *
 * Does not support decomposition for variables that are arrays, in those cases the code stays unchanged.
 */
class DecomposeVarDeclarations extends SimplePass {

  /**
   * @return {string} Name of the pass
   * @override
   */
  get name() {
    return "DecomposeVarDeclarations";
  }

  matchJoinpoint($jp) {
    return (
      $jp.getInstanceOf("vardecl") && // Must be a variable declaration
      $jp.hasInit && // Must have initialization
      $jp.initStyle === "cinit" && // Only C-style initializations
      !$jp.isGlobal && // Ignore global variables
      !$jp.isInsideHeader && // Ignore if inside any header (e.g. if, switch, loop...)
      !$jp.type.getInstanceOf("arrayType") && // Ignore if array
      !this.#isLiteralAuto($jp) // Specific case of vardecl in literal code that uses auto (e.g. as inserted by Timer)
    );
  }

  #isLiteralAuto($jp) {
    return $jp.type.isAuto && $jp.init.type.getInstanceOf("undefinedType");
  }

  transformJoinpoint($vardecl) {
    // store init expression for later
    const $init = $vardecl.init;

    // remove init from ast and make type explicit, if necessary
    $vardecl.removeInit();
    if ($vardecl.type.isAuto) {
      $vardecl.type = $init.type;
    }

    const { assign, varRef, exprStmt } = ClavaJoinPoints;
    const $newInitStmt = exprStmt(assign(varRef($vardecl), $init));

    $vardecl.insertAfter($newInitStmt);

    return new PassResult(this, $vardecl);
  }
}
