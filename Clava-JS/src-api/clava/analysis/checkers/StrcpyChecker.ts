import Checker from "../Checker.js";
import CheckResult from "../CheckResult.js";
import Fix from "../Fix.js";
import { Joinpoint, Call } from "../../../Joinpoints.js";
import AnalyserResult from "../AnalyserResult.js";
import ClavaJoinPoints from "../../ClavaJoinPoints.js";

/*Check for the presence of strcpy functions*/

export default class StrcpyChecker extends Checker {
  private advice =
    " Unsafe function strcpy() can be replaced by safer strncpy()(Possible Fix). Be careful though because strncpy() doesn't null-terminate. strcpy() doesn't check the length of the buffer: risk of buffer overflow (CWE-120).\n\n";

  constructor() {
    super("strcpy");
  }

  static fixAction($jp: Call): void {
    const newFunction = ClavaJoinPoints.callFromName(
      "strncpy",
      ClavaJoinPoints.type("char *"),
      $jp.args[0],
      $jp.args[1],
      ClavaJoinPoints.exprLiteral(`sizeof(${$jp.args[0].code})`)
    );

    $jp.replaceWith(newFunction);
  }

  check(node: Joinpoint): AnalyserResult<Call> | undefined {
    if (!(node instanceof Call)) {
      return;
    }

    if (node.name !== "strcpy") {
      return;
    }

    return new CheckResult(
      this.name,
      node,
      this.advice,
      new Fix(node, ($jp: Call) => {
        StrcpyChecker.fixAction($jp);
      })
    );
  }
}
