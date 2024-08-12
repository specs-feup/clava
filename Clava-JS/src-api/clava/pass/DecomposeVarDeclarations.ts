import SimplePass from "lara-js/api/lara/pass/SimplePass.js";
import PassResult from "lara-js/api/lara/pass/results/PassResult.js";
import {
  ArrayType,
  Joinpoint,
  UndefinedType,
  Vardecl,
} from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";

/**
 * Decomposes the vardecl nodes that are reachable from the given join point.
 *
 * E.g. transforms int i = 0; into int i; i = 0;
 *
 * Does not support decomposition for variables that are arrays, in those cases the code stays unchanged.
 */
export default class DecomposeVarDeclarations extends SimplePass {
  protected _name = "DecomposeVarDeclarations";

  matchJoinpoint($jp: Joinpoint): boolean {
    return (
      $jp instanceof Vardecl && // Must be a variable declaration
      $jp.hasInit && // Must have initialization
      $jp.initStyle === "cinit" && // Only C-style initializations
      !$jp.isGlobal && // Ignore global variables
      !$jp.isInsideHeader && // Ignore if inside any header (e.g. if, switch, loop...)
      !($jp.type instanceof ArrayType) && // Ignore if array
      !this.isLiteralAuto($jp) // Specific case of vardecl in literal code that uses auto (e.g. as inserted by Timer)
    );
  }

  private isLiteralAuto($jp: Vardecl): boolean {
    return $jp.type.isAuto && $jp.init.type instanceof UndefinedType;
  }

  transformJoinpoint($vardecl: Vardecl): PassResult {
    // store init expression for later
    const $init = $vardecl.init;

    // remove init from ast and make type explicit, if necessary
    $vardecl.removeInit();
    if ($vardecl.type.isAuto) {
      $vardecl.type = $init.type;
    }

    const $newInitStmt = ClavaJoinPoints.exprStmt(
      ClavaJoinPoints.assign(ClavaJoinPoints.varRef($vardecl), $init)
    );

    $vardecl.insertAfter($newInitStmt);

    return new PassResult(this, $vardecl);
  }
}
