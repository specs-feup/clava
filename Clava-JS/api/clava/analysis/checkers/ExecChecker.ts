import { Call, Joinpoint } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";

/**
 * Check for the presence of exec family functions
 */
export default class ExecChecker extends Checker {
  private advice =
    " This function executes another program used as parameter and can allow an attacker to execute his own code. " +
    "Be extremely cautious when using this function and check inputs for a better security (CWE-78).\n\n";

  constructor() {
    super("exec");
  }

  check($node: Joinpoint): CheckResult | undefined {
    if (
      !($node instanceof Call) ||
      !$node.name.match(/execl|execlp|execle|execv|execvp|execvpe/)
    ) {
      return;
    }
    return new CheckResult(this.name, $node, this.advice);
  }
}
