laraImport("clava.ClavaJoinPoints");
laraImport("clava.code.GlobalVariable");
laraImport("clava.stats.OpsBlock");

laraImport("weaver.Query");

laraImport("lara.util.StringSet");
laraImport("lara.util.PrintOnce");
laraImport("lara.code.Logger");

laraImport("lara.Strings");

function analyseIterationsExpr($expr, $source) {
  const result = {};

  for (const $varref of Query.searchFromInclusive($expr, "varref")) {
    if (result[$varref.name] !== undefined) {
      continue;
    }
    if ($varref.decl.instanceOf("param")) {
      continue;
    }
    const $lastWrite = getLastWrite($source, $varref.decl);
    result[$varref.name] = $lastWrite;
  }
  return result;
}

function getLastWrite($currentJp, $vardecl) {
  if ($currentJp === undefined) {
    println("Could not find declaration");
    return undefined;
  }

  // Get siblings on the left
  // Go back until the variable declaration/parameter is found
  for (const sibl of $currentJp.siblingsLeft.slice().reverse()) {
    // For each sibling, find write references to the variable
    const refs = sibl
      .descendantsAndSelf("varref")
      .filter((varref) => varref.name === $vardecl.name);
    for (const $ref of refs) {
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
      const $refParent = $ref.parent;
      if (!$refParent.instanceOf("binaryOp") && $refParent.kind !== "assign") {
        println("Not supported when not an assignment");
        return undefined;
      }
      return $refParent.right;
    }

    // Check vardecl
    const decls = sibl
      .descendantsAndSelf("vardecl")
      .filter((vardecl) => vardecl.equals($vardecl));
    for (const $decl of decls) {
      // Found decl
      if (!$decl.hasInit) {
        println(`Variable declaration for ${$decl.name} has no initialization`);
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
 */
class StaticOpsCounter {
  /**
   * @param {($op: op) => boolean} [filterFunction] - Function that receives an $op. If returns false, $op will not be counted.
   */
  constructor(filterFunction) {
    this._counters = {}; // TODO: To remove
    this._$counterType = ClavaJoinPoints.builtinType("long long"); // TODO: To remove
    this._instrumentedFunctions = new StringSet();
    this._filterFunction = filterFunction;
    if (filterFunction !== undefined) {
      println("StaticOpsCounter: filter function set");
    }
  }

  // Whitelist of ops
  static _validOps = new StringSet(
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

  count($function, opsBlock, includeOpKind = false) {
    const $actualFunction = $function.instanceOf("function")
      ? $actualFunction
      : $actualFunction.ancestor("function");

    if ($actualFunction === undefined) {
      PrintOnce.message(
        `StaticOpsCounter.count: Could not find function corresponding to the join point ${$actualFunction.location}`
      );
      return;
    }

    const functionId = `${$actualFunction.name}@${$actualFunction.location}`;

    // Check if it is already instrumented
    if (this._instrumentedFunctions.has(functionId)) {
      return;
    }

    this._instrumentedFunctions.add(functionId);

    println(`StaticOpsCounter.count: Estimating ops of function ${functionId}`);

    const actualOpsBlock = opsBlock ?? new OpsBlock(functionId);

    // Go statement-by-statement
    const stmts = $actualFunction.body.children;

    for (const $stmt of stmts) {
      this._countOpStatic($stmt, actualOpsBlock, includeOpKind);
    }

    return opsBlock;
  }

  _countOpStatic($stmt, opsBlock, includeOpKind) {
    // If stmt is a loop, count new block, recursively

    if ($stmt == undefined) {
      return;
    }

    if ($stmt.instanceOf("loop")) {
      if ($stmt.kind !== "for") {
        println(
          `Ignoring loops that are not 'fors' (location ${$stmt.location}) for now`
        );
        return;
      }

      const rank = $stmt.rank;
      const nestedId = `${opsBlock.id} => ${rank[rank.length - 1]}`;

      // Create block for loop
      const nestedOpsBlock = new OpsBlock(nestedId);

      this._countOpStatic($stmt.init, opsBlock, includeOpKind);
      this._countOpStatic($stmt.cond, nestedOpsBlock, includeOpKind);
      this._countOpStatic($stmt.step, nestedOpsBlock, includeOpKind);

      // Extract iterations
      const iter = $stmt.iterationsExpr;
      let replacementsMap;
      do {
        replacementsMap = analyseIterationsExpr(iter, $stmt);

        for (rep in replacementsMap) {
          for (const $jp of iter.descendants) {
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
      const nestedStmts = $stmt.body.children;

      for (const $stmt of nestedStmts) {
        this._countOpStatic($stmt, nestedOpsBlock, includeOpKind);
      }

      return;
    }

    // If stmt is not a loop, count ops
    // Apply to all ops found in the stmt
    for (const $op of Query.searchFrom($stmt, "op")) {
      // If not a valid op, continue
      if (!this._isValidOp($op)) {
        continue;
      }

      // Calculate type and bitwidth
      const $builtinType = this._toBuiltinType($op.type);
      const counterType = this._getCounterType($builtinType);
      const bitwidth = $builtinType?.bitWidth($op);

      // Increment counter
      const opsId = `${counterType}-${bitwidth}`;
      if (includeOpKind) {
        opsId += `-${$op.kind}`;
      }
      opsBlock.add(opsId);
    }

    // Call function recursively when function calls are found
    for (const $call of Query.searchFrom($stmt, "call")) {
      const $funcDef = $call.definition;

      if ($funcDef === undefined) {
        continue;
      }
      this.count($funcDef, opsBlock, includeOpKind);
    }
  }

  _getCounterType($builtinType) {
    if ($builtinType === undefined) {
      return undefined;
    }

    if ($builtinType.isFloat) {
      return "flops";
    } else if ($builtinType.isInteger) {
      return "iops";
    } else {
      PrintOnce.message(
        `StaticOpsCounter: could not determine if builtinType ${$builtinType.kind} is integer or float`
      );
      return undefined;
    }
  }

  _toBuiltinType($type) {
    if (!$type.instanceOf("builtinType")) {
      PrintOnce.message(
        `StaticOpsCounter: could not determine builtinType of ${$type.joinPointType}`
      );
      return undefined;
    }

    return $type;
  }

  _isValidOp($op) {
    return (
      StaticOpsCounter._validOps.has($op.kind) &&
      (this._filterFunction === undefined || this._filterFunction($op))
    );
  }
}
