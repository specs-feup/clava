import Io from "lara-js/api/lara/Io.js";
import { arrayFromArgs } from "lara-js/api/lara/core/LaraCore.js";
import IdGenerator from "lara-js/api/lara/util/IdGenerator.js";
import { TimerUnit } from "lara-js/api/lara/util/TimeUnits.js";
import Query from "lara-js/api/weaver/Query.js";
import { Call, FileJp, FunctionJp } from "../../Joinpoints.js";
import Timer from "../../lara/code/Timer.js";
import ClavaJavaTypes, { ClavaJavaClasses } from "../ClavaJavaTypes.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import MemoiTarget from "./MemoiTarget.js";
import MemoiUtils from "./MemoiUtils.js";

class MemoiGen {
  private _target: MemoiTarget;
  private _isEmpty: boolean = true;
  private _isOnline: boolean = true;
  private _isUpdateAlways: boolean = false;
  private _approxBits: number = 0;
  private _isProf: boolean = false;
  private _profReportFiles: string[] = [];
  private _tableSize: number = 65536;
  private _isDebug: boolean = false;
  private _applyPolicy = MemoiGen.ApplyPolicy.NOT_EMPTY;
  private _isZeroSim: boolean = false;
  private _isResetFunc: boolean = false;

  constructor(target: MemoiTarget) {
    this._target = target;
  }

  /**
   * 		Sets whether to generate a reset function.
   * */
  setResetFunc(isResetFunc: boolean = true) {
    this._isResetFunc = isResetFunc;
  }

  /**
   * 		Sets whether to generate code for a 0% sim.
   * */
  setZeroSim(isZeroSim: boolean = true) {
    this._isZeroSim = isZeroSim;
  }

  /**
   * 		Sets whether to generate an empty table in the final application.
   * */
  setEmpty(isEmpty: boolean = true) {
    this._isEmpty = isEmpty;
  }

  /**
   * 		Sets whether to generate update code in the final application.
   * */
  setOnline(isOnline: boolean = true) {
    this._isOnline = isOnline;
  }

  /**
   * 		Sets whether to always update the table on a miss, even if not vacant.
   * */
  setUpdateAlways(isUpdateAlways: boolean = true) {
    this._isUpdateAlways = isUpdateAlways;
  }

  /**
   * 		Sets the approximation bits in the final application.
   * 		Defaults to 0.
   * */
  setApproxBits(bits: number = 0) {
    this._approxBits = bits;
  }

  /**
   * 		Sets the table size in the final application.
   * 		Defaults to 65536.
   * */
  setTableSize(size: number = 65536) {
    const allowedSizes = [
      256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536,
    ];
    if (!allowedSizes.includes(size)) {
      throw new Error(
        "MemoiGen.setTableSize: The possible table sizes are 2^b, with b in [8, 16]."
      );
    }

    this._tableSize = size;
  }

  /**
   * 		Sets whether to generate debug code in the final application.
   * */
  setDebug(isDebug: boolean = true) {
    this._isDebug = isDebug;
  }

  /**
   *		Sets the apply policy.
   *		Defaults to MemoiApplyPolicy.NOT_EMPTY.
   */
  setApplyPolicy(
    policy: MemoiGen.ApplyPolicy = MemoiGen.ApplyPolicy.NOT_EMPTY
  ) {
    this._applyPolicy = policy;
  }

  /**
   * Checks files with the given names for reports matching the current target.
   * @param names - the paths of the report files
   */
  setProfFromFileNames(...names: string[]) {
    this._isProf = true;
    this._isEmpty = false;

    const namesArray = arrayFromArgs(names) as string[];

    for (const name of namesArray) {
      if (!Io.isFile(name)) {
        throw new Error(`MemoiGen.setProfFromFileNames: ${name} is not a file`);
      }
      this._profReportFiles.push(name);
    }
  }

  /**
   * Checks dir for reports matching the current target.
   * @param dir - The path to the directory of the report files
   */
  setProfFromDir(dir: string) {
    this._isProf = true;
    this._isEmpty = false;
    this._profReportFiles = Io.getFiles(dir, "*.json", false).map((f) =>
      f.getAbsolutePath()
    );
  }

