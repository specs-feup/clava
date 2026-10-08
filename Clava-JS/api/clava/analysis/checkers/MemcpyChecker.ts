import { Call, Joinpoint } from "../../../Joinpoints.ts";
import Checker from "../Checker.ts";
import CheckResult from "../CheckResult.ts";

/**
 * Check for the presence of memcpy functions
 */
export default class MemcpyChecker extends Checker {
  private advice =
    " memcpy() doesn't check the length of the destination when copying: risk of buffer overflow. " +
    "Check if the length of the destination is sufficient (CWE-120).\n\n";

  constructor() {
    super("memcpy");
  }

  check($node: Joinpoint) {
    if (!($node instanceof Call) || $node.name !== "memcpy") {
      return;
    }
    return new CheckResult(this.name, $node, this.advice);
  }
}
