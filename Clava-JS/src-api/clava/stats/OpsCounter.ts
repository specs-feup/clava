import ClavaJoinPoints from "../ClavaJoinPoints.js";
import GlobalVariable from "../code/GlobalVariable.js";
import Query from "lara-js/api/weaver/Query.js";
import PrintOnce from "lara-js/api/lara/util/PrintOnce.js";
import Logger from "../../lara/code/Logger.js";
import {
  BuiltinType,
  Call,
  FunctionJp,
  Joinpoint,
  Op,
  Type,
} from "../../Joinpoints.js";

/**
 * Instruments an application so that it counts total operations in a region of code.
 *
 * @param filterFunction - Function that receives an $op. If returns false, $op will not be counted.
 */
export default class OpsCounter {
  // Whitelist of ops
  private static validOps: Set<string> = new Set([
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
    "pre_dec",
  ]);

  private counters: Map<string, GlobalVariable> = new Map();
  private $counterType = ClavaJoinPoints.builtinType("long long");
  private instrumentedFunctions: Set<string> = new Set();
  private filterFunction: (op: Op) => boolean;

  constructor(
    filterFunction: (op: Op) => boolean = ($op: Op) => !$op.isInsideLoopHeader
  ) {
    this.filterFunction = filterFunction;
  }

  instrument($region: Joinpoint) {
    const $function =
      $region instanceof FunctionJp
        ? $region
        : ($region.getAncestor("function") as FunctionJp | undefined);

    if ($function === undefined) {
      PrintOnce.message(
        `OpsCounter.instrument: Could not find function corresponding to the region ${$region.location}`
      );
      return;
    }

    // Check if it is already instrumented
    if (this.instrumentedFunctions.has($function.jpId)) {
      return;
    }

    this.instrumentedFunctions.add($function.jpId);

    console.log(
      `OpsCounter.instrument: Instrumenting  function ${$function.jpId}`
    );

    // Apply to all ops found in the region
    for (const $op of Query.searchFrom($region, "op")) {
      this.countOp($op as Op);
    }

    // Call function recursively when function calls are found
    for (const $call of Query.searchFrom($region, "call")) {
      const $funcDef = ($call as Call).definition;

      if ($funcDef === undefined) {
        continue;
      }

      this.instrument($funcDef);
    }
  }

  private countOp($op: Op) {
    // If not a valid op, return
    if (!this.isValidOp($op)) {
      return;
    }

    console.log(`Op (${$op.kind}): ${$op.code}`);

    // Always add to ops counter
    const opsCounter = this.getCounter("ops", "");
    const opsCounterStmt = ClavaJoinPoints.stmtLiteral(
      opsCounter.getRef($op).code + "++;"
    );
    $op.insertBefore(opsCounterStmt);

    // Calculate type and bitwidth
    const $builtinType = this.toBuiltinType($op.type);
    const counterType = this.getCounterType($builtinType);
    const bitwidth: string | undefined =
      $builtinType !== undefined ? String($op.bitWidth) : undefined;

    // Get counter
    const counter = this.getCounter(counterType, bitwidth);

    // Add to corresponding counter type
    const counterStmt = ClavaJoinPoints.stmtLiteral(
      counter.getRef($op).code + "++;"
    );
    $op.insertBefore(counterStmt);
  }

  private getCounter(counterType?: string, bitwidth?: string) {
    const counterName =
      this.getCounterPrefix(counterType, bitwidth) + "_counter";

    // Check if counter exists
    let counter = this.counters.get(counterName);

    if (counter === undefined) {
      counter = new GlobalVariable(counterName, this.$counterType, "0");
      this.counters.set(counterName, counter);
    }

    return counter;
  }

  private getCounterPrefix(counterType?: string, bitwidth: string = "unknown") {
    // If counterType is undefined, return unknown, without looking at the bitwidth
    if (counterType === undefined) {
      return "unknown";
    }

    let counterPrefix: string = counterType;

    if (bitwidth !== "") {
      counterPrefix += "_";
    }

    counterPrefix += bitwidth;

    return counterPrefix;
  }

  private getCounterType($builtinType?: BuiltinType) {
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

  private toBuiltinType($type: Type) {
    if ($type instanceof BuiltinType) {
      return $type;
    }

    PrintOnce.message(
      `OpsCounter: could not determine builtinType of ${$type.joinPointType}`
    );
    return undefined;
  }

  /**
   * Adds code that prints the operation counting report.
   */
  log($insertionPoint: Joinpoint) {
    const logger = new Logger();
    for (const entry of this.counters.entries()) {
      const counterName = entry[0];
      const counter = entry[1];
      logger
        .text(counterName + ": ")
        .longLong(counter.getRef($insertionPoint).code)
        .ln();
    }

    logger.log($insertionPoint);
  }

  private isValidOp($op: Op) {
    const isValid = OpsCounter.validOps.has($op.kind);

    if (!isValid) {
      return false;
    }

    if (!this.filterFunction($op)) {
      return false;
    }

    return true;
  }
}
