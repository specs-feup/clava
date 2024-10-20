import { printObject } from "@specs-feup/lara/api/core/output.js";
import Add_msgError from "./Add_msgError.js";
import SearchStruct from "./SearchStruct.js";
import GetLoopIndex from "./GetLoopIndex.js";
import { Loop } from "../../Joinpoints.js";
import { LoopOmpAttributes } from "./checkForOpenMPCanonicalForm.js";

/**************************************************************
 *
 *                       SetMemberAccessOpenMPscoping
 *
 **************************************************************/
export default function SetMemberAccessOpenMPscoping($ForStmt: Loop) {
    const loopindex = GetLoopIndex($ForStmt);
    if (LoopOmpAttributes[loopindex].msgError?.length !== 0) return;

    const varreflist = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {
        varTypeAccess: "memberAccess",
    });

    printObject(
        varreflist,
        "aspectdef SetMemberAccessOpenMPscoping : varAccess for For#" +
            $ForStmt.line
    );

    for (const element of varreflist) {
        const varObj = element;
        if (
            varObj.hasDescendantOfArrayAccess === false &&
            varObj.usedInClause === false &&
            varObj.nextUse !== "R" &&
            (varObj.use === "WR" || varObj.use === "W")
        ) {
            varObj.usedInClause = true;
        }
    }

    for (const element of varreflist) {
        if (element.usedInClause === false) {
            Add_msgError(
                LoopOmpAttributes,
                $ForStmt,
                "Variable " +
                    element.name +
                    " could not be categorized into any OpenMP Variable Scope"
            );
        }
    }

    printObject(
        varreflist,
        "aspectdef SetMemberAccessOpenMPscoping : varAccess for For#" +
            $ForStmt.line
    );
}
