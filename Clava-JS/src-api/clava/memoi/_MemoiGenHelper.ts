import { debug } from "lara-js/api/lara/core/LaraCore.js";
import IdGenerator from "lara-js/api/lara/util/IdGenerator.js";
import Query from "lara-js/api/weaver/Query.js";
import { Call, FileJp, FunctionJp } from "../../Joinpoints.js";
import ClavaJavaTypes, { ClavaJavaClasses } from "../ClavaJavaTypes.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import MemoiUtils from "./MemoiUtils.js";

/**
 * BE VERY CAREFULL WHEN USING FUNCTIONS FROM THIS FILE.
 * WHEN REFACTORING THIS FILE I NOTICED THAT SOME JAVA FUNCTIONS BEING CALLED
 * DO NOT APPEAR TO EXIST OR ARE INCORRECTLY CALLED.
 * ALSO, THERE ARE NO TESTS COVERING THIS CODE.
 */

/**
 * 		Generates the table and supporting code for this report.
 *
 * 		Inserts elements in the table based on the predicate insertPred.
 * */
export function _generate(
  insertPred: any,
  countComparator: any,
  report: ClavaJavaClasses.MemoiReport,
  isMemoiDebug: boolean,
  isMemoiOnline: boolean,
  isMemoiEmpty: boolean,
  isMemoiUpdateAlways: boolean,
  memoiApproxBits: number,
  tableSize: number,
  signature: string,
  callSite: string
) {
  let wt;

  if (callSite === "global") {
    wt = _Memoi_WrapGlobalTarget(signature);
  } else {
    wt = _Memoi_WrapSingleTarget(signature, callSite);
  }
  _Memoi_InsertTableCode(
    insertPred,
    countComparator,
    report,
    wt.wrapperName,
    isMemoiDebug,
    isMemoiOnline,
    isMemoiEmpty,
    isMemoiUpdateAlways,
    memoiApproxBits,
    tableSize
  );
}

export function _Memoi_WrapGlobalTarget(signature: string) {
  const wrapperName = `mw_${MemoiUtils.normalizeSig(signature)}`;

  for (const chain of Query.search("stmt")
    .search("call", {
      signature: (sig: string) => sig.replace(/ /g, "") == signature,
    })
    .chain()) {
    const $call = chain["call"] as Call;
    $call.wrap(wrapperName);
    debug("wrapped");
  }

  return { wrapperName };
}

export function _Memoi_WrapSingleTarget(signature: string, location: string) {
  for (const chain of Query.search("stmt")
    .search("call", {
      signature: (sig: string) => sig.replace(/ /g, "") == signature,
    })
    .chain()) {
    const $call = chain["call"] as Call;

    const wrapperName = IdGenerator.next(
      `mw_${MemoiUtils.normalizeSig(signature)}`
    );
    $call.wrap(wrapperName);
    debug("wrapped");
    return { wrapperName };
  }

  throw `Did not find call to ${signature} at ${location}`;
}

export function _Memoi_InsertTableCode(
  insertPred: any,
  countComparator: any,
  report: ClavaJavaClasses.MemoiReport,
  wrapperName: string,
  isMemoiDebug: boolean,
  isMemoiOnline: boolean,
  isMemoiEmpty: boolean,
  isMemoiUpdateAlways: boolean,
  memoiApproxBits: number,
  tableSize: number
) {
  Query.search("file")
    .search("function", { name: wrapperName })
    .search("call")
    .chain()
    .forEach((chain) => {
      const $file = chain["file"] as FileJp;
      const $function = chain["function"] as FunctionJp;
      const $call = chain["call"] as Call;

      if (isMemoiDebug) {
        const totalName = wrapperName + "_total_calls";
        const missesName = wrapperName + "_total_misses";
        $file.addGlobal(totalName, ClavaJoinPoints.builtinType("int"), "0");
        $file.addGlobal(missesName, ClavaJoinPoints.builtinType("int"), "0");
        $call.insertBefore(`${totalName}++;`);
        $call.insertAfter(`${missesName}++;`);

        _Memoi_AddMainDebug(totalName, missesName, wrapperName);
      }

      const tableCode = _makeTableCode(
        insertPred,
        countComparator,
        report,
        $function,
        tableSize,
        isMemoiEmpty,
        isMemoiOnline,
        memoiApproxBits
      );
      $call.insertBefore(tableCode);

      if (isMemoiOnline) {
        const updateCode = _makeUpdateCode(
          report,
          $function,
          tableSize,
          isMemoiUpdateAlways
        );
        $call.insertAfter(updateCode);
      }

      $file.addInclude("stdint.h", true);
    });
}

