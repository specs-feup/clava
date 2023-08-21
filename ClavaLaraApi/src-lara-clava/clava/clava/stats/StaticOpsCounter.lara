import clava.ClavaJoinPoints;
import clava.code.GlobalVariable;
import clava.stats.OpsBlock;

import weaver.Query;

import lara.util.StringSet;
import lara.util.PrintOnce;
import lara.code.Logger;

import lara.Strings;


function analyseIterationsExpr($expr, $source) {
    var result = {};

    for (var $varref of Query.searchFromInclusive($expr, "varref")) {
        if (result[$varref.name] !== undefined) {
            continue;
        }
        if ($varref.decl.instanceOf("param")) {
            continue;
        }
        var $lastWrite = getLastWrite($source, $varref.decl);
        result[$varref.name] = $lastWrite;
    }
    return result;
}

function getLastWrite($currentJp, $vardecl) {
    if ($currentJp === undefined) {
        println("Could not find declaration");
        return undefined;
    }
    // println("getVarrefUses: " + $currentJp.code);
    //println("Type: " + $currentJp.joinPointType);
    // Get siblings on the left
    var siblLeft = $currentJp.siblingsLeft;
    // Go back until the variable declaration/parameter is found
    for (var i = siblLeft.length - 1; i >= 0; i--) {
        var sibl = siblLeft[i];
        // For each sibling, find write references to the variable
        var refs = sibl
            .getDescendantsAndSelf("varref")
            .filter((varref) => varref.name === $vardecl.name);
        for (var $ref of refs) {
            // Ignore
            if ($ref.use === "read") {
                continue;
            }
            // Not supported yet
            if ($ref.use === "readwrite") {
                println("Readwrite not supported yet");
                return undefined;
            }
            // Check if assignment
            var $refParent = $ref.parent;
            if (
                !$refParent.instanceOf("binaryOp") &&
                $refParent.kind !== "assign"
            ) {
                println("Not supported when not an assignment");
                return undefined;
            }
            return $refParent.right;
        }
        // Check vardecl
        var decls = sibl
            .getDescendantsAndSelf("vardecl")
            .filter((vardecl) => vardecl.equals($vardecl));
        for (var $decl of decls) {
            // Found decl
            if (!$decl.hasInit) {
                println(
                    "Variable declaration for " +
                        $decl.name +
                        " has no initialization"
                );
                return undefined;
            }

            return $decl.init;
        }
    }
    // Did not find declaration yet, call on parent
    return getLastWrite($currentJp.parent, $vardecl);
}

/**
 * Instruments an application so that it counts total operations in a region of code.
 *
 * @param {function} [filterFunction=undefined] - Function that receives an $op. If returns false, $op will not be counted.
 */
var StaticOpsCounter = function (filterFunction) {
    this._counters = {}; // TODO: To remove
    this._$counterType = ClavaJoinPoints.builtinType("long long"); // TODO: To remove
    // this._$counterType = ClavaJoinPoints.builtinType("unsigned long long");
    this._instrumentedFunctions = new StringSet();
    this._filterFunction = filterFunction;
    if (filterFunction !== undefined) {
        println("StaticOpsCounter: filter function set");
    }
};

// Whitelist of ops
StaticOpsCounter._validOps = new StringSet(
    "mul",
    "div",
    "rem",
    "add",
    "sub",
    "shl",
    "shr",
    "cmp",
    "and",
    "xor",
    "or",
    "l_and",
    "l_or",
    "mul_assign",
    "div_assign",
    "rem_assign",
    "add_assign",
    "sub_assign",
    "shl_assign",
    "shr_assign",
    "and_assign",
    "xor_assign",
    "or_assign",
    "post_inc",
    "post_dec",
    "pre_inc",
    "pre_dec"
);

StaticOpsCounter.prototype.count = function (
    $function,
    opsBlock,
    includeOpKind
) {
    includeOpKind = includeOpKind !== undefined ? includeOpKind : false;

    var $function = $function.instanceOf("function")
        ? $function
        : $function.getAncestor("function");

    if ($function === undefined) {
        PrintOnce.message(
            "StaticOpsCounter.count: Could not find function corresponding to the join point " +
                $function.location
        );
        return;
    }

    var functionId = $function.name + "@" + $function.location;

    // Check if it is already instrumented
    // println("ID: " + $function.location);
    if (this._instrumentedFunctions.has(functionId)) {
        // if(this._instrumentedFunctions.has($function.jpId)) {
        // Not working yet
        /*
        if(opsBlock === undefined) {
            println("Expected opsBlock to be defined!");
        } else {
            opsBlock.isRecursive = true;
        }
        */

        return;
    }

    this._instrumentedFunctions.add(functionId);

    println("StaticOpsCounter.count: Estimating ops of function " + functionId);

    // var opsBlock = new OpsBlock(functionId);
    opsBlock = opsBlock !== undefined ? opsBlock : new OpsBlock(functionId);

    // Go statement-by-statement
    var stmts = $function.body.children;

    for (var $stmt of stmts) {
        this._countOpStatic($stmt, opsBlock, includeOpKind);
    }

    return opsBlock;
};

