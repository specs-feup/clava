import { Call, Joinpoint } from "../../../Joinpoints.ts";
import Checker from "../Checker.ts";
import CheckResult from "../CheckResult.ts";

/**
 * Check for the presence of chgrp functions
 */
export default class ChgrpChecker extends Checker {
  private advice =
    " This function uses paths to files, if an attacker can modify or move these files " +
    " he can redirect the execution flow or create a race condition. Consider using fchgrp() instead (CWE-362).\n\n";

  constructor() {
    super("chgrp");
  }

  check($node: Joinpoint): CheckResult | undefined {
    if (!($node instanceof Call) || $node.name !== "chgrp") {
      return;
    }
    return new CheckResult(this.name, $node, this.advice);
  }
}