export function _Memoi_AddMainDebug(
  totalName: string,
  missesName: string,
  wrapperName: string
) {
  Query.search("file")
    .search("function", { name: "main" })
    .chain()
    .forEach((chain) => {
      const $file = chain["file"] as FileJp;
      const $function = chain["function"] as FunctionJp;

      $file.addGlobal(totalName, ClavaJoinPoints.builtinType("int"), "0");
      $file.addGlobal(missesName, ClavaJoinPoints.builtinType("int"), "0");
      $file.addInclude("stdio.h", true);

      $function.insertReturn(`
          printf("${wrapperName}\t%d / %d (%.2f%%)\n",
              ${totalName} - ${missesName},
              ${totalName},
              (${totalName} - ${missesName}) * 100.0 / ${totalName});
      `);
    });
}

export function _baseLog(num: number, base: number) {
  return Math.log(num) / Math.log(base);
}

export function _makeTableCode(
  insertPred: any,
  countComparator: any,
  report: ClavaJavaClasses.MemoiReport,
  $function: FunctionJp,
  tableSize: number,
  isMemoiEmpty: boolean,
  isMemoiOnline: boolean,
  memoiApproxBits: number
) {
  const indexBits = _baseLog(tableSize, 2);

  debug("table size: " + tableSize);
  debug("index bits: " + indexBits);

  const paramNames = [];
  for (const $param of $function.params) {
    paramNames.push($param.name);
  }

  const code = ClavaJavaTypes.MemoiCodeGen.generateDmtCode(
    report,
    tableSize,
    paramNames,
    isMemoiEmpty,
    isMemoiOnline,
    memoiApproxBits
  ) as string;

  return code;
}

export function _makeUpdateCode(
  report: ClavaJavaClasses.MemoiReport,
  $function: FunctionJp,
  tableSize: number,
  isMemoiUpdateAlways: boolean
) {
  const paramNames = [];
  for (const $param of $function.params) {
    paramNames.push($param.name);
  }

  const code = ClavaJavaTypes.MemoiCodeGen.generateUpdateCode(
    report,
    tableSize,
    paramNames,
    isMemoiUpdateAlways
  ) as string;

  return code;
}

export const sizeMap: Record<string, number> = {
  float: 32,
  int: 32,
  double: 64,
};

export function _printTable(table: Record<string, any>[], tableSize: number) {
  for (let i = 0; i < tableSize; i++) {
    if (table[i] !== undefined) {
      let code = "";

      const fullKey = table[i].fullKey as string;
      const keys = fullKey.split("#");
      for (let k = 0; k < keys.length; k++) {
        code += "0x" + keys[k] + ", ";
      }
      code += "0x" + table[i].output;

      console.log(code);
    }
  }
}

export function _printTableReport(
  collisions: number,
  totalElements: number,
  maxCollision: number,
  report: ClavaJavaClasses.MemoiReport,
  tableSize: number,
  table: Record<string, any>[]
) {
  let tableCalls = 0;
  let tableElements = 0;
  for (let i = 0; i < tableSize; i++) {
    if (table[i] != undefined) {
      tableCalls += mean(table[i].counter, report.reportCount);
      tableElements++;
    }
  }
  const totalCalls = mean(report.calls, report.reportCount);
  const collisionPercentage = (collisions / totalElements) * 100;
  const elementsCoverage = (tableElements / totalElements) * 100;
  const callCoverage = (tableCalls / totalCalls) * 100;

  console.log(
    "collisions: " + collisions + " (" + collisionPercentage.toFixed(2) + "%)"
  );
  console.log("largest collision: " + maxCollision);
  console.log(
    "element coverage: " +
      tableElements +
      "/" +
      totalElements +
      " (" +
      elementsCoverage.toFixed(2) +
      ")%"
  );
  console.log(
    "call coverage: " +
      tableCalls +
      "/" +
      totalCalls +
      " (" +
      callCoverage.toFixed(2) +
      ")%"
  );
}

export function _hashFunctionHalf(bits64: string): string {
  const len = bits64.length;
  let hashString = "";

  for (let i = 0; i < len / 2; i++) {
    const number =
      parseInt(bits64.charAt(i), 16) ^ parseInt(bits64.charAt(i + len / 2), 16);
    hashString += number.toString(16);
  }

  return hashString;
}