  /**
   * 		Generates a table for all calls of the target function.
   *
   * 		If a profiling reports are provided, the reports are used to
   * 	determine whether to generate for all target or for each single
   * 	target.
   */
  generate() {
    return this._generateAll();
  }

  /**
   * 		Generates a table for each call of the target function.
   *
   * 		If a profiling reports are provided, the reports are used to
   * 	determine whether to generate for all target or for each single
   * 	target.
   */
  generateEach() {
    this.checkParams("generateEach");

    if (this._isProf) {
      return this.generateFromReport();
    } else {
      return this._generateEach(undefined);
    }
  }

  /**
   * 		Generates a table for all calls of the target function.
   *
   * 		If a profiling reports are provided, the reports are used to
   * 	determine whether to generate for all target or for each single
   * 	target.
   */
  generateAll() {
    this.checkParams("generateAll");

    if (this._isProf) {
      return this.generateFromReport();
    } else {
      return this._generateAll(undefined);
    }
  }

  private generateFromReport() {
    const s = new Set<string>();

    const reportsMap = ClavaJavaTypes.MemoiReportsMap.fromNames(
      this._profReportFiles
    ); // maybe this needs list

    const siteMap = reportsMap.get(this._target.sig);

    if (siteMap === null || siteMap === undefined) {
      throw (
        "Could not find report for target " +
        this._target.sig +
        " in files [" +
        this._profReportFiles.join(", ") +
        "]"
      );
    }

    for (const site in siteMap) {
      // merge all reports for this <target, site>
      const reportPathList = siteMap.get(site);
      const report = ClavaJavaTypes.MemoiReport.mergeReportsFromNames(
        reportPathList
      ) as ClavaJavaClasses.MemoiReport;

      if (site === "global") {
        const sTmp = this._generateAll(report);
        for (const sT of sTmp.values()) {
          s.add(sT);
        }
      } else {
        const sTmp = this._generateEach(report);
        for (const sT of sTmp.values()) {
          s.add(sT);
        }
      }
    }

    return s;
  }

  private _generateEach(report?: ClavaJavaClasses.MemoiReport) {
    const s = new Set<string>();

    const cSig = MemoiUtils.cSig(this._target.sig);

    const filter = {
      signature: (s: Call["signature"]) =>
        this._target.sig === MemoiUtils.normalizeSig(s),
      location: (l: Call["location"]) =>
        report !== undefined ? report.callSites[0] === l : true, // if there is a report, we also filter by site
    };

    for (const $jp of Query.search(Call, filter)) {
      const $call = $jp as Call;

      const wrapperName = IdGenerator.next("mw_" + cSig);
      s.add(wrapperName);

      $call.wrap(wrapperName);

      this.generateGeneric(wrapperName, report);
    }

    return s;
  }

  private _generateAll(report?: ClavaJavaClasses.MemoiReport) {
    const s = new Set<string>();

    const cSig = MemoiUtils.cSig(this._target.sig);
    const wrapperName = "mw_" + cSig;
    s.add(wrapperName);

    for (const $jp of Query.search(Call, {
      signature: (s: Call["signature"]) =>
        this._target.sig === MemoiUtils.normalizeSig(s),
    })) {
      const $call = $jp as Call;
      $call.wrap(wrapperName);
    }
    this.generateGeneric(wrapperName, report);

    return s;
  }

