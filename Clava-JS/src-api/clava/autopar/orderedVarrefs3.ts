import {
    ArrayAccess,
    BinaryOp,
    Expression,
    Joinpoint,
    MemberAccess,
    Statement,
    Varref,
} from "../../Joinpoints.js";

/**************************************************************
 *
 *                       orderedVarrefs3
 *
 **************************************************************/
export function orderedVarrefs3($jp: Joinpoint): Varref[] {
    let varrefs: Varref[] = [];
    if ($jp instanceof Expression || $jp instanceof Statement) {
        return orderedVarrefsBase3($jp);
    }

    for (let i = 0; i < $jp.numChildren; i++) {
        const astChild = $jp.getChild(i);
        if (astChild === undefined) {
            continue;
        }
        varrefs = varrefs.concat(orderedVarrefs3(astChild));
    }
    return varrefs;
}

function orderedVarrefsBase3($stmt: Expression | Statement): Varref[] {
    const varrefTable: Record<number, Varref[]> = {};
    const maxLevel = varrefUsageOrder3($stmt, 0, varrefTable);
    let orderedExprs: Varref[] = [];
    for (let i = maxLevel; i >= 0; i--) {
        const exprList = varrefTable[i];
        if (exprList === undefined) {
            continue;
        }
        orderedExprs = orderedExprs.concat(exprList);
    }
    return orderedExprs;
}

export function varrefUsageOrder3(
    $jp: Joinpoint,
    currentLevel: number,
    varrefTable: Record<number, Expression[]>
): number {
    if ($jp instanceof BinaryOp) {
        let leftLevel: number;
        let rightLevel: number;
        if ($jp.kind === "assign") {
            rightLevel = varrefUsageOrder3(
                $jp.getChild(1),
                currentLevel + 1,
                varrefTable
            );
            leftLevel = varrefUsageOrder3(
                $jp.getChild(0),
                currentLevel + 1,
                varrefTable
            );
            return rightLevel > leftLevel ? rightLevel : leftLevel;
        } else {
            leftLevel = varrefUsageOrder3(
                $jp.getChild(0),
                currentLevel + 1,
                varrefTable
            );
            rightLevel = varrefUsageOrder3(
                $jp.getChild(1),
                currentLevel + 1,
                varrefTable
            );

            return rightLevel > leftLevel ? rightLevel : leftLevel;
        }
    } else if ($jp instanceof Varref && $jp.isFunctionCall === false) {
        let currentVarrefs = varrefTable[currentLevel];
        if (currentVarrefs === undefined) {
            currentVarrefs = [];
            varrefTable[currentLevel] = currentVarrefs;
        }
        currentVarrefs.push($jp);
        return currentLevel;
    } else if ($jp instanceof ArrayAccess || $jp instanceof MemberAccess) {
        let maxLevel = currentLevel;
        for (let i = 0; i < $jp.numChildren; i++) {
            const lastLevel = varrefUsageOrder3(
                $jp.getChild(i),
                currentLevel,
                varrefTable
            );
            if (lastLevel > maxLevel) {
                maxLevel = lastLevel;
            }
        }

        let currentVarrefs = varrefTable[currentLevel];
        if (currentVarrefs === undefined) {
            currentVarrefs = [];
            varrefTable[currentLevel] = currentVarrefs;
        }
        currentVarrefs.push($jp);

        return maxLevel;
    } else {
        let maxLevel = currentLevel;
        for (let i = 0; i < $jp.numChildren; i++) {
            const lastLevel = varrefUsageOrder3(
                $jp.getChild(i),
                currentLevel + 1,
                varrefTable
            );
            if (lastLevel > maxLevel) {
                maxLevel = lastLevel;
            }
        }
        return maxLevel;
    }
}
