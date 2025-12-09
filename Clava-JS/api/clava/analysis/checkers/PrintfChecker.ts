import { Call, Joinpoint } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";

/**
 * Check for the presence of printf functions
 */
export default class PrintfChecker extends Checker {
  private advice =
    " Variable used as format string can be modified by an attacker. Use a constant format specification instead (CWE-134).\n\n";

  constructor() {
    super("printf");
  }

  check($node: Joinpoint) {
    if (
      !($node instanceof Call) ||
      $node.name !== "printf" ||
      $node.args[0].code.match(/\s*".*"\s*/)
    ) {
      return;
    }
    return new CheckResult(this.name, $node, this.advice);
  }
}
