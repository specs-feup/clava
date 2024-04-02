import { Call, Joinpoint } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";

/**
 * Check for the presence of syslog functions
 */
export default class SyslogChecker extends Checker {
  private advice =
    " Variable used as format string can be modified by an attacker. Use a constant format specification instead (CWE-134).\n\n";

  constructor() {
    super("syslog");
  }

  check($node: Joinpoint) {
    if (
      !($node instanceof Call) ||
      $node.name !== "syslog" ||
      $node.args[1].code.match(/\s*".*"\s*/)
    ) {
      return;
    }

    return new CheckResult(this.name, $node, this.advice);
  }
}
