import Query from "@specs-feup/lara/api/weaver/Query.js";
import { Call, Loop } from "../../Joinpoints.js";
import { safefunctionCallslist } from "./SafeFunctionCalls.js";

/**************************************************************
 *
 *                      checkForFunctionCalls
 *
 **************************************************************/
export default function checkForFunctionCalls($ForStmt: Loop) {
    const FunctionCalls = [];

    // check for calling functions

    const userSafeFunctions = ($ForStmt.data as any).safeFunctions;

    for (const $call of Query.searchFrom($ForStmt, Call, {
        astName: "CallExpr",
    })) {
        const isSafe =
            safefunctionCallslist.indexOf($call.name) !== -1 ||
            (userSafeFunctions !== undefined &&
                userSafeFunctions.indexOf($call.name) !== -1);

        if (!isSafe) {
            FunctionCalls.push(
                $call.name + "#" + $call.line + "{" + $call.code + "}"
            );
        }
    }

    return FunctionCalls;
}