  generatePerfectInst() {
    const cSig = MemoiUtils.cSig(this._target.sig);
    const wrapperName = "mw_" + cSig;
    const globalName = "memoi_target_timer";
    const printName = "print_perfect_inst";

    // wrap every call to the target
    for (const $jp of Query.search(Call, {
      signature: (s: string) => this._target.sig === MemoiUtils.normalizeSig(s),
    })) {
      const $call = $jp as Call;
      $call.wrap(wrapperName);
    }

    // change the wrapper by timing around the original call
    for (const chain of Query.search(FileJp)
      .search(FunctionJp, { name: wrapperName })
      .search(Call)
      .chain()) {
      const $file = chain["file"] as FileJp;
      const $function = chain["function"] as FunctionJp;
      const $call = chain["call"] as Call;

      const t = new Timer(TimerUnit.SECONDS);

      $file.addGlobal(globalName, ClavaJoinPoints.builtinType("double"), "0.0");

      const tVar = t.time($call);
      $function.insertReturn(`memoi_target_timer += ${tVar};`);
    }

    // if print_perfect_inst function is found, some other target has dealt with the main code and we're done
    for (const chain of Query.search(FileJp, { hasMain: true })
      .search(FunctionJp, { name: printName })
      .chain()) {
      return;
    }

    // change the main function to print the time to a file
    for (const chain of Query.search(FileJp)
      .search(FunctionJp, { name: "main" })
      .chain()) {
      const $file = chain["file"] as FileJp;
      const $main = chain["function"] as FunctionJp;

      $file.addGlobal(globalName, ClavaJoinPoints.builtinType("double"), "0.0");
      $file.addInclude("stdio.h", true);
      $file.addInclude("sys/stat.h", true);
      $file.addInclude("sys/types.h", true);
      $file.addInclude("errno.h", true);
      $file.addInclude("string.h", true);
      $file.addInclude("libgen.h", true);

      $main.insertReturn(printName + "(argv[0]);");
      $main.insertBefore("void " + printName + "(char*);");

      const $function = $file.addFunction(printName) as FunctionJp;
      $function.setParamsFromStrings(["char* name"]);

      $function.insertReturn(`
            
            
            errno = 0;
            char* prog_name = basename(name);
            char* file_name = malloc(strlen(prog_name) + strlen("${globalName}.txt") + 1 + 1);
            sprintf(file_name, "%s.%s", prog_name, "${globalName}.txt");

            FILE * ${globalName}_file = fopen (file_name,"a");
            if(${globalName}_file == NULL) {
                perror("Could not create file for memoi perfect instrumentation");
            } else {
                fprintf(${globalName}_file, "%f\n", ${globalName});
                fclose(${globalName}_file);
            }
        `);
    }
  }

  private generateGeneric(
    wrapperName: string,
    report?: ClavaJavaClasses.MemoiReport
  ) {
    for (const chain of Query.search(FileJp)
      .search(FunctionJp, { name: wrapperName })
      .search(Call)
      .chain()) {
      const $file = chain["file"] as FileJp;
      const $function = chain["function"] as FunctionJp;
      const $call = chain["call"] as Call;

      let dmt: any | undefined = undefined;
      if (report !== undefined) {
        // build the DMT and test the apply policy
        dmt = ClavaJavaTypes.MemoiCodeGen.generateDmt(
          this._tableSize,
          report,
          this._applyPolicy
        );
        if (!dmt.testPolicy()) {
          console.log(
            this._target.sig +
              "[" +
              report.callSites[0] +
              "]" +
              " fails the apply policy " +
              this._applyPolicy +
              ". Not applying memoization."
          );
          return;
        }
      }

      let updatesName: string | undefined = undefined;
      if (this._isDebug) {
        const totalName = wrapperName + "_total_calls";
        const missesName = wrapperName + "_total_misses";
        $file.addGlobal(
          totalName,
          ClavaJoinPoints.builtinType("unsigned int"),
          "0"
        );
        $file.addGlobal(
          missesName,
          ClavaJoinPoints.builtinType("unsigned int"),
          "0"
        );
        $call.insert("before", `${totalName}++;`);
        $call.insert("after", `${missesName}++;`);

        if (this._isOnline) {
          updatesName = wrapperName + "_total_updates";
          $file.addGlobal(
            updatesName,
            ClavaJoinPoints.builtinType("unsigned int"),
            "0"
          );
        }

        this.addMainDebug(totalName, missesName, updatesName, wrapperName);
      }

      // generate the table (and reset) code and add it before the wrapper
      const tableCode = ClavaJavaTypes.MemoiCodeGen.generateTableCode(
        dmt.getTable(),
        this._tableSize,
        this._target.numInputs,
        this._target.numOutputs,
        this._isOnline,
        this._isResetFunc,
        this._isZeroSim ? true : this._isEmpty, // whether this is an empty table
        wrapperName
      ) as string;
      $function.insert("before", tableCode);

      // generate the logic code and add it before the original call
      const paramNames = [];
      for (const $param of $function.params) {
        paramNames.push($param.name);
      }
      const logicCode = ClavaJavaTypes.MemoiCodeGen.generateLogicCode(
        this._tableSize,
        paramNames,
        this._approxBits,
        this._target.numInputs,
        this._target.numOutputs,
        this._target.inputTypes,
        this._target.outputTypes,
        wrapperName
      ) as string;
      $call.insert("before", logicCode);

      if (this._isOnline) {
        const updateCode = ClavaJavaTypes.MemoiCodeGen.generateUpdateCode(
          this._tableSize,
          paramNames,
          this._isUpdateAlways,
          this._target.numInputs,
          this._target.numOutputs,
          updatesName,
          this._isZeroSim,
          wrapperName
        ) as string;
        $call.insert("after", updateCode);
      }

      $file.addInclude("stdint.h", true);
    }
  }

