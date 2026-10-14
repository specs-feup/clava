import { Call, Joinpoint } from "../../../Joinpoints.ts";
import Checker from "../Checker.ts";
import CheckResult from "../CheckResult.ts";

/**
 * Check for the presence of fscanf functions
 */
export default class FscanfChecker extends Checker {
  private advice =
    " If there is no limit specification in the format specifier an attacker can cause buffer overflows. Use a size limitation in format specification (CWE-120, CWE-20).\n\n";

  constructor() {
    super("fscanf");
  }

  check($node: Joinpoint): CheckResult | undefined {
    if (!($node instanceof Call) || $node.name !== "fscanf") {
      return;
    }
    return new CheckResult(this.name, $node, this.advice);
  }
}
