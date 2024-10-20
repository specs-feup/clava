import { Loop } from "../../Joinpoints.js";
import GetLoopIndex from "./GetLoopIndex.js";
import { LoopOmpAttribute } from "./checkForOpenMPCanonicalForm.js";

/**************************************************************
 *
 *                       Add_msgError
 *
 **************************************************************/
export default function Add_msgError(
    LoopOmpAttributes: Record<string, LoopOmpAttribute>,
    $ForStmt: Loop,
    msgError: string | any
) {
    const loopindex = GetLoopIndex($ForStmt);

    if (LoopOmpAttributes[loopindex].msgError === undefined)
        LoopOmpAttributes[loopindex].msgError = [];

    if (typeof msgError === "string") {
        LoopOmpAttributes[loopindex].msgError.push(msgError);
    } else {
        LoopOmpAttributes[loopindex].msgError =
            LoopOmpAttributes[loopindex].msgError.concat(msgError);
    }
}
