import Query from "@specs-feup/lara/api/weaver/Query.js";
import { BinaryOp, Expression, Loop, UnaryOp, Varref } from "../../Joinpoints.js";
import GetLoopIndex from "./GetLoopIndex.js";
import { orderedVarrefs3 } from "./orderedVarrefs3.js";
import SearchStruct from "./SearchStruct.js";
import { LoopOmpAttributes } from "./checkForOpenMPCanonicalForm.js";

/**************************************************************
* 
*                       FindReductionArrays
* 
**************************************************************/
export default function FindReductionArrays($ForStmt: Loop, candidateArrayName, exprline: number, isdependentInnerloop: boolean, isdependentCurrentloop: boolean, isdependentOuterloop: boolean) {
    let reduction: string[] = [];

    let isReductionArrays = false;
    
    const check1 = (isdependentInnerloop === true) && (isdependentCurrentloop === false) && (isdependentOuterloop === false);
    const check2 = (isdependentInnerloop === false) && (isdependentCurrentloop === false) && (isdependentOuterloop === true);


    if (
        check1 === false && check2 === false  
        )
        return;

    // type 1 : x++, ++x, x--, --x
    // type 2 : x binaryOp= expr , binaryOp={+, *, -, &, ^ ,|}
    // type 3 : x = x binaryOp expr,		binaryOp={+, *, -, &, ^ ,|, &&, ||}
    //			x = expr binaryOp x (except for subtraction)	
    // x is not referenced in exp
    // expr has scalar type (no array, objects etc)

    
    const loopindex = GetLoopIndex($ForStmt);
    const loopControlVarname = LoopOmpAttributes[loopindex].loopControlVarname;


    const candidateArray = SearchStruct(LoopOmpAttributes[loopindex].varAccess, {usedInClause : false, name : candidateArrayName})[0];

    const count = (candidateArray.useT.match(/RW/g) || []);

    if (count.length >1 || count.length === 0)
        return;


    // check for similar dependency of all occurrence for candidateArray
    for(let i = 1; i < candidateArray.varUsage.length; i++)
        if (
            candidateArray.varUsage[i].IsdependentCurrentloop !== candidateArray.varUsage[0].IsdependentCurrentloop ||
            candidateArray.varUsage[i].IsdependentInnerloop !== candidateArray.varUsage[0].IsdependentInnerloop ||
            candidateArray.varUsage[i].IsdependentOuterloop !== candidateArray.varUsage[0].IsdependentOuterloop
            )
            return;


    for (const $expr of Query.searchFrom($ForStmt.body, Expression, {line: exprline})) {
        reduction = retReductionOpArray($expr, candidateArray, isdependentInnerloop, isdependentCurrentloop, isdependentOuterloop);
        break #$ForStmt;
    
    }

    if (reduction.length > 0)
    {
        LoopOmpAttributes[loopindex].Reduction = LoopOmpAttributes[loopindex].Reduction?.concat(reduction);
        LoopOmpAttributes[loopindex].Reduction_listVars?.push(candidateArrayName);
        isReductionArrays = true;
    }

    return isReductionArrays;
}	