  private addMainDebug(
    totalName: string,
    missesName: string,
    updatesName: string | undefined,
    wrapperName: string
  ) {
    const chain = Query.search(FileJp)
      .search(FunctionJp, { name: "main" })
      .chain();
    const firstAndOnly = chain[0];

    if (firstAndOnly === undefined) {
      console.log(
        "Could not find a main function. It may be impossible to print debug stats if this is a library code."
      );
      return;
    }

    const $file = firstAndOnly["file"] as FileJp;
    const $function = firstAndOnly["function"] as FunctionJp;

    $file.addGlobal(
      totalName,
      ClavaJoinPoints.builtinType("unsigned int"),
      "0"
    );
    $file.addGlobal(
      missesName,
      ClavaJoinPoints.builtinType("unsigned int"),
      "0"
    );
    if (this._isOnline) {
      $file.addGlobal(
        updatesName!,
        ClavaJoinPoints.builtinType("unsigned int"),
        "0"
      );
    }

    $file.addInclude("stdio.h", true);
    $file.addInclude("sys/stat.h", true);
    $file.addInclude("sys/types.h", true);
    $file.addInclude("errno.h", true);

    let updatesStringCode = "";
    let updatesValuesCode = "";

    if (this._isOnline) {
      updatesStringCode = ', \\"updates\\": %u';
      updatesValuesCode = ", " + updatesName!;
    }

    const json = `
        {
        errno = 0;
        int dir_result = mkdir("memoi-exec-report", 0755);
        if(dir_result != 0 && errno != EEXIST){
            perror("Could not create directory for memoi execution reports");
        }
        else{
            errno = 0;
            FILE * _memoi_rep_file = fopen ("memoi-exec-report/${wrapperName}.json","w");
            if(_memoi_rep_file == NULL) {
                perror("Could not create file for memoi execution report");
            } else {
                fprintf(_memoi_rep_file, "{"name": "${wrapperName}", "total": %u, "hits": %u, "misses": %u${updatesStringCode}}\n", ${totalName}, ${totalName} - ${missesName}, ${missesName}${updatesValuesCode});
                fclose(_memoi_rep_file);
            }
        }
        }
    `;

    $function.insertReturn(json);
  }

  private checkParams(source: string) {
    if (!((this._isEmpty && this._isOnline) || !this._isEmpty)) {
      throw new Error(
        `MemoiGen.${source}: Can't have empty and offline table.`
      );
    }

    if (!(this._isEmpty ? !this._isProf : this._isProf)) {
      throw new Error(
        "MemoiGen.${source}: Empty table and profile are mutually exclusive table."
      );
    }
  }
}

namespace MemoiGen {
  export enum ApplyPolicy {
    ALWAYS = "ALWAYS",
    NOT_EMPTY = "NOT_EMPTY",
    OVER_25_PCT = "OVER_25_PCT",
    OVER_50_PCT = "OVER_50_PCT",
    OVER_75_PCT = "OVER_75_PCT",
    OVER_90_PCT = "OVER_90_PCT",
  }
}

export default MemoiGen;
