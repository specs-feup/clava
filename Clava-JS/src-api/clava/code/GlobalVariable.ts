import { FileJp, Joinpoint, Type, Varref } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";

/**
 * Adds and manages global variables.
 */
export default class GlobalVariable {
  private _filesWithGlobal: Set<string> = new Set<string>();
  private _varName: string;
  private _$type: Type;
  private _initValue: string;
  
  constructor(varName: string, $type: Type, initValue: string) {
    this._varName = varName;
    this._$type = $type;
    this._initValue = initValue;
  }

  /**
   * @returns A reference to the global variable defined by this object.
   */
  getRef($reference: Joinpoint): Varref | undefined {
    // Check file for the reference point
    const $file = $reference.getAncestor("file") as FileJp | undefined;
    if ($file === undefined) {
      console.log(
        `GlobalVariable.getRef: Could not find the file for the reference point ${$reference.location}`
      );
      return undefined;
    }

    // Check if file already has this global variable declared
    const fileId = $file.jpId;
    if (!this._filesWithGlobal.has(fileId)) {
      this._filesWithGlobal.add(fileId);
      $file.addGlobal(this._varName, this._$type, this._initValue);
    }

    // Create varref
    return ClavaJoinPoints.varRef(this._varName, this._$type);
  }
}
