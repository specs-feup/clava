import { Call, Joinpoint } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";
import Fix from "../Fix.js";

/**
 * Check for the presence of gets functions
 */
export default class GetsChecker extends Checker {
  private advice =
    " Unsafe function gets() can be replaced by safer fgets()(Possible Fix). gets() doesn't check the length of the buffer: risk of buffer overflow (CWE-120, CWE-20).\n\n";

  constructor() {
    super("gets");
  }

  static fixAction($node: Call) {
    const newFunction = `fgets(${$node.args[0].code}, sizeof(${$node.args[0].code}), stdin)`;
    $node.replaceWith(newFunction);
  }

  check($node: Joinpoint) {
    if (!($node instanceof Call) || $node.name !== "gets") {
      return;
    }
    return new CheckResult(
      this.name,
      $node,
      this.advice,
      new Fix($node, () => {
        GetsChecker.fixAction($node);
      })
    );
  }
}
