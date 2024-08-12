import { Call } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";
import Fix from "../Fix.js";
/**
 * Check for the presence of strcat functions
 */
export default class StrcatChecker extends Checker {
    advice = " Unsafe function strcat() can be replaced by safer strncat()(Possible Fix). Be careful though because strncat() doesn't null-terminate. strcat() doesn't check the length of the buffer: risk of buffer overflow (CWE-120).\n\n";
    constructor() {
        super("strcat");
    }
    static fixAction($node) {
        const newFunction = `strncat(${$node.args[0].code}, ${$node.args[1].code}, sizeof(${$node.args[0].code}))`;
        $node.replaceWith(newFunction);
    }
    check($node) {
        if (!($node instanceof Call) || $node.name !== "strcat") {
            return;
        }
        return new CheckResult(this.name, $node, this.advice, new Fix($node, () => {
            StrcatChecker.fixAction($node);
        }));
    }
}
//# sourceMappingURL=StrcatChecker.js.map