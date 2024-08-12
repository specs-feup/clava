import { Call } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";
/**
 * Check for the presence of fprintf functions
 */
export default class FprintfChecker extends Checker {
    advice = " Variable used as format string can be modified by an attacker.Use a constant format specification instead (CWE-134).\n\n";
    constructor() {
        super("fprintf");
    }
    check($node) {
        if (!($node instanceof Call) ||
            $node.name !== "fprintf" ||
            $node.args[1].code.match(/\s*".*"\s*/)) {
            return;
        }
        return new CheckResult(this.name, $node, this.advice);
    }
}
//# sourceMappingURL=FprintfChecker.js.map