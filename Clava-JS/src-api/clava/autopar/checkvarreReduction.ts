/**************************************************************
 *
 *                       checkvarreReduction
 *
 **************************************************************/

import Query from "@specs-feup/lara/api/weaver/Query.js";
import { BinaryOp, Body, Expression, Loop, UnaryOp } from "../../Joinpoints.js";
import GetLoopIndex from "./GetLoopIndex.js";
import SearchStruct from "./SearchStruct.js";
import { orderedVarrefs3 } from "./orderedVarrefs3.js";
import { LoopOmpAttributes } from "./checkForOpenMPCanonicalForm.js";

export default function checkvarreReduction($ForStmt: Loop) {
    let Reduction: string[] = [];
    // type 1 : x++, ++x, x--, --x
    // type 2 : x binaryOp= expr , binaryOp={+, *, -, &, ^ ,|}
    // type 3 : x = x binaryOp expr,		binaryOp={+, *, -, &, ^ ,|, &&, ||}
    //			x = expr binaryOp x (except for subtraction)
    // x is not referenced in exp
    // expr has scalar type (no array, objects etc)

    const loopindex = GetLoopIndex($ForStmt);
    const candidateVarlist = SearchStruct(
        LoopOmpAttributes[loopindex].varAccess,
        {
            varTypeAccess: "varref",
            use: "RW",
            isInsideLoopHeader: false,
            usedInClause: false,
        }
    );

    const exprlines = [];

    for (let i = 0; i < candidateVarlist.length; i++)
        if (
            candidateVarlist[i].varUsage.length == 1 || // for type 1 (or type 2,3)
            (candidateVarlist[i].varUsage.length == 2 &&
                candidateVarlist[i].varUsage[0].line ==
                    candidateVarlist[i].varUsage[1].line) // for type 2,3 , at same line
        ) {
            exprlines.push(candidateVarlist[i].varUsage[0].line);
        } else {
            candidateVarlist.splice(i, 1); // remove from candidates
            i--;
        }

    for (const $expr of Query.searchFrom($ForStmt, Body).search(Expression)) {
        if (exprlines.indexOf($expr.line) !== -1) {
            let candidateVar = null;
            for (const element of candidateVarlist)
                if (element.varUsage[0].line === $expr.line) {
                    candidateVar = element;
                    break;
                }

            const o = retReductionOpVar($expr, candidateVar);
            Reduction = Reduction.concat(o);
            exprlines.splice(exprlines.indexOf($expr.line), 1);
        }
    }

    return Reduction;
}

/**************************************************************
 *
 *                       retReductionOpVar
 *
 **************************************************************/
function retReductionOpVar($expr: Expression, candidateVar) {
    const Reduction: string[] = [];

    const exprvarrefset = orderedVarrefs3($expr);
    let candidateVarOp = [];
    let otherVarUsednumber = 0; // number of other varref in expr
    for (const element of exprvarrefset) {
        if (element.name === candidateVar.name) {
            if (
                element.getAncestor("expression") !== undefined &&
                element.getAncestor("expression").astName === "ParenExpr"
            ) {
                candidateVarOp = [];
                break; // candidateVar should not be in any ParenExpr
            }

            let op = null;
            if (element.getAncestor("unaryOp") !== undefined)
                op = (element.getAncestor("unaryOp") as UnaryOp).kind;
            else if (element.getAncestor("binaryOp") !== undefined)
                op =
                    (element.getAncestor("binaryOp") as BinaryOp).kind +
                    ((element.getAncestor("binaryOp") as BinaryOp).kind !==
                        "assign" &&
                    (element.getAncestor("binaryOp") as BinaryOp)
                        .isAssignment === true
                        ? ""
                        : "");
            if (
                op === "sub" &&
                typeof (element.getAncestor("binaryOp") as BinaryOp).right.vardecl !==
                    "undefined" &&
                (element.getAncestor("binaryOp") as BinaryOp).right.vardecl.name ===
                    candidateVar.name
            ) {
                candidateVarOp = [];
                break; // x = expr - x  not acceptable
            }

            candidateVarOp.push(op);
        } else {
            otherVarUsednumber++;
        }
    }

    let op = "";
    if (candidateVarOp.length == 1) {
        op = candidateVarOp[0];
    } else if (
        candidateVarOp.length == 2 &&
        candidateVarOp[1] == "assign" &&
        ([
            "add_assign",
            "sub_assign",
            "mul_assign",
            "and_assign",
            "or_assign",
            "xor_assign",
        ].indexOf(candidateVarOp[0]) !== -1 ||
            ["add", "mul", "sub", "and", "or", "l_and", "l_or"].indexOf(
                candidateVarOp[0]
            ) !== -1)
    ) {
        op = candidateVarOp[0];
    } else {
        return Reduction;
    }

    let findOpflag = true;
    if (["pre_dec", "post_dec", "sub", "sub_assign"].indexOf(op) !== -1)
        Reduction.push("reduction(- : " + candidateVar.name + ")");
    else if (["pre_inc", "post_inc", "add", "add_assign"].indexOf(op) !== -1)
        Reduction.push("reduction(+ : " + candidateVar.name + ")");
    else if (["mul", "mul_assign"].indexOf(op) !== -1)
        Reduction.push("reduction(* : " + candidateVar.name + ")");
    else if (["and", "and_assign"].indexOf(op) !== -1)
        Reduction.push("reduction(& : " + candidateVar.name + ")");
    else if (["xor", "xor_assign"].indexOf(op) !== -1)
        Reduction.push("reduction(^ : " + candidateVar.name + ")");
    else if (["or", "or_assign"].indexOf(op) !== -1)
        Reduction.push("reduction(| : " + candidateVar.name + ")");
    else if (["l_and"].indexOf(op) !== -1)
        Reduction.push("reduction(&& : " + candidateVar.name + ")");
    else if (["l_or"].indexOf(op) !== -1)
        Reduction.push("reduction(|| : " + candidateVar.name + ")");
    else findOpflag = false;

    if (findOpflag === true) candidateVar.usedInClause = true;

    return Reduction;
}
