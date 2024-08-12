import { Call } from "../../../Joinpoints.js";
import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";
import Fix from "../Fix.js";
/**
 * Check for the presence of sprintf functions
 */
export default class SprintfChecker extends Checker {
    advice = " Unsafe function sprintf() can be replaced by safer snprintf()(Possible Fix). sprintf() doesn't check the length of the buffer: risk of buffer overflow (CWE-120).\n\n";
    constructor() {
        super("sprintf");
    }
    static fixAction($node) {
        let newFunction = `snprintf(${$node.args[0].code}, sizeof(${$node.args[0].code}), ${$node.args[1].code}`;
        if ($node.args.length > 2) {
            let i;
            for (i = 2; i < $node.args.length; i++) {
                newFunction += "," + $node.args[i].code;
            }
        }
        newFunction += ")";
        $node.replaceWith(newFunction);
    }
    check($node) {
        if (!($node instanceof Call) || $node.name !== "sprintf") {
            return;
        }
        if (!$node.args[1].code.match(/\s*".*"\s*/)) {
            this.advice +=
                " Variable used as format string can be modified by an attacker. Use a constant format specification instead (CWE-134).\n\n";
        }
        return new CheckResult(this.name, $node, this.advice, new Fix($node, () => {
            SprintfChecker.fixAction($node);
        }));
    }
}
//# sourceMappingURL=SprintfChecker.js.map