export function _hashFunctionOld(bits64: string, indexBits: number): string {
  switch (indexBits) {
    case 8: {
      const bits32 = _hashFunctionHalf(bits64);
      const bits16 = _hashFunctionHalf(bits32);
      const bits8 = _hashFunctionHalf(bits16);
      return String(parseInt(bits8, 16));
    }
    case 16: {
      const bits32 = _hashFunctionHalf(bits64);
      const bits16 = _hashFunctionHalf(bits32);
      return String(parseInt(bits16, 16));
    }
    default:
      return bits64;
      break;
  }
}

export function _hashFunction(bits64: string, indexBits: number): string {
  const varBits = 64;
  let lastPower = varBits;
  const iters = _baseLog(varBits / indexBits, 2);
  const intIters = parseInt(String(iters), 10);

  let hash = bits64;
  for (let i = 0; i < intIters; i++) {
    hash = _hashFunctionHalf(hash);
    lastPower = lastPower / 2;
  }

  // if not integer, we need to mask bits at the end
  if (iters !== intIters) {
    // mask starts with 16 bits
    let mask = parseInt("0xffff", 16);
    const shift = 16 - indexBits;
    mask = mask >> shift;

    hash = String(parseInt(hash, 16) & mask);

    return hash;
  }

  hash = String(parseInt(hash, 16));
  return hash;
}

/**
 * 		Converts counts from a map to an array.
 * */
export function _convertCounts(newReport: ClavaJavaClasses.MemoiReport) {
  const a = [];

  for (const countP in newReport.counts) {
    const count = newReport.counts[countP];
    a.push(count);
  }

  newReport.counts = a;
}

// function
export function totalTopN(
  report: ClavaJavaClasses.MemoiReport,
  n: number,
  reportCount: number
) {
  let result = 0;

  const averageCounts = [];

  for (const count of report.counts) {
    averageCounts.push(mean(count.counter, reportCount));
  }

  sortDescending(averageCounts);

  for (let i = 0; i < Math.min(n, averageCounts.length); i++) {
    result += averageCounts[i];
  }

  return result;
}

// function
export function elementsForRatio(
  report: ClavaJavaClasses.MemoiReport,
  total: number,
  ratio: number,
  reportCount: number
): number {
  let sum = 0;

  const averageCounts = [];

  for (const count of report.counts) {
    averageCounts.push(mean(count.counter, reportCount));
  }

  sortDescending(averageCounts);

  for (let elements = 0; elements < averageCounts.length; elements++) {
    sum += averageCounts[elements];
    if (sum / total > ratio) {
      return elements + 1;
    }
  }

  return report.elements; // ?
}

// function
export function getQuartVal(counts: number[], idx: number) {
  const floor = Math.floor(idx);

  let val;
  if (idx == floor) {
    val = (counts[idx] + counts[idx - 1]) / 2;
  } else {
    val = counts[floor];
  }

  return val;
}

// function
export function bwp(report: ClavaJavaClasses.MemoiReport, reportCount: number) {
  const averageCounts = [];

  for (const count of report.counts) {
    averageCounts.push(average(count.counter, reportCount));
  }

  sortDescending(averageCounts);

  const length = averageCounts.length;

  const min = averageCounts[length - 1];
  const q1 = getQuartVal(averageCounts, (1 / 4) * length);
  const q2 = getQuartVal(averageCounts, (2 / 4) * length);
  const q3 = getQuartVal(averageCounts, (3 / 4) * length);
  const max = averageCounts[0];
  const iqr = q3 - q1;

  return {
    min,
    q1,
    q2,
    q3,
    max,
    iqr,
  };
}

// function
export function printBwp(
  report: ClavaJavaClasses.MemoiReport,
  reportCount: number
) {
  const b = bwp(report, reportCount);
  console.log(
    `{ ${b.min}, ${b.q1}, ${b.q2}, ${b.q3}, ${b.max} } iqr: ${b.iqr}`
  );
}

/**
 * @deprecated This function does not calculate a mean, but an average.
 */
export function mean(values: number[], count: number) {
  return average(values, count);
}

export function average(values: number[], count: number) {
  let sum = 0;

  for (const value of values) {
    sum += value;
  }

  if (count === undefined) {
    return sum / values.length;
  } else {
    return sum / count;
  }
}

export function sortDescending<T>(array: T[]) {
  return array.sort(function (a, b) {
    if (a < b) return 1;
    else if (a > b) return -1;
    else return 0;
  });
}

export function sortAscending<T>(array: T[]) {
  return sortDescending(array).reverse();
}
