laraImport("clava.ClavaJoinPoints");
laraImport("clava.code.GlobalVariable");

laraImport("weaver.Query");

laraImport("lara.util.StringSet");
laraImport("lara.util.PrintOnce");
laraImport("lara.code.Logger");

laraImport("lara.Strings");

/**
 * Instruments an application so that it counts total operations in a region of code.
 */
class OpsCounter {
  /**
   * @param {function} [filterFunction=undefined] - Function that receives an $op. If returns false, $op will not be counted.
   */
  constructor(filterFunction) {
    this._counters = {};
    this._$counterType = ClavaJoinPoints.builtinType("long long");
    this._instrumentedFunctions = new StringSet();
    this._filterFunction = filterFunction;
    if (filterFunction !== undefined) {
      println("OpsCounter: filter function set");
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

  instrument($region) {
    const $function = $region.instanceOf("function")
      ? $region
      : $region.ancestor("function");

    if ($function === undefined) {
      PrintOnce.message(
        `OpsCounter.instrument: Could not find function corresponding to the region ${$region.location}`
      );
      return;
    }

    // Check if it is already instrumented
    if (this._instrumentedFunctions.has($function.jpId)) {
      return;
    }

    this._instrumentedFunctions.add($function.jpId);

    println("OpsCounter.instrument: Instrumenting  function " + $function.jpId);

    // Apply to all ops found in the region
    for (const $op of Query.searchFrom($region, "op")) {
      this._countOp($op);
    }

    // Call function recursively when function calls are found
    for (const $call of Query.searchFrom($region, "call")) {
      const $funcDef = $call.definition;

      if ($funcDef === undefined) {
        continue;
      }

      this.instrument($funcDef);
    }
  }

  _countOp($op) {
    // If not a valid op, return
    if (!this._isValidOp($op)) {
      return;
    }

    println(`Op (${$op.kind}): ${$op.code}`);

    // Always add to ops counter
    const opsCounter = this._getCounter("ops", "");
    const opsCounterStmt = ClavaJoinPoints.stmtLiteral(
      `${opsCounter.getRef($op).code}++;`
    );
    $op.insertBefore(opsCounterStmt);

    // Calculate type and bitwidth
    const $builtinType = this._toBuiltinType($op.type);
    const counterType = this._getCounterType($builtinType);
    const bitwidth =
      $builtinType !== undefined ? $builtinType.bitWidth($op) : undefined;

    // Get counter
    const counter = this._getCounter(counterType, bitwidth);

    // Add to corresponding counter type
    const counterStmt = ClavaJoinPoints.stmtLiteral(
      `${counter.getRef($op).code}++;`
    );
    $op.insertBefore(counterStmt);
  }

  _getCounter(counterType, bitwidth) {
    const counterName = `${this._getCounterPrefix(
      counterType,
      bitwidth
    )}_counter`;

    // Check if counter exists
    const counter = this._counters[counterName];

    if (counter === undefined) {
      counter = new GlobalVariable(counterName, this._$counterType, "0");
      this._counters[counterName] = counter;
    }

    return counter;
  }

  _getCounterPrefix(counterType, bitwidth) {
    // If counterType is undefined, return unknown, without looking at the bitwidth
    if (counterType === undefined) {
      return "unknown";
    }

    let counterPrefix = counterType;

    const bitwidthString = bitwidth?.toString() ?? "unknown";

    if (!Strings.isEmpty(bitwidthString)) {
      counterPrefix += "_";
    }

    counterPrefix += bitwidthString;

    return counterPrefix;
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
        `OpsCounter: could not determine if builtinType ${$builtinType.kind} is integer or float`
      );
      return undefined;
    }
  }

  _toBuiltinType($type) {
    if (!$type.instanceOf("builtinType")) {
      PrintOnce.message(
        `OpsCounter: could not determine builtinType of ${$type.joinPointType}`
      );
      return undefined;
    }

    return $type;
  }

  /**
   * Adds code that prints the operation counting report.
   */
  log($insertionPoint) {
    const logger = new Logger();
    for (const counterName in this._counters) {
      const counter = this._counters[counterName];
      logger
        .text(`${counterName}: `)
        .longLong(counter.getRef($insertionPoint).code)
        .ln();
    }

    logger.log($insertionPoint);
  }

  _isValidOp($op) {
    return (
      OpsCounter._validOps.has($op.kind) &&
      // Ignore operations inside loop headers
      !$op.isInsideLoopHeader &&
      // If there's a filter, apply it
      (this._filterFunction === undefined || this._filterFunction($op))
    );
  }
}
