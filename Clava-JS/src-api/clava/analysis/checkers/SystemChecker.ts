import { Call, Joinpoint } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";

/**
 * Check for the presence of system functions
 */
export default class SystemChecker extends Checker {
  private advice =
    " This function executes the command used as parameter and can allow an attacker to execute his own code. " +
    "Be extremely cautious when using this function and check inputs for a better security (CWE-78).\n\n";

  constructor() {
    super("system");
  }

  check($node: Joinpoint): CheckResult | undefined {
    if (!($node instanceof Call) || $node.name !== "system") {
      return;
    }
    return new CheckResult(this.name, $node, this.advice);
  }
}
