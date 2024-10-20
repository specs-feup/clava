/**************************************************************
 *
 *                   additionalConditionsCheck
 *
 **************************************************************/
import Query from "@specs-feup/lara/api/weaver/Query.js";
import Add_msgError from "./Add_msgError.js";
import checkForFunctionCalls from "./checkForFunctionCalls.js";
import GetLoopIndex from "./GetLoopIndex.js";
import { ArrayAccess, Loop } from "../../Joinpoints.js";
import { LoopOmpAttributes } from "./checkForOpenMPCanonicalForm.js";

export default function additionalConditionsCheck($ForStmt: Loop) {
    const loopindex = GetLoopIndex($ForStmt);
    if (LoopOmpAttributes[loopindex].msgError?.length !== 0) return;

    let conterNum = null;

    try {
        conterNum = eval($ForStmt.endValue + "-" + $ForStmt.initValue);
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (e) { /* empty */ }

    if (
        !Number.isNaN($ForStmt.endValue) &&
        conterNum !== null &&
        Math.abs(Number(conterNum)) < 50
    ) {
        Add_msgError(
            LoopOmpAttributes,
            $ForStmt,
            " Loop Iteration number is too low"
        );
        return;
    }

    const o = checkForFunctionCalls($ForStmt);

    if (o.length > 0) {
        Add_msgError(
            LoopOmpAttributes,
            $ForStmt,
            "Variables Access as passed arguments Can not be traced inside of function calls : \n" +
                o.join("\n")
        );
        return;
    }

    // check non-affine array subscript A[B[j]].... ????
    //------------------------------------------------------------
    // check for usage of array access as the subscript A[B[.]]
    //------------------------------------------------------------
    for (const $arrayAccess of Query.searchFrom($ForStmt,
        ArrayAccess,
        { use: "write" }
    )) {
        const $subscripts = $arrayAccess.subscript;
        for (const $subscript of $subscripts) {
            if ($subscript.getDescendantsAndSelf("arrayAccess").length > 0) {
                Add_msgError(
                    LoopOmpAttributes,
                    $ForStmt,
                    " Array access " +
                        $arrayAccess.code +
                        " which is used for writing has subscript of arrayType " +
                        $subscript.code
                );
                return;
            }
        }
    }
}
