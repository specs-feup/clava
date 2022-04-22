laraImport("clava.ClavaJoinPoints");

/**
 * Adds and manages global variables.
 */
class GlobalVariable {
  constructor(varName, $type, initValue) {
    this._filesWithGlobal = new StringSet();
    this._varName = varName;
    this._$type = $type;
    this._initValue = initValue.toString();
  }

  /**
   * @return {$varref} - A reference to the global variable defined by this object.
   */
  getRef($reference) {
    // Check file for the reference point
    const $file = $reference.ancestor("file");
    if ($file === undefined) {
      println(
        "GlobalVariable.getRef: Could not find the file for the reference point " +
          $reference.location
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