StaticOpsCounter.prototype._countOpStatic = function (
    $stmt,
    opsBlock,
    includeOpKind
) {
    // If stmt is a loop, count new block, recursively

    if ($stmt == undefined) {
        return;
    }

    if ($stmt.instanceOf("loop")) {
        if ($stmt.kind !== "for") {
            println(
                "Ignoring loops that are not 'fors' (location " +
                    $stmt.location +
                    ") for now"
            );
            return;
        }

        var rank = $stmt.rank;
        var nestedId = opsBlock.id + " => " + rank[rank.length - 1];

        // Create block for loop
        var nestedOpsBlock = new OpsBlock(nestedId);

        this._countOpStatic($stmt.init, opsBlock, includeOpKind);
        this._countOpStatic($stmt.cond, nestedOpsBlock, includeOpKind);
        this._countOpStatic($stmt.step, nestedOpsBlock, includeOpKind);

        // Extract iterations
        var iter = $stmt.iterationsExpr;
        do {
            var replacementsMap = analyseIterationsExpr(iter, $stmt);

            for (rep in replacementsMap) {
                for (var $jp of iter.descendants) {
                    if ($jp.code === rep) {
                        $jp.replaceWith(replacementsMap[rep]);
                    }
                }
            }
        } while (Object.keys(replacementsMap).length > 0);
        nestedOpsBlock.repetitions = iter.code;

        // Add to nested blocks
        opsBlock.nestedOpsBlocks.push(nestedOpsBlock);

        // Go statement-by-statement
        var nestedStmts = $stmt.body.children;

        for (var $stmt of nestedStmts) {
            this._countOpStatic($stmt, nestedOpsBlock, includeOpKind);
        }

        return;
    }

    // If stmt is not a loop, count ops
    // Apply to all ops found in the stmt
    for (var $op of Query.searchFrom($stmt, "op")) {
        // If not a valid op, continue
        if (!this._isValidOp($op)) {
            continue;
        }

        // println("Op ("+$op.kind+"): " + $op.code);

        // Calculate type and bitwidth
        var $builtinType = this._toBuiltinType($op.type);
        var counterType = this._getCounterType($builtinType);
        var bitwidth =
            $builtinType !== undefined ? $builtinType.bitWidth($op) : undefined;

        // Increment counter
        var opsId = counterType + "-" + bitwidth;
        if (includeOpKind) {
            opsId += "-" + $op.kind;
        }
        opsBlock.add(opsId);
    }

    // Call function recursively when function calls are found
    for (var $call of Query.searchFrom($stmt, "call")) {
        var $funcDef = $call.definition;

        if ($funcDef === undefined) {
            continue;
        }
        // println("FUNC DEF: " + $funcDef.joinPointType);
        this.count($funcDef, opsBlock, includeOpKind);
    }
};

StaticOpsCounter.prototype._getCounterType = function ($builtinType) {
    if ($builtinType === undefined) {
        return undefined;
    }

    if ($builtinType.isFloat) {
        return "flops";
    } else if ($builtinType.isInteger) {
        return "iops";
    } else {
        PrintOnce.message(
            "StaticOpsCounter: could not determine if builtinType " +
                $type.kind +
                " is integer or float"
        );
        return undefined;
    }
};

StaticOpsCounter.prototype._toBuiltinType = function ($type) {
    if ($type.instanceOf("builtinType")) {
        return $type;
    }

    PrintOnce.message(
        "StaticOpsCounter: could not determine builtinType of " +
            $type.joinPointType
    );
    return undefined;
};

StaticOpsCounter.prototype._isValidOp = function ($op) {
    var isValid = StaticOpsCounter._validOps.has($op.kind);

    if (!isValid) {
        return false;
    }

    if (this._filterFunction !== undefined) {
        if (!this._filterFunction($op)) {
            return false;
        }
    }

    return true;
};
