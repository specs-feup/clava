laraImport("weaver.util.WeaverDataStore");

/**
 * DataStore used in Clava.
 */
class ClavaDataStore extends WeaverDataStore {
  constructor(data) {
    super(
      data,
      Java.type("pt.up.fe.specs.clava.weaver.CxxWeaver").getWeaverDefinition()
    );
    // Add alias
    this.addAlias("Disable Clava Info", "disable_info");
  }

  /*** PRIVATE OVERRIDABLE FUNCTIONS ***/

  /**
   * Wraps a Java DataStore around a Lara DataStore.
   */
  _dataStoreWrapper(javaDataStore) {
    return new ClavaDataStore(javaDataStore, this.definition);
  }

  /*** NEW CLAVA_DATA_STORE FUNCTIONS ***/

  /**
   * @return {string} a string with the current C/C++ compiler flags.
   */
  getFlags() {
    return this.get("Compiler Flags");
  }

  /**
   * @param {string} flags - A string with C/C++ compiler flags.
   *
   */
  setFlags(flags) {
    checkString(flags);

    this.put("Compiler Flags", flags);
  }

  /**
   * @return {J#java.util.List<String>} A list with the current extra system includes.
   */
  getSystemIncludes() {
    return this.get("library includes").getFiles();
  }

  /**
   * @return {J#java.util.List<String>} A list with the current user includes.
   */
  getUserIncludes() {
    return this.get("header includes").getFiles();
  }

  /**
   * @param {string...} includes - A variable number of strings with the extra system includes.
   *
   */
  setSystemIncludes = function (...includes) {
    const files = flattenArgsArray(includes).map((include) =>
      Io.getPath(include)
    );

    const fileList = Java.type(
      "org.lara.interpreter.joptions.keys.FileList"
    ).newInstance(files);
    this.put("library includes", fileList);
  };

  /**
   * @param {string...} includes - A variable number of strings with the user includes.
   *
   */
  setUserIncludes(...includes) {
    const files = flattenArgsArray(includes).map((include) =>
      Io.getPath(include)
    );

    const fileList = Java.type(
      "org.lara.interpreter.joptions.keys.FileList"
    ).newInstance(files);
    this.put("header includes", fileList);
  }

  /**
   * @return {string} a string with the current compilation standard.
   */
  getStandard() {
    return this.get("C/C++ Standard").toString();
  }

  /**
   * @param {string} flags - A string with a C/C++/OpenCL compilation standard.
   *
   */
  setStandard(standard) {
    checkString(standard);

    const stdObject = Java.type("pt.up.fe.specs.clava.language.Standard")
      .getEnumHelper()
      .fromValue(standard);
    this.put("C/C++ Standard", stdObject);
  }
}
