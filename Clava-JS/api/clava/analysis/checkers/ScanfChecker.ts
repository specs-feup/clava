import { Call, Joinpoint } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";

/**
 * Check for the presence of scanf functions
 */
export default class ScanfChecker extends Checker {
  private advice =
    " If there is no limit specification in the format specifier an attacker can cause buffer overflows. Use a size limitation in format specification (CWE-120, CWE-20).\n\n";

  constructor() {
    super("scanf");
  }

  check($node: Joinpoint) {
    if (
      !($node instanceof Call) ||
      $node.name !== "scanf" ||
      $node.args[0].code.match(/\s*".*%[0-9]+[a-zA-Z]+.*"\s*/)
    ) {
      return;
    }

    return new CheckResult(this.name, $node, this.advice);
  }
}