/**************************************************************
* 
*                       retReductionOpArray
* 
**************************************************************/
function retReductionOpArray($expr: Expression, candidateVar, isdependentInnerloop: boolean, isdependentCurrentloop: boolean, isdependentOuterloop: boolean) {
    
    const reduction: string[] = [];
    let candidateVarUse = null;

    const exprvarrefset = orderedVarrefs3($expr);
    let candidateVarOp = [];
    const otherVarUsednumber = 0; // number of other varref in expr
    for(let j = 0; j < exprvarrefset.length; j++)
    {	

        // Fix: Although the array is called exprvarrefset, not all elements are varref (arrayAccess can appear)
        if (exprvarrefset[j].name === candidateVar.name && exprvarrefset[j].instanceOf("varref"))
        {
            if (exprvarrefset[j].useExpr.use.indexOf('write') !== -1)
                candidateVarUse = exprvarrefset[j].useExpr.code;

            if (exprvarrefset[j].getAncestor('expression') !== undefined && exprvarrefset[j].getAncestor('expression').astName === 'ParenExpr')
            {
                candidateVarOp = [];
                break; // candidateVar should not be in any ParenExpr
            }

            let op = null;
            if ((exprvarrefset[j].getAncestor('unaryOp') as UnaryOp) !== undefined)
                op = (exprvarrefset[j].getAncestor('unaryOp') as UnaryOp).kind;
            else if (exprvarrefset[j].getAncestor('binaryOp') !== undefined)
                op = (exprvarrefset[j].getAncestor('binaryOp')as BinaryOp).kind 
                // TODO: Is this dead code?
                + (( (exprvarrefset[j].getAncestor('binaryOp') as BinaryOp).kind !== 'assign' && (exprvarrefset[j].getAncestor('binaryOp') as BinaryOp).isAssignment === true) ? '' :'');										
            if (op === 'sub' &&
                typeof((exprvarrefset[j].getAncestor('binaryOp')as BinaryOp).right.vardecl) !== 'undefined' && 
                (exprvarrefset[j].getAncestor('binaryOp')as BinaryOp).right.vardecl.name === candidateVar.name
                )
            {
                candidateVarOp = [];
                break; // x = expr - x  not acceptable
            }

            candidateVarOp.push(op);
        }
    }
    
    let op: string;
    if (candidateVarOp.length == 1)
    {
        op = candidateVarOp[0]!;
    }
    else if(candidateVarOp.length == 2 && 
            candidateVarOp[1] == 'assign' &&
            (
                // TODO: Remove compound operators (add_assign), since there is an assign?
                ['add_assign', 'sub_assign', 'mul_assign','and_assign', 'or_assign' , 'xor_assign'].indexOf(candidateVarOp[0]!) !== -1 ||
                ['add', 'mul', 'sub', 'and', 'or', 'l_and' , 'l_or'].indexOf(candidateVarOp[0]!) !== -1
            )
        )
    {
        op = candidateVarOp[0]!;
    }
    else
    {
        return reduction;
    }


    let arraysizeStr = null;

    if (candidateVar.ArraySize === null)
        return reduction;
    

    if (
        isdependentInnerloop === true
        )
        arraysizeStr = candidateVar.name + candidateVar.ArraySize.allReplace({'\\[':'[:'});
    else if (isdependentInnerloop === false)		
    {
        arraysizeStr = candidateVarUse;
    }	
    // TODO: Dead code?
    else
    {
        return reduction;
    }


    let findOpflag = true;
    if ( ['pre_dec', 'post_dec', 'sub', 'sub_assign'].indexOf(op) !== -1 )
        reduction.push('reduction (-:' +  arraysizeStr + ')');
    else if ( ['pre_inc', 'post_inc', 'add', 'add_assign'].indexOf(op) !== -1 )
        reduction.push('reduction (+:' + arraysizeStr + ')');
    else if ( ['mul' , 'mul_assign'].indexOf(op) !== -1 )
        reduction.push('reduction (*:' + arraysizeStr + ')');
    else if ( ['and' , 'and_assign'].indexOf(op) !== -1 )
        reduction.push('reduction (&:' + arraysizeStr + ')');
    else if ( ['xor' , 'xor_assign'].indexOf(op) !== -1 )
        reduction.push('reduction (^:' + arraysizeStr + ')');
    else if ( ['or' , 'or_assign'].indexOf(op) !== -1 )
        reduction.push('reduction (|:' + arraysizeStr + ')');
    else if ( ['l_and'].indexOf(op) !== -1 )
        reduction.push('reduction (&&:' + arraysizeStr + ')');
    else if ( ['l_or'].indexOf(op) !== -1 )
        reduction.push('reduction (||:' + arraysizeStr + ')');
    else
        findOpflag = false;

    if (findOpflag === true)
        candidateVar.usedInClause = true;

    return reduction;
}