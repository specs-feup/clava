import { JSONtoFile } from "lara-js/api/core/output.js";
import { debug } from "lara-js/api/lara/core/LaraCore.js";
import Query from "lara-js/api/weaver/Query.js";
import { Call, FunctionJp, Type, Varref } from "../../Joinpoints.js";
import MemoiTarget from "./MemoiTarget.js";
import MemoiUtils from "./MemoiUtils.js";

/**
 * 		Enum with the results of the predicate test.
 */
export enum PRED {
  VALID = 1,
  INVALID = -1,
  WAITING = 0,
}

type MemoiPred = (
  $call: Call,
  processed: Record<string, PRED | undefined>,
  report: FailReport
) => PRED | undefined;

export default class MemoiAnalysis {
  /**
   * Returns array of MemoiTarget.
   */
  static findTargets(pred: MemoiPred) {
    return MemoiAnalysis.findTargetsReport(pred).targets;
  }

  /**
   * Returns MemoiTargetReport.
   */
  static findTargetsReport(pred: MemoiPred = defaultMemoiPred) {
    const targets = [];
    const report = new FailReport();

    const processed = {};

    for (const $jp of Query.search(Call)) {
      const $call = $jp as Call;

      // find function or skip
      const $func = $call.function;
      if ($func === undefined) {
        continue;
      }

      const sig = MemoiUtils.normalizeSig($func.signature);

      // if we've processed this one before (valid or not), skip
      if (isProcessed(sig, processed)) {
        continue;
      }

      // test if valid
      const valid = pred($call, processed, report);
      if (valid === PRED.VALID) {
        targets.push(MemoiTarget.fromFunction($func));
      }
    }

    return new MemoiTargetReport(targets, report);
  }
}

function isProcessed(
  sig: string,
  processed: Parameters<MemoiPred>[1]
): boolean {
  return processed[sig] === PRED.VALID || processed[sig] === PRED.INVALID;
}

function isWaiting(sig: string, processed: Parameters<MemoiPred>[1]): boolean {
  return processed[sig] === PRED.WAITING;
}

/**
 * This is the default predicate.
 */
function defaultMemoiPred(
  $call: Call,
  processed: Parameters<MemoiPred>[1],
  report: FailReport
) {
  const test = defaultMemoiPredRecursive($call, processed, report);

  if (test === PRED.WAITING) {
    const sig = $call.signature;

    processed[sig] = PRED.VALID;
    return PRED.VALID;
  }

  return test;
}

/**
 *  Checks if the target function is valid.
 *
 *	The constraints are:
 *	1) has between 1 and 3 parameters
 *	2) type of inputs and outputs is one of \{int, float, double\}
 * 	3) It doesn't access global state;
 *	4) It doesn't call non-valid functions.
 */
function defaultMemoiPredRecursive(
  $call: Call,
  processed: Parameters<MemoiPred>[1],
  report: FailReport
) {
  const sig = MemoiUtils.normalizeSig($call.signature);

  // 0) check if this function was processed before
  if (isProcessed(sig, processed)) {
    return processed[sig];
  }

  // mark this as being processed
  processed[sig] = PRED.WAITING;

  const $func = $call.function;
  const $functionType = $func.functionType;
  const $returnType = $functionType.returnType;
  const paramTypes = $functionType.paramTypes;

  // 1) has between 1 and 3 parameters
  if (paramTypes.length < 1 || paramTypes.length > 3) {
    debug(sig + " - wrong number of parameters: " + paramTypes.length);

    report.incNumParams($func.signature, paramTypes.length);

    processed[sig] = PRED.INVALID;
    return PRED.INVALID;
  }

  // 2) type of return and params is one of {int, float, double}
  if (!testType($returnType, ["int", "float", "double"])) {
    debug(sig + " - return type is not supported: " + $returnType.code);

    report.incTypeReturn($func.signature, $returnType.code);

    processed[sig] = PRED.INVALID;
    return PRED.INVALID;
  }

  for (const $type of paramTypes) {
    if (!testType($type, ["int", "float", "double"])) {
      debug(sig + " - param type is not supported: " + $type.code);

      report.incTypeParams($func.signature, $type.code);

      processed[sig] = PRED.INVALID;
      return PRED.INVALID;
    }
  }

  // Try to get the definition
  const $def = $call.definition;
  if ($def === undefined) {
    if (!MemoiUtils.isWhiteListed(sig)) {
      debug(sig + " - definition not found, not whitelisted");

      processed[sig] = PRED.INVALID;
      return PRED.INVALID;
    } else {
      processed[sig] = PRED.VALID;
      return PRED.VALID;
    }
  }

  // 3) It doesn't access global state (unless constants)
  const varRefs = $def.getDescendants("varref") as Varref[];
  for (const $ref of varRefs) {
    const $varDecl = $ref.vardecl;

    if ($varDecl.isGlobal && (!$ref.type.constant || $ref.type.isPointer)) {
      debug(sig + " - accesses non-const global storage variable " + $ref.code);

      report.incGlobalAccess($func.signature);

      processed[sig] = PRED.INVALID;
      return PRED.INVALID;
    }
  }

  // 4) It doesn't call non-valid functions
  let isChildWaiting = false;
  const $calls = $def.getDescendants("call") as Call[];
  for (const $childCall of $calls) {
    const childSig = $childCall.signature;

    if (isWaiting(childSig, processed)) {
      isChildWaiting = true;
      continue;
    }

    const test = defaultMemoiPredRecursive($childCall, processed, report);
    if (test === PRED.INVALID) {
      debug(sig + " - calls invalid function " + childSig);

      report.incInvalidCalls();

      processed[sig] = PRED.INVALID;
      return PRED.INVALID;
    } else if (test === PRED.WAITING) {
      isChildWaiting = true;
    }
  }

  if (isChildWaiting) {
    processed[sig] = undefined;
    return PRED.WAITING;
  }

  // Everything checked OK
  processed[sig] = PRED.VALID;
  return PRED.VALID;
}

