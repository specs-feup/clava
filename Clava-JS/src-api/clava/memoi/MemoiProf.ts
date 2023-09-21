import IdGenerator from "lara-js/api/lara/util/IdGenerator.js";
import PrintOnce from "lara-js/api/lara/util/PrintOnce.js";
import Query from "lara-js/api/weaver/Query.js";
import { Call, FileJp, FunctionJp, Scope, Type } from "../../Joinpoints.js";
import Clava from "../Clava.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import MemoiTarget from "./MemoiTarget.js";
import MemoiUtils from "./MemoiUtils.js";

/**
 * Library to instrument applications with the memoiprof profiling library.
 *
 * @param targetSig - The signature of the target funtion
 * @param id - Unique ID representing this function
 * @param reportDir - Path to the directory where the report will be saved (does not need trailing /)
 */
export default class MemoiProf {
  private target: MemoiTarget;
  private id: string;
  private reportDir: string;

  /**
   * Options for memoiprof.
   */
  private memoiprofOptions: MemoiprofOptions = new MemoiprofOptions();

  constructor(target: MemoiTarget, id: string, reportDir: string) {
    this.target = target;
    this.id = id.replace(" ", "_");
    this.reportDir = reportDir;

    // Deal with dependecy to memoiprof
    PrintOnce.message(
      "Woven code has dependency to project memoiprof, which can be found at https://github.com/cc187/memoiprof"
    );
    Clava.getProgram().addProjectFromGit(
      "https://github.com/cc187/memoiprof.git",
      ["mp"]
    );
  }

  setSampling(samplingKind: SamplingKind, samplingRate: number) {
    this.memoiprofOptions.setSampling(samplingKind, samplingRate);
  }

  setPeriodicReporting(
    periodicReportKind: boolean,
    periodicReportRate: number
  ) {
    this.memoiprofOptions.setPeriodicReporting(
      periodicReportKind,
      periodicReportRate
    );
  }

  setCulling(cullingKind: boolean, cullingRatio: number) {
    this.memoiprofOptions.setCulling(cullingKind, cullingRatio);
  }

  setApprox(approxKind: boolean, approxBits: number) {
    this.memoiprofOptions.setApprox(approxKind, approxBits);
  }

  /**
   * 		Profiles all calls of the target function. This includes making a single
   * wrapper for all calls and adding the memoization profiling code inside this
   * wrapper.
   * */
  profAll() {
    const cSig = MemoiUtils.cSig(this.target.sig);
    const wrapperName = "mw_" + cSig;
    const monitorName = "mp_" + cSig;
    const monitorType = ClavaJoinPoints.typeLiteral("MemoiProf*");

    // make the wrapper
    for (const $jp of Query.search("call", {
      signature: (s: string) => this.target.sig === MemoiUtils.normalizeSig(s),
    })) {
      const $call = $jp as Call;
      $call.wrap(wrapperName);
    }

    // instrument the wrapper
    this.memoiInstrumentWrapper(wrapperName, monitorName, monitorType);

    // setup
    this.memoiSetup(monitorName, monitorType, this.id, ["global"]);
  }

  /**
   * 		Profiles each call to the target function separately. This includes
   * 	making a wrapper for each call and adding the memoization profiling code
   * 	inside the wrapper.
   * */
  profEach() {
    const cSig = MemoiUtils.cSig(this.target.sig);
    const wrapperNameBase = "mw_" + cSig;
    const monitorNameBase = "mp_" + cSig;
    const monitorType = ClavaJoinPoints.typeLiteral("MemoiProf*");

    for (const $jp of Query.search("call", {
      signature: (s: string) => this.target.sig === MemoiUtils.normalizeSig(s),
    })) {
      const $call = $jp as Call;

      // make the wrapper
      const wrapperName = IdGenerator.next(wrapperNameBase);
      $call.wrap(wrapperName);

      // instrument the wrapper
      const monitorName = IdGenerator.next(monitorNameBase);
      this.memoiInstrumentWrapper(wrapperName, monitorName, monitorType);

      const callSiteInfo = $call.location;

      // setup
      const id = IdGenerator.next(this.id + "_");
      this.memoiSetup(monitorName, monitorType, id, [callSiteInfo]);
    }
  }

  private memoiInstrumentWrapper(
    wrapperName: string,
    monitorName: string,
    monitorType: Type
  ) {
    const numInputs = this.target.numInputs;
    const numOutputs = this.target.numOutputs;

    const query = Query.search("file")
      .search("function", { name: wrapperName })
      .search("call")
      .chain();

    for (const row of query) {
      let code = "mp_inc(" + monitorName;

      const $params = (row["function"] as FunctionJp).params;

      for (let i = 0; i < numInputs; i++) {
        code += ", &" + $params[i].name;
      }

      if (numOutputs == 1) {
        code += ", &result";
      } else {
        for (let o = numInputs; o < $params.length; o++) {
          code += ", " + $params[o].name;
        }
      }

      code += ");";

      const $call = row["call"] as Call;
      $call.insert("after", code);
      $call.insert("after", "#pragma omp critical"); // needed for correct semantics under OpenMP

      const $file = row["file"] as FileJp;
      $file.addGlobal(monitorName, monitorType, "NULL");
      $file.addInclude("MemoiProfiler.h", false);
      $file.addInclude("stdlib.h", true);
    }
  }

