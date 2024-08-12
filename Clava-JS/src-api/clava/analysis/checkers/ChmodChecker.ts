import { Call, Joinpoint } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";

/**
 * Check for the presence of chmod functions
 */
export default class ChmodChecker extends Checker {
  private advice =
    " This function uses paths to files, if an attacker can modify or move these files " +
    " he can redirect the execution flow or create a race condition. Consider using fchmod() instead (CWE-362).\n\n";

  constructor() {
    super("chmod");
  }

  check($node: Joinpoint): CheckResult | undefined {
    if (!($node instanceof Call) || $node.name !== "chmod") {
      return;
    }
    return new CheckResult(this.name, $node, this.advice);
  }
}
