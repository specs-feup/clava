laraImport("lara.Io");

laraImport("lara.benchmark.BenchmarkInstance");

laraImport("clava.Clava");
laraImport("clava.ClavaJoinPoints");

laraImport("weaver.Query");
laraImport("weaver.Weaver");

/**
 * Instance of a Clava benchmark.
 *
 * Implements _compilePrivate and .getKernel().
 */
class ClavaBenchmarkInstance extends BenchmarkInstance {
  constructor(name) {
    super(name);
    // this._cmaker = new CMaker(this.getName());
    this._isCachedAst = false;
  }

  /** INHERITANCE **/

  static _CACHE_ENABLE = false;

  /**
   * @param {boolean} enable - if true, enables caching of parsed files. By default, caching is enabled.
   */
  static setCache(enable) {
    ClavaBenchmarkInstance._CACHE_ENABLE = enable;
  }

  /**
   * @return temporary folder for caching ASTs.
   */
  static _getCacheFolder() {
    return Io.getTempFolder("clavaBenchmarkAsts");
  }

  /**
   * Clears compilation cache of all ClavaBenchmarkInstances.
   */
  static purgeCache() {
    Io.deleteFolderContents(ClavaBenchmarkInstance._getCacheFolder());
  }

  isCachedAst() {
    return this._isCachedAst;
  }

  /**
   * @return {J#java.io.File} the File representing the cached program of this BenchmarkInstance. The file might not exist.
   */
  _getCachedFile() {
    return Io.getPath(
      ClavaBenchmarkInstance._getCacheFolder(),
      `${this.getName()}.ast`
    );

    //return Io.isFile(cachedFile) ? cachedFile : undefined;
  }

  /**
   * The output folder for this BenchmarkInstance.
   */
  _getOutputFolder() {
    return Io.mkdir(this.getBaseFolder(), this.getName());
  }

  //   getCMaker() {
  //     return this._cmaker;
  //   }

  _loadCached(astFile) {
    println(`Loading cached AST from file ${astFile.getAbsolutePath()}...`);

    // Load saved AST
    var $app = Weaver.deserialize(Io.readFile(astFile));

    // Push loaded AST
    Clava.pushAst($app);

    // // Clean AST
    // Query.root().removeChildren();

    // // Add code
    // this._addCode();

    // // Set standard
    // this._previousStandard = Clava.getData().getStandard();
    // Clava.getData().setStandard("c99");

    // // Set define
    // this._previousFlags = Clava.getData().getFlags();
    // const modifiedFlags = `${this._previousFlags} -D${this._inputSize}_DATASET -DPOLYBENCH_TIME`;
    // Clava.getData().setFlags(modifiedFlags);

    // // Rebuild
    // Clava.rebuild();
  }

  _loadPrologue() {
    // by default, does nothing
  }

  /** IMPLEMENTATIONS **/

  //   close() {
  //     const result = super.close();

  //     // Clean CMaker
  //     this._cmaker = new CMaker(this.getName());

  //     return result;
  //   }

  /**
   *
   */
  _compilePrivate() {
    const folder = this._getOutputFolder();
    Clava.writeCode(folder);

    const cmaker = this.getCMaker();
    cmaker.addCurrentAst();

    const exe = cmaker.build(folder);
    this._setExecutable(exe);

    return exe;
  }

  /**
   * Looks for #pragma kernel, returns target of that pragma
   */
  getKernel() {
    const $pragma = Query.search("pragma", "kernel").first();

    if ($pragma === undefined) {
      throw `ClavaBenchmarkInstance.getKernel: Could not find '#pragma kernel' in benchmark ${this.getName()}`;
    }

    return $pragma.target;
  }

  /** RE-IMPLEMENTATIONS **/

  /**
   * Adds support for caching.
   */
  load() {
    // Check if already loaded
    if (this._hasLoaded) {
      // println(`BenchmarkInstance.load(): Benchmark ${this.getName()} is already loaded`);
      return;
    }

    // Execute things that are common to cache and AST parsing
    this._loadPrologue();

    let result = undefined;

    // Check if a chached version of the tree has already been cached
    var cachedFile = this._getCachedFile();
    if (Io.isFile(cachedFile)) {
      // Load cached AST
      result = this._loadCached(cachedFile);
      this._isCachedAst = true;
    } else {
      println(`Parsing ${this.getName()}...`);
      result = this._loadPrivate();

      // If caching enabled, save AST
      if (ClavaBenchmarkInstance._CACHE_ENABLE) {
        println(`Saving AST to file ${cachedFile.getAbsolutePath()}...`);
        const serialized = Weaver.serialize(Query.root());
        Io.writeFile(cachedFile, serialized);
      }

      this._isCachedAst = false;
    }

    // Mark as loaded
    this._hasLoaded = true;

    return result;
  }
}
