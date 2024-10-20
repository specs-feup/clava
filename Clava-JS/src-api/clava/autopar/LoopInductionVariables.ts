import Query from "@specs-feup/lara/api/weaver/Query.js";
import {
    ArrayAccess,
    BinaryOp,
    Body,
    FileJp,
    FunctionJp,
    Loop,
    Varref,
} from "../../Joinpoints.js";
import AutoParStats from "./AutoParStats.js";

/**************************************************************
 *
 *                       LoopInductionVariables
 *
 **************************************************************/

export interface VarAccess {
    line: number,
    replaceCode: string
};

export interface InductionVariable {
    varName: string,
    varAccess: VarAccess[]
}

export default function LoopInductionVariables() {
    const inductionVariables: Record<string, InductionVariable> = {};
    const inductionVariablesName = [];
    const functionNames = [];

    for (const chain of Query.search(FileJp)
        .search(FunctionJp)
        .search(Loop, { nestedLevel: 0 })
        .search(BinaryOp, { kind: "assign" })
        .chain()) {
        const $function = chain["function"]!;
        const $binaryOp = chain["binaryOp"]!;

        if ($binaryOp.left instanceof Varref) {
            const $varref = $binaryOp.left;
            const astId = $varref.vardecl.astId;

            if (inductionVariables[astId] === undefined) {
                inductionVariables[astId] = {
                    varName: $varref.name,
                    varAccess: []
                };

                inductionVariablesName.push($varref.name);

                if (functionNames.indexOf($function.name) === -1)
                    functionNames.push($function.name);
            }

            if (
                $binaryOp.isInsideLoopHeader === false &&
                $binaryOp.getAncestor("if") === undefined &&
                $binaryOp.getDescendantsAndSelf("arrayAccess").length === 0 &&
                $binaryOp.getDescendantsAndSelf("call").length === 0 &&
                $binaryOp.getDescendantsAndSelf("unaryOp").length === 0 &&
                $binaryOp.code.indexOf(">>") === -1 &&
                $binaryOp.code.indexOf("<<") === -1
            ) {
                inductionVariables[astId].varAccess.push({
                    line: $varref.line,
                    replaceCode: $binaryOp.right.code,
                });
            } else {
                inductionVariables[astId].varAccess.push({
                    line: $varref.line,
                    replaceCode: $varref.name,
                });
            }
        }
    }

    for (const $arrayAccess of Query.search(FileJp)
        .search(FunctionJp)
        .search(Loop, { nestedLevel: 0 })
        .search(Body)
        .search(ArrayAccess)) {
        const $subscript = $arrayAccess.subscript;
        if ($subscript instanceof Varref) {
            if (inductionVariables[$subscript.vardecl.astId] !== undefined) {
                let replaceCode = null;
                for (const obj of inductionVariables[$subscript.vardecl.astId]
                    .varAccess)
                    if (obj.line < $subscript.line)
                        replaceCode = obj.replaceCode;

                if (replaceCode !== null && replaceCode !== $subscript.name) {
                    const strbefor = $arrayAccess.code;
                    $subscript.insert("replace", replaceCode);
                    AutoParStats.get().incIndunctionVariableReplacements();
                }
            }
        }
    }
}
