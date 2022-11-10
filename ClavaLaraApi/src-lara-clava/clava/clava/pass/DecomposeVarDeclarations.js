laraImport("lara.pass.Pass");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.pass.PassTransformationResult");

/**
 * Decomposes the vardecl nodes that are reachable from the given join point.
 *
 * E.g. transforms int i = 0; into int i; i = 0;
 *
 * Does not support decomposition for variables that are arrays, in those cases the code stays unchanged.
 */
class DecomposeVarDeclarations extends Pass {
  constructor() {
    super();
  }

  matchJoinpoint($jp) {
    return (
      $jp.instanceOf("vardecl") && // Must be a variable declaration
      $jp.hasInit && // Must have initialization
      $jp.initStyle === "cinit" && // Only C-style initializations
      !$jp.isGlobal && // Ignore global variables
      !$jp.isInsideHeader && // Ignore if inside any header (e.g. if, switch, loop...)
      !$jp.type.instanceOf("arrayType") // Ignore if array
    );
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
