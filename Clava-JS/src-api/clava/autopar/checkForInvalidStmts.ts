import Query from "@specs-feup/lara/api/weaver/Query.js";
import { Call, Loop, Statement } from "../../Joinpoints.js";

/**************************************************************
 *
 *                      checkForInvalidStmts
 *
 **************************************************************/
export default function checkForInvalidStmts($ForStmt: Loop) {
    // Initialize output
    const InvalidStmts: string[] = [];

    // check for [exit] at any subregion
    for (const $call of Query.searchFrom($ForStmt, Call, "exit")) {
        InvalidStmts.push($call.name + "#" + $call.line);
        return;
    }

    // check for [break,return]
    for (const $stmt of Query.searchFrom($ForStmt.body, Statement, {
        astName: (astName) =>
            astName === "ReturnStmt" || astName === "BreakStmt",
    })) {
        if (
            $stmt.astName === "ReturnStmt" ||
            ($stmt.astName === "BreakStmt" &&
                $stmt.getAstAncestor("ForStmt").line === $ForStmt.line)
        ) {
            InvalidStmts.push($stmt.astName + "#" + $stmt.line);
            return;
        }
    }

    return InvalidStmts;
}
