import { Vardecl } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";
/**
 * Check for the presence of lambda objects using capture by reference
 */
export default class LambdaChecker extends Checker {
    advice = " A lambda object must not outlive any of its reference captured objects." +
        "Make sure that variables contained in the lambda expression will not use an obsolete pointer.(CWE-416).\n\n";
    constructor() {
        super("lambda");
    }
    check($node) {
        if (!$node.code.match(/.*\[&\]\s*\(.*\)\s*\{.*\}.*/) ||
            !($node instanceof Vardecl)) {
            return;
        }
        return new CheckResult(this.name, $node, this.advice);
    }
}
//# sourceMappingURL=LambdaChecker.js.map