laraImport("lara.pass.Pass");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.pass.PassTransformationResult");

/**
 * Decomposes the vardecl nodes that are reachable from the given join point.
 *
 * E.g. transforms int i = 0; into int i; i = 0;
 */
class DecomposeVarDeclarations extends Pass {
  constructor() {
    super();
  }

  matchJoinpoint($jp) {
    return $jp.instanceOf("vardecl") && $jp.hasInit;
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

    return new PassTransformationResult({
      pass: DecomposeVarDeclarations,
      $joinpoint: $vardecl,
      insertedLiteralCode: false,
    });
  }
}
