import { FileJp, Joinpoint, Type, Varref } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";

/**
 * Adds and manages global variables.
 */
export default class GlobalVariable {
  private filesWithGlobal: Set<string> = new Set<string>();
  private varName: string;
  private $type: Type;
  private initValue: string;
  
  constructor(varName: string, $type: Type, initValue: string) {
    this.varName = varName;
    this.$type = $type;
    this.initValue = initValue;
  }

  /**
   * @returns A reference to the global variable defined by this object.
   */
  getRef($reference: Joinpoint): Varref {
    // Check file for the reference point
    const $file = $reference.getAncestor("file") as FileJp | undefined;
    if ($file === undefined) {
      console.log(
        `GlobalVariable.getRef: Could not find the file for the reference point ${$reference.location}`
      );
    } else {
      // Check if file already has this global variable declared
      const fileId = $file.jpId;
      if (!this.filesWithGlobal.has(fileId)) {
        this.filesWithGlobal.add(fileId);
        $file.addGlobal(this.varName, this.$type, this.initValue);
      }
    }


    // Create varref
    return ClavaJoinPoints.varRef(this.varName, this.$type);
  }
}
