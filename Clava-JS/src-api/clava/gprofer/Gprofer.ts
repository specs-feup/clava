import Io from "lara-js/api/lara/Io.js";
import Strings from "lara-js/api/lara/Strings.js";
import JavaTypes, { JavaClasses } from "lara-js/api/lara/util/JavaTypes.js";
import Query from "lara-js/api/weaver/Query.js";
import { FileJp, FunctionJp } from "../../Joinpoints.js";
import Clava from "../Clava.js";
import CMaker from "../cmake/CMaker.js";
import { debug } from "lara-js/api/lara/core/LaraCore.js";

function GproferGetCxxFunction(signature: string) {
  return Query.search(FunctionJp, {
    signature: signature,
    hasDefinition: true,
  }).first();
}

function GproferGetCFunction(signature: string) {
  return Query.search(FunctionJp, {
    name: signature,
    hasDefinition: true,
  }).first();
}

interface GproferProfileFileContents {
  data: Record<string, Record<string, number>>;
  hotspots: Record<number, string>;
}

export default class Gprofer {
  private _runs: number;
  private _args: string[];

  private _app = Clava.getProgram();
  private _workingDir: JavaClasses.File | undefined = undefined;
  private _deleteWorkingDir: boolean = false;
  private _checkReturn: boolean = true;
  private _data: Record<string, Record<string, number>> = {};
  private _hotSpots: Record<number, string> = {};
  private _cmaker: CMaker = this.defaultCmaker();
  private _gProfer = JavaTypes.Gprofer;

  constructor(runs: number = 1, args: string[] = []) {
    this._runs = runs;
    this._args = args;
  }

  getCmaker(): CMaker {
    return this._cmaker;
  }

  static _EXE_NAME = "gprofer_bin";

  protected defaultCmaker(): CMaker {
    const cmaker = new CMaker(Gprofer._EXE_NAME, false);

    if (Clava.isCxx()) {
      cmaker.addCxxFlags("-no-pie", "-pg");
    } else {
      cmaker.addFlags("-no-pie", "-pg");
    }

    // sources
    for (const $jp of this._app.getDescendants("file")) {
      const $file = $jp as FileJp;
      if ($file.isHeader) {
        continue;
      }

      cmaker.getSources().addSource($file.filepath);
    }

    // includes
    for (const obj of Clava.getIncludeFolders()) {
      const userInclude = obj as string;
      debug("Adding include: " + userInclude);
      cmaker.addIncludeFolder(userInclude);
    }

    return cmaker;
  }

  protected static _buildName($function: FunctionJp): string {
    if (Clava.isCxx()) {
      /* signature or qualified name */
      return $function.signature;
    }

    return $function.name;
  }

  setArgs(args: string[]) {
    this._args = args;
    return this;
  }

  setRuns(runs: number) {
    this._runs = runs;
    return this;
  }

  setCheckReturn(checkReturn: boolean) {
    this._checkReturn = checkReturn;
    return this;
  }

  setWorkingDir(workingDir: string, deleteWorkingDir: boolean) {
    this._workingDir = Io.getPath(workingDir);
    this._deleteWorkingDir = deleteWorkingDir;
    return this;
  }

  profile() {
    if (this._workingDir === undefined) {
      this._workingDir = Io.getTempFolder("gprofer_" + Strings.uuid());
      this._deleteWorkingDir = true;
    }

    // compile the application
    const binary = this._cmaker.build(
      this._workingDir,
      Io.getPath(this._workingDir, "build")
    );

    // call java gprofer
    const data = this._gProfer.profile(
      binary,
      this._args,
      this._runs,
      this._workingDir,
      this._deleteWorkingDir,
      this._checkReturn
    );
    const json = this._gProfer.getJsonData(data) as string;

    // fill this._data and this._hotSpots
    const obj = JSON.parse(json);
    this._hotSpots = obj.hotspots;
    this._data = obj.table;

    return this;
  }

  getHotspotNames() {
    return this._hotSpots;
  }

  writeProfile(path: JavaClasses.File = Io.getPath("./gprofer.json")) {
    const profile: GproferProfileFileContents = {
      data: this._data,
      hotspots: this._hotSpots,
    };
    Io.writeJson(path.getAbsolutePath(), profile);
  }

  readProfile(path: JavaClasses.File = Io.getPath("./gprofer.json")) {
    const obj = Io.readJson(
      path.getAbsolutePath()
    ) as GproferProfileFileContents;
    this._data = obj.data;
    this._hotSpots = obj.hotspots;
  }

  /**
   *
   * May return undefined if the desired function is a system or library function and not available in the source code.
   * */
  getHotspot(rank: number = 0) {
    const _rank = rank;

    const signature = this._hotSpots[_rank];

    const f = this._getHotspot(signature);

    if (f === undefined) {
      console.log(
        `Could not find hotspot with rank ${rank} and signature ${signature}. It may be a system function.\n`
      );
    }

    return f;
  }

  /**
   * Internal method that uses the signature to identify a function.
   * */
  protected _getHotspot(signature: string): FunctionJp | undefined {
    let f = undefined;
    if (Clava.isCxx()) {
      debug("finding Cxx function with signature " + signature);
      f = GproferGetCxxFunction(signature);
    } else {
      debug("finding C function with signature " + signature);
      f = GproferGetCFunction(signature);
    }

    return f;
  }

  getPercentage($function: FunctionJp) {
    return this.get("percentage", $function);
  }

  getCalls($function: FunctionJp) {
    return this.get("calls", $function);
  }

  getSelfSeconds($function: FunctionJp) {
    return this.get("selfSeconds", $function);
  }

  getSelfMsCall($function: FunctionJp) {
    return this.get("selfMsCall", $function);
  }

  getTotalMsCall($function: FunctionJp) {
    return this.get("totalMsCall", $function);
  }

  protected get(type: string, $function: FunctionJp) {
    const name = Gprofer._buildName($function);
    return this._data[name][type];
  }

  print($function: FunctionJp) {
    const perc = this.getPercentage($function);
    const calls = this.getCalls($function);
    const self = this.getSelfSeconds($function);
    const selfCall = this.getSelfMsCall($function);
    const totalCall = this.getTotalMsCall($function);

    console.log($function.signature);

    if (perc !== undefined) {
      console.log(`\tPercentage: ${perc}%`);
    }

    if (calls !== undefined) {
      console.log(`\tCalls: ${calls}`);
    }

    if (self !== undefined) {
      console.log(`\tSelf seconds: ${self}s`);
    }

    if (selfCall !== undefined) {
      console.log(`\tSelf ms/call: ${selfCall}ms`);
    }

    if (totalCall !== undefined) {
      console.log(`\tTotal ms/call: ${totalCall}ms`);
    }
  }

  removeSystemFunctions() {
    const toRemove: number[] = [];

    for (const index in this._hotSpots) {
      const sigIndex: number = Number(index);

      const sig = this._hotSpots[sigIndex];
      const $hs = this._getHotspot(sig);

      if ($hs === undefined) {
        // mark to remove from array
        toRemove.push(sigIndex);

        // remove from map
        delete this._data[sig];
      }
    }

    // remove from array
    for (const sigIndex of toRemove) {
      delete this._hotSpots[sigIndex];
    }
  }
}
