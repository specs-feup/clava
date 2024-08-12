import { Call } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";
/**
 * Check for the presence of memcpy functions
 */
export default class MemcpyChecker extends Checker {
    advice = " memcpy() doesn't check the length of the destination when copying: risk of buffer overflow. " +
        "Check if the length of the destination is sufficient (CWE-120).\n\n";
    constructor() {
        super("memcpy");
    }
    check($node) {
        if (!($node instanceof Call) || $node.name !== "memcpy") {
            return;
        }
        return new CheckResult(this.name, $node, this.advice);
    }
}
//# sourceMappingURL=MemcpyChecker.js.map