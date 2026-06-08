import Checker from "../Checker.ts";
import CheckResult from "../CheckResult.ts";
import Fix from "../Fix.ts";
import { Joinpoint, Call } from "../../../Joinpoints.ts";
import AnalyserResult from "../AnalyserResult.ts";
import ClavaJoinPoints from "../../ClavaJoinPoints.ts";

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

  check(node: Joinpoint): AnalyserResult | undefined {
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
      new Fix(node, ($jp: Joinpoint) => {
        if ($jp instanceof Call) {
          StrcpyChecker.fixAction($jp);
        } else {
          throw new Error("Invalid joinpoint type");
        }
      })
    );
  }
}
