laraImport("lara.Check");
laraImport("lara.Io");
laraImport("lara.util.StringSet");

laraImport("clava.Clava");

/**
 * Given a folder, collects sources in that folder, parses and returns one each time next() is called.
 *
 * Pushes an empty Clava AST. Parsed files are added one at a time, and the AST contains at most one file at any given time.
 */
class FileIterator {
  /**
   * @param {string} srcFoldername name of the folder with the source files to iterate.
   * @param {string[]} [sourceExt=["c", "cpp"]] extensions of the source files.
   * @param {string[]} [headerExt=["h", "hpp"]] extensions of the header files.
   */
  constructor(srcFoldername, sourceExt, headerExt) {
    Check.isDefined(srcFoldername, "srcFoldername");

    this.srcFoldername = srcFoldername;
    this.sourceExt = sourceExt ?? ["c", "cpp"];
    this.headerExt = headerExt ?? ["h", "hpp"];

    this.files = [];
    this.currentFile = 0;
    this.isInit = false;
    this.isClosed = false;
    this.pushedAst = false;
  }

  /**
   * @return $file join point, if there are still files to iterate over, or undefined otherwise
   */
  next() {
    // Initialized, in case it has not been initialized yet
    this._init();

    // Check if finished
    if (!this.hasNext()) {
      return undefined;
    }

    // Next file
    const sourceFile = this.files[this.currentFile];

    // Increment
    this.currentFile++;

    debug("FileIterator.next: Processing file " + sourceFile);

    // Ensure program tree is empty before adding file
    Clava.getProgram().removeChildren();

    Clava.getProgram().addFileFromPath(sourceFile);

    // Rebuild
    Clava.rebuild();

    return Clava.getProgram().firstChild;
  }

  /**
   * @return {boolean} true if there are still files to iterate over, false otherwise.
   */
  hasNext() {
    // Init, if not yet initalized
    this._init();

    if (this.currentFile < this.files.length) {
      return true;
    }

    // Close, if not yet closed
    this._close();

    return false;
  }

  _init() {
    if (this.isInit) {
      // Already initialized
      return;
    }

    this.isInit = true;

    const srcFolder = Io.getPath(
      Clava.getData().getContextFolder(),
      this.srcFoldername
    );

    this._addIncludes(srcFolder, this.headerExt);

    this.files = this._getFiles(srcFolder, this.sourceExt);

    // Sort files
    this.files.sort();

    debug("FileIterator: found " + this.files.length + " files");

    // Work on new AST tree
    Clava.pushAst();
    this.pushedAst = true;
  }

  _close() {
    if (this.isClosed) {
      return;
    }

    this.isClosed = true;

    // Recover previous AST
    if (this.pushedAst) {
      Clava.popAst();
    }
  }

  /**
   * Attempts to add folders of header files as includes.
   */
  _addIncludes(srcFolder, headerExt) {
    // TODO: If needed, add a 'nestingLevel' parameter, which includes up to X ancestors for each header,
    // cutting off if ancestors go before srcFolder

    // Current user includes
    const data = Clava.getData();
    const userIncludes = data.getUserIncludes();
    debug("FileIterator._addIncludes: User includes before " + userIncludes);

    // Populate initial set with user includes
    const parents = new StringSet();

    for (const userInclude of userIncludes) {
      parents.add(Io.getAbsolutePath(userInclude));
    }

    // Get folders of hFiles
    const headerFiles = this._getFiles(srcFolder, headerExt);

    for (const hFile of headerFiles) {
      parents.add(Io.getAbsolutePath(hFile.getParentFile()));
    }

    // Build new value
    const includeFolders = [];
    for (const parent of parents.values()) {
      // Converting to File
      includeFolders.push(Io.getPath(parent));
    }
    data.setUserIncludes(includeFolders);
    debug(
      `FileIterator._addIncludes: User includes after ${data.getUserIncludes()}`
    );
  }

  _getFiles(folder, extensions) {
    let files = [];

    for (const extension of extensions) {
      const sourceFiles = Io.getFiles(folder, `*.${extension}`, true);
      files = files.concat(sourceFiles);
    }

    return files;
  }
}