/**
 * 		Tests if the type is one of the provided types.
 */
function testType($type: Type, typesToTest: string[]) {
  const code = $type.code;
  return typesToTest.includes(code);
}

/**
 * Class to hold info about target finding (targets and reports).
 */
export class MemoiTargetReport {
  targets: MemoiTarget[];
  failReport: FailReport;

  constructor(targets: MemoiTarget[], failReport: FailReport) {
    this.targets = targets;
    this.failReport = failReport;
  }

  isTarget($func: FunctionJp) {
    const sig = MemoiUtils.normalizeSig($func.signature);

    for (const t of this.targets) {
      if (t.sig === sig) {
        return true;
      }
    }

    return false;
  }

  toJson(jsonPath: string) {
    JSONtoFile(jsonPath, this);
  }

  printFailReport() {
    console.log("Reasons to fail:");

    if (this.failReport.numParams > 0) {
      console.log(`\tWrong number of params: ${this.failReport.numParams}`);
      console.log(
        "\t\t{" +
          Object.keys(this.failReport.unsupportedNumParams)
            .map(
              (key) => `${key}: ${this.failReport.unsupportedNumParams[key]}`
            )
            .join(", ") +
          "}"
      );
    }

    if (this.failReport.typeParams > 0)
      console.log(`\tWrong type of params: ${this.failReport.typeParams}`);

    if (this.failReport.typeReturn > 0)
      console.log(`\tWrong type of return: ${this.failReport.typeReturn}`);

    if (this.failReport.typeParams > 0 || this.failReport.typeReturn > 0) {
      console.log(
        "\t\t{" +
          Object.keys(this.failReport.unsupportedTypes)
            .map((key) => `${key}: ${this.failReport.unsupportedTypes[key]}`)
            .join(", ") +
          "}"
      );
    }

    if (this.failReport.globalAccess > 0)
      console.log(`\tGlobal accesses: ${this.failReport.globalAccess}`);

    if (this.failReport.invalidCalls > 0)
      console.log(
        `\tCalls to invalid functions: ${this.failReport.invalidCalls}`
      );
  }
}

/**
 * Class to hold info about failed targets.
 */
export class FailReport {
  private _failMap: Record<string, string> = {};
  private _numParams: number = 0;
  private _unsupportedNumParams: Record<string, number> = {};
  private _typeParams: number = 0;
  private _typeReturn: number = 0;
  private _unsupportedTypes: Record<string, number> = {};
  private _globalAccess: number = 0;
  private _invalidCalls: number = 0;

  get numParams() {
    return this._numParams;
  }

  get typeParams() {
    return this._typeParams;
  }

  get typeReturn() {
    return this._typeReturn;
  }

  get globalAccess() {
    return this._globalAccess;
  }

  get invalidCalls() {
    return this._invalidCalls;
  }

  get unsupportedNumParams() {
    return this._unsupportedNumParams;
  }

  get unsupportedTypes() {
    return this._unsupportedTypes;
  }

  incNumParams(sig: string, num: number | string) {
    this._failMap[sig] = "Wrong number of params";
    this._numParams++;
    this._addUnsupportedNumParams(String(num));
  }

  private _addUnsupportedNumParams(num: string) {
    this._unsupportedNumParams[num]
      ? this._unsupportedNumParams[num]++
      : (this._unsupportedNumParams[num] = 1);
  }

  incTypeParams(sig: string, type: string) {
    this._failMap[sig] = "Wrong type of params";
    this._typeParams++;
    this._addUnsupportedTypes(type);
  }

  incTypeReturn(sig: string, type: string) {
    this._failMap[sig] = "Wrong type of return";
    this._typeReturn++;
    this._addUnsupportedTypes(type);
  }

  private _addUnsupportedTypes(type: string) {
    this._unsupportedTypes[type]
      ? this._unsupportedTypes[type]++
      : (this._unsupportedTypes[type] = 1);
  }

  incGlobalAccess(sig: string) {
    this._failMap[sig] = "Global accesses";
    this._globalAccess++;
  }

  incInvalidCalls(sig?: string) {
    if (sig) {
      this._failMap[sig] = "Calls to invalid functions";
    }

    this._invalidCalls++;
  }
}
