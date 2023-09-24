import { Call } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";
export default class CinChecker extends Checker {
    advice = " Using std::cin with operator>> is risky because there is no verification for buffer overflow. Consider using a safer way to retrieve user input (CWE-20).\n\n";
    constructor() {
        super("cin");
    }
    check($node) {
        if (!($node instanceof Call) ||
            $node.name !== "operator>>" ||
            ($node.args[0].code !== "cin" && $node.args[0].code !== "std::cin")) {
            return;
        }
        return new CheckResult(this.name, $node, this.advice);
    }
}
//# sourceMappingURL=CinChecker.js.map