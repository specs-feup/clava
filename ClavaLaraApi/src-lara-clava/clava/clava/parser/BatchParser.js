laraImport("lara.Io");
laraImport("lara.Check");
laraImport("lara.System");
laraImport("lara.Strings");

laraImport("weaver.WeaverJps");

laraImport("clava.Clava");
laraImport("clava.ClavaJoinPoints");

/**
 * Parses C/C++ files.
 */
class BatchParser {
  constructor(srcPath) {
    Check.isDefined(srcPath, "srcPath");

    this.basePath = srcPath;

    // The source files found on the given path
    this.sourceFiles = Io.getFiles(
      srcPath,
      BatchParser._IMPLEMENTATION_PATTERNS,
      true
    );

    // Maps header file names to the corresponding File objects
    this.headerFilesMap = {};

    const headerFiles = Io.getFiles(
      srcPath,
      BatchParser._HEADER_PATTERNS,
      true
    );
    for (const headerFile of headerFiles) {
      this.headerFilesMap[headerFile.getName()] = headerFile;
    }
  }

  static _IMPLEMENTATION_PATTERNS = ["*.c", "*.cpp"];
  static _HEADER_PATTERNS = ["*.h", "*.hpp"];

  getSourceFiles() {
    return this.sourceFiles;
  }

  parse(sourceFile) {
    debug(`Parsing ${sourceFile}...`);
    const parsingStart = System.nanos();

    const $literalFile = Clava.getProgram().addFileFromPath(sourceFile);

    // Rebuild tree
    $parsedFile = this._rebuildFile($literalFile);

    const parsingTime = System.toc(parsingStart);
    debug(`Parsing took ${parsingTime}`);

    return $parsedFile;
  }

  /**
   * Tries to rebuild the current tree, using several methods to fix any problem it finds
   */
  _rebuildFile($literalFile) {
    let parsing = true;
    while (parsing) {
      $parsedFile = $literalFile.rebuildTry();

      // Check if it is a file
      if ($parsedFile.instanceOf("file")) {
        return $parsedFile;
      }

      // It is an exception
      parsing = this._solveRebuildFile($parsedFile, $literalFile);
    }

    return undefined;
  }

  _solveRebuildFile($exception, $literalFile) {
    // Get error message
    const message = $exception.message;

    // Check if correct type
    if ($exception.exceptionType !== "ClavaParserException") {
      throw $exception.exception;
    }

    const lines = Strings.asLines(message);

    // Check first line
    Check.isTrue(lines.length > 0);
    Check.strings(lines[0], "There are errors in the source code:");

    // Parse first error
    return this._parseError(lines.subList(1, lines.length), $literalFile);
  }

  _parseError(lines, $literalFile) {
    const errorLine = lines[0];
    const filename = $literalFile.name;

    // Find name of file in line
    const nameIndex = errorLine.indexOf(filename);
    if (nameIndex === -1) {
      throw `Could not find filename '${filename}' in ${errorLine}`;
    }

    // Remove filename
    let parsedLine = errorLine
      .substring(nameIndex + filename.length, errorLine.length)
      .trim();

    // Remove location
    const locationSep = parsedLine.indexOf(" ");
    if (locationSep !== -1) {
      parsedLine = parsedLine
        .substring(locationSep + 1, parsedLine.length)
        .trim();
    }

    // Check if fatal error
    if (parsedLine.startsWith("fatal error:")) {
      const fatalError = "fatal error:";
      parsedLine = parsedLine
        .substring(fatalError.length, parsedLine.length)
        .trim();
      return this._parseFatalError(parsedLine);
    }

    println("Line 0: " + lines[0]);
    println("Parsed line: " + parsedLine);
    return false;
  }

  _parseFatalError(error) {
    if (error.endsWith("' file not found")) {
      const fileNotFound = "' file not found";
      const endIndex = error.length - fileNotFound.length;
      let parsedError = error.substring(0, endIndex).trim();
      if (!parsedError.startsWith("'")) {
        throw `Expected file not found string to start with ':${parsedError}`;
      }
      parsedError = parsedError.substring(1, parsedError.length).trim();

      // Normalize
      Strings.replacer(parsedError, "\\\\", "/");

      let filename = parsedError;
      let path;

      // Extract path
      const slashIndex = parsedError.lastIndexOf("/");
      if (slashIndex !== -1) {
        filename = parsedError.substring(slashIndex + 1);
        path = parsedError.substring(0, slashIndex);
      }

      println(`File: ${filename}`);
      println(`Path: ${path}`);

      let pathname = filename;
      if (path !== undefined) {
        pathname = `${path}/${filename}`;
      }
      debug(`Adding file ${pathname}`);
      const newFile = ClavaJoinPoints.file(filename, path);
      Clava.getProgram().addFile(newFile);

      return true;
    }
  }
}