  private memoiSetup(
    monitorName: string,
    monitorType: Type,
    id: string,
    callSiteInfo: string[]
  ) {
    const inputsCode = this.target.inputTypes
      .map(function (e) {
        return "mp_" + e;
      })
      .join(",")
      .toUpperCase();
    const outputsCode = this.target.outputTypes
      .map(function (e) {
        return "mp_" + e;
      })
      .join(",")
      .toUpperCase();

    const query = Query.search("file")
      .search("function", { name: "main" })
      .children("scope")
      .chain()[0];

    if (query !== undefined) {
      throw new Error(
        "MemoiProf: Could not find main function needed for setup"
      );
    }

    const memoiReportPath = "path_" + monitorName;

    const $body = query["scope"] as Scope;

    // memoiprof options
    if (this.memoiprofOptions.samplingKind !== SamplingKind.OFF) {
      const samplingKind =
        "MP_SAMPLING_" + this.memoiprofOptions.samplingKind.toUpperCase();
      $body.insertBegin(
        `mp_set_sampling(${monitorName}, ${samplingKind}, ${this.memoiprofOptions.samplingRate});`
      );
    }

    if (this.memoiprofOptions.periodicReportKind) {
      $body.insertBegin(
        `mp_set_periodic_reporting(${monitorName}, MP_PERIODIC_ON, ${this.memoiprofOptions.periodicReportRate});`
      );
    }

    if (this.memoiprofOptions.cullingKind) {
      $body.insertBegin(
        `mp_set_culling(${monitorName}, MP_CULLING_ON, ${this.memoiprofOptions.cullingRatio});`
      );
    }

    if (this.memoiprofOptions.approxKind) {
      $body.insertBegin(
        `mp_set_approx(${monitorName}, MP_APPROX_ON, ${this.memoiprofOptions.approxBits});`
      );
    }

    this.memoiAddCallSiteInfo($body, callSiteInfo, monitorName);
    $body.insertBegin(`free(${memoiReportPath});`); // can free here, since mp_init duped it
    $body.insertBegin(
      `${monitorName} = mp_init("${this.target.sig}", "${id}", ${memoiReportPath}, ${this.target.inputTypes.length}, ${this.target.outputTypes.length}, ${inputsCode}, ${outputsCode});`
    );
    $body.insertBegin(
      `char* ${memoiReportPath} = mp_make_report_path("${this.reportDir}", "${monitorName}");`
    );

    /* add functions to print and clean up at every return on main */
    const $function = query["function"] as FunctionJp;
    $function.insertReturn(`mp_to_json(${monitorName});`);
    $function.insertReturn(`${monitorName} = mp_destroy(${monitorName});`);

    const $file = query["file"] as FileJp;
    $file.addGlobal(monitorName, monitorType, "NULL");
    $file.addInclude("MemoiProfiler.h", false);
    $file.addInclude("stdlib.h", true);
  }

  private memoiAddCallSiteInfo(
    $mainBody: Scope,
    callSiteInfo: string[] = ["global"],
    monitorName: string
  ) {
    const length = callSiteInfo.length;

    $mainBody.insertBegin(
      "mp_set_call_sites(" +
        monitorName +
        ", " +
        length +
        ", " +
        callSiteInfo.map((i) => `"${i}"`).join(", ") +
        ");"
    );
  }
}

export enum SamplingKind {
  RANDOM = "random",
  FIXED = "fixed",
  OFF = "off",
}

/**
 * Class used to store memoiprof options.
 * */
export class MemoiprofOptions {
  private _samplingKind: SamplingKind = SamplingKind.OFF;
  private _samplingRate: number = 0;

  private _periodicReportKind: boolean = false;
  private _periodicReportRate: number = 0;

  private _cullingKind: boolean = false;
  private _cullingRatio: number = 0.0;

  private _approxKind: boolean = false;
  private _approxBits: number = 0;

  get samplingKind() {
    return this._samplingKind;
  }

  get samplingRate() {
    return this._samplingRate;
  }

  get periodicReportKind() {
    return this._periodicReportKind;
  }

  get periodicReportRate() {
    return this._periodicReportRate;
  }

  get cullingKind() {
    return this._cullingKind;
  }

  get cullingRatio() {
    return this._cullingRatio;
  }

  get approxKind() {
    return this._approxKind;
  }

  get approxBits() {
    return this._approxBits;
  }

  /**
   * Supported samplingKind values are: 'random', 'fixed', 'off'.
   * For 1/x sampling, samplingRate should be x.
   * */
  setSampling(samplingKind: SamplingKind, samplingRate: number) {
    this._samplingKind = samplingKind;
    this._samplingRate = samplingRate;
  }

  /**
   * Supported periodicReportKind values are: true, false.
   * periodicReportRate is the number of calls between writes of periodic reports.
   * */
  setPeriodicReporting(
    periodicReportKind: boolean,
    periodicReportRate: number
  ) {
    this._periodicReportKind = periodicReportKind;
    this._periodicReportRate = periodicReportRate;
  }

  /**
   * Supported cullingKind values are: true, false.
   * cullingRatio is the threshold (% of calls) for printing to the JSON.
   * */
  setCulling(cullingKind: boolean, cullingRatio: number) {
    this._cullingKind = cullingKind;
    this._cullingRatio = cullingRatio;
  }

  /**
   * Supported approxKind values are: true, false.
   * */
  setApprox(approxKind: boolean, approxBits: number) {
    this._approxKind = approxKind;
    this._approxBits = approxBits;
  }
}
