import Query from "lara-js/api/weaver/Query.js";
import { WrapperStmt } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";

export default class VitisHlsUtils {
  static activateAllDirectives(turnOn: boolean) {
    const pragmas = Query.search("wrapperStmt", {
      code: (code: string) =>
        code.includes("#pragma HLS") || code.includes("#pragma hls"),
    }).get() as WrapperStmt[] | undefined;

    if (pragmas == undefined) {
      console.log("No pragmas found");
      return;
    }

    for (const pragma of pragmas) {
      console.log(pragma.code);
      if (turnOn) {
        if (pragma.code.startsWith("//")) {
          pragma.replaceWith(
            ClavaJoinPoints.stmtLiteral(pragma.code.replace("//", ""))
          );
        }
      } else {
        pragma.replaceWith(ClavaJoinPoints.stmtLiteral("//" + pragma.code));
      }
    }
  }
}
