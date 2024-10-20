/**************************************************************
 *
 *                       SetVarrefOpenMPscoping
 *
 **************************************************************/

import Query from "@specs-feup/lara/api/weaver/Query.js";
import { Loop, Vardecl, Varref } from "../../Joinpoints.js";
import Add_msgError from "./Add_msgError.js";
import checkvarreReduction from "./checkvarreReduction.js";
import GetLoopIndex from "./GetLoopIndex.js";
import SearchStruct from "./SearchStruct.js";
import { LoopOmpAttributes } from "./checkForOpenMPCanonicalForm.js";

export default function SetVarrefOpenMPscoping($ForStmt: Loop) {
    const loopindex = GetLoopIndex($ForStmt);
    if (LoopOmpAttributes[loopindex].msgError?.length !== 0) {
        return;
    }

    const varreflist = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {
        varTypeAccess: "varref",
    });

    for (const element of varreflist) {
        if (element.declpos === "inside") {
            element.usedInClause = true;
        }
    }

    //------------------------------------------------------------
    //	add all sub loop control vars to private list
    //------------------------------------------------------------
    let loopsControlVarname = [];
    loopsControlVarname.push(LoopOmpAttributes[loopindex].loopControlVarname);
    if (LoopOmpAttributes[loopindex].innerloopsControlVarname !== undefined) {
        loopsControlVarname = loopsControlVarname.concat(
            LoopOmpAttributes[loopindex].innerloopsControlVarname
        );
    }

    for (const element of loopsControlVarname) {
        if (
            LoopOmpAttributes[loopindex].privateVars?.indexOf(
                element
            ) === -1
        )
            LoopOmpAttributes[loopindex].privateVars.push(
                element
            );
    }

    //------------------------------------------------------------
    // add all vars which [use==WR or use==W] and [nextUse==null] and are local function var
    //------------------------------------------------------------
    for (const element of varreflist) {
        const varObj = element;
        if (
            varObj.usedInClause === false &&
            varObj.nextUse !== "R" &&
            (varObj.use === "WR" || varObj.use === "W")
        ) {
            if (
                LoopOmpAttributes[loopindex].privateVars?.indexOf(
                    varObj.name
                ) === -1
            )
                LoopOmpAttributes[loopindex].privateVars.push(varObj.name);
            varObj.usedInClause = true;
        }
    }

    //------------------------------------------------------------
    // find Reduction for variable access
    //------------------------------------------------------------
    const o = checkvarreReduction($ForStmt);
    LoopOmpAttributes[loopindex].Reduction =
        LoopOmpAttributes[loopindex].Reduction?.concat(o);

    //------------------------------------------------------------
    // 						Fill firstprivate and lastprivate
    //	Restrictions :
    //		- A variable that is part of another variable (as an array or structure element) cannot appear in a private clause
    //		- If a list item appears in both firstprivate and lastprivate clauses, the update required for lastprivate occurs after all the initializations for firstprivate
    //		- A list item that appears in a reduction clause of a parallel construct must not appear in a firstprivate clause
    //		- A variable that appears in a firstprivate clause must not have an incomplete C/C++ type or be a reference to an incomplete type
    //------------------------------------------------------------
    for (const element of varreflist) {
        if (
            element.usedInClause === false &&
            element.declpos !== "inside"
        ) {
            if (
                element.use === "R" &&
                LoopOmpAttributes[loopindex].firstprivateVars?.indexOf(
                    element.name
                ) === -1
            ) {
                LoopOmpAttributes[loopindex].firstprivateVars.push(
                    element.name
                );
                element.usedInClause = true;
            }

            if (
                element.nextUse === "R" &&
                element.use.indexOf("W") !== -1 &&
                element.use.indexOf("RW") === -1
            ) {
                // hamid changed for EP benchmark
                LoopOmpAttributes[loopindex].lastprivateVars?.push(
                    element.name
                );
                element.usedInClause = true;
            }
        }
    }

    //------------------------------------------------------------
    // simple test if all variable used in Clauses are set as usedInClause
    //------------------------------------------------------------
    for (const element of varreflist) {
        if (
            element.usedInClause === false &&
            (LoopOmpAttributes[loopindex].firstprivateVars?.indexOf(
                element.name
            ) !== -1 ||
                LoopOmpAttributes[loopindex].lastprivateVars?.indexOf(
                    element.name
                ) !== -1 ||
                LoopOmpAttributes[loopindex].privateVars?.indexOf(
                    element.name
                ) !== -1)
        ) {
            element.usedInClause = true;
        }
    }

    for (const element of varreflist) {
        if (element.usedInClause === false) {
            Add_msgError(
                LoopOmpAttributes,
                $ForStmt,
                "Variable " +
                    element.name +
                    " could not be categorized into any OpenMP Variable Scope" +
                    "use : " +
                    element.use
            );
        }
    }

    for (const $vardecl of Query.searchFrom($ForStmt, Vardecl)) {
        if (
            LoopOmpAttributes[loopindex].privateVars?.indexOf($vardecl.name) !==
            -1
        ) {
            LoopOmpAttributes[loopindex].privateVars?.splice(
                LoopOmpAttributes[loopindex].privateVars.indexOf($vardecl.name),
                1
            );
        }

        if (
            LoopOmpAttributes[loopindex].firstprivateVars?.indexOf(
                $vardecl.name
            ) !== -1
        ) {
            LoopOmpAttributes[loopindex].firstprivateVars?.splice(
                LoopOmpAttributes[loopindex].firstprivateVars.indexOf(
                    $vardecl.name
                ),
                1
            );
        }

        if (
            LoopOmpAttributes[loopindex].lastprivateVars?.indexOf(
                $vardecl.name
            ) !== -1
        ) {
            LoopOmpAttributes[loopindex].lastprivateVars?.splice(
                LoopOmpAttributes[loopindex].lastprivateVars.indexOf(
                    $vardecl.name
                ),
                1
            );
        }
    }

    for (const $varref of Query.searchFrom($ForStmt, Varref, {
        type: (type) => type.isPointer === true,
    })) {
        if (
            LoopOmpAttributes[loopindex].privateVars?.indexOf($varref.name) !==
            -1
        ) {
            LoopOmpAttributes[loopindex].privateVars?.splice(
                LoopOmpAttributes[loopindex].privateVars.indexOf($varref.name),
                1
            );
        }

        if (
            LoopOmpAttributes[loopindex].firstprivateVars?.indexOf(
                $varref.name
            ) !== -1
        ) {
            LoopOmpAttributes[loopindex].firstprivateVars?.splice(
                LoopOmpAttributes[loopindex].firstprivateVars.indexOf(
                    $varref.name
                ),
                1
            );
        }

        if (
            LoopOmpAttributes[loopindex].lastprivateVars?.indexOf(
                $varref.name
            ) !== -1
        ) {
            LoopOmpAttributes[loopindex].lastprivateVars?.splice(
                LoopOmpAttributes[loopindex].lastprivateVars.indexOf(
                    $varref.name
                ),
                1
            );
        }
    }
}
