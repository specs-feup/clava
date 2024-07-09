import Io from "lara-js/api/lara/Io.js";
import Check from "lara-js/api/lara/Check.js";
import System from "lara-js/api/lara/System.js";
import Strings from "lara-js/api/lara/Strings.js";
import Clava from "../Clava.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import { JavaClasses } from "lara-js/api/lara/util/JavaTypes.js";
import { debug } from "lara-js/api/lara/core/LaraCore.js";
import { ClavaException, FileJp } from "../../Joinpoints.js";

/**
 * Parses C/C++ files.
 */
export default class BatchParser {
  private basePath: string;
  /**
   * The source files found on the given path
   */
  private sourceFiles: JavaClasses.File[];
  /**
   * Maps header file names to the corresponding File objects
   */
  private headerFilesMap: Map<string, JavaClasses.File> = new Map();

  private static _IMPLEMENTATION_PATTERNS = ["*.c", "*.cpp"];
  private static _HEADER_PATTERNS = ["*.h", "*.hpp"];

  constructor(srcPath: string) {
    this.basePath = srcPath;

    this.sourceFiles = Io.getFiles(
      srcPath,
      BatchParser._IMPLEMENTATION_PATTERNS,
      true
    );

    const headerFiles = Io.getFiles(
      srcPath,
      BatchParser._HEADER_PATTERNS,
      true
    );
    for (const headerFile of headerFiles) {
      this.headerFilesMap.set(headerFile.getName() as string, headerFile);
    }
  }

  getSourceFiles() {
    return this.sourceFiles;
  }

  parse(sourceFile: string | JavaClasses.File) {
    debug("Parsing " + sourceFile.toString() + "...");
    const parsingStart = System.nanos();

    const $literalFile = Clava.getProgram().addFileFromPath(
      sourceFile as object
    ) as FileJp;

    // Rebuild tree
    const $parsedFile = this.rebuildFile($literalFile);

    const parsingTime = System.toc(parsingStart);
    debug("Parsing took " + parsingTime);

    return $parsedFile;
  }

  /**
   * Tries to rebuild the current tree, using several methods to fix any problem it finds
   */
  private rebuildFile($literalFile: FileJp) {
    let parsing: boolean | undefined = true;
    while (parsing) {
      const $parsedFile = $literalFile.rebuildTry() as FileJp | ClavaException;

      // Check if it is a file
      if ($parsedFile instanceof FileJp) {
        return $parsedFile;
      }

      // It is an exception
      parsing = this.solveRebuildFile($parsedFile, $literalFile);
    }

    return undefined;
  }

  private solveRebuildFile($exception: ClavaException, $literalFile: FileJp) {
    // Get error message
    const message = $exception.message;

    // Check if correct type
    if ($exception.exceptionType !== "ClavaParserException") {
      throw $exception.exception;
    }

    const lines = Strings.asLines(message);

    // Check first line
    if (lines === undefined || lines.length === 0) {
      throw new Error("Could not parse error message: " + message);
    }

    Check.strings(lines[0], "There are errors in the source code:");

    // Parse first error
    return this.parseError(lines.subList(1, lines.length), $literalFile);
  }

  private parseError(lines: JavaClasses.List<string>, $literalFile: FileJp) {
    const errorLine: string = lines[0];
    const filename = $literalFile.name;

    // Find name of file in line
    const nameIndex = errorLine.indexOf(filename);
    if (nameIndex === -1) {
      throw "Could not find filename '" + filename + "' in " + errorLine;
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
      return this.parseFatalError(parsedLine);
    }

    console.log("Line 0: " + lines[0]);
    console.log("Parsed line: " + parsedLine);
    return false;
  }

  private parseFatalError(error: string) {
    if (error.endsWith("' file not found")) {
      const fileNotFound = "' file not found";
      const endIndex = error.length - fileNotFound.length;
      let parsedError = error.substring(0, endIndex).trim();
      if (!parsedError.startsWith("'")) {
        throw "Expected file not found string to start with ':" + parsedError;
      }
      parsedError = parsedError.substring(1, parsedError.length).trim();

      // Normalize
      parsedError.replace("\\\\", "/");

      let filename = parsedError;
      let path = undefined;

      // Extract path
      const slashIndex = parsedError.lastIndexOf("/");
      if (slashIndex !== -1) {
        filename = parsedError.substring(slashIndex + 1);
        path = parsedError.substring(0, slashIndex);
      }

      console.log("File: " + filename);
      console.log("Path: " + path);

      let pathname = filename;
      if (path !== undefined) {
        pathname = path + "/" + filename;
      }
      debug("Adding file " + pathname);
      const newFile = ClavaJoinPoints.file(filename, path);
      Clava.getProgram().addFile(newFile);

      return true;
    }
  }
}
