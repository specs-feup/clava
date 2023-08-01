laraImport("clava.Clava");

laraImport("lara.util.LineInserter");
laraImport("lara.Io");

/**
 * Writes the original code of the application, with the possibility of inserting new lines of code.
 *
 * @class
 */
class CodeInserter {
  // Maps $file AST ids to an object that maps line numbers to strings to insert.
  _linesToInserts;

  constructor() {
    this._linesToInserts = {};
  }

  add($file, line, content) {
    var astId = $file.astId;
    var fileLines = this._getFileLines(astId);
    this._addContent(fileLines, line, content);
  }

  /**
   * Writes the code of the current tree, plus the lines to insert, to the given folder.
   */
  write(outputFolder) {
    var lineInserter = new LineInserter();

    // Write each file, inserting lines if needed
    for (var $file of Clava.getProgram().getDescendants("file")) {
      if (!Io.isFile($file.filepath)) {
        println(
          "CodeInserter.write: skipping file, could not find path '" +
            $file.filepath +
            "'"
        );
        continue;
      }

      // Original code
      var fileCode = Io.readFile($file.filepath);

      // Intrument code, if needed
      var fileLines = this._linesToInserts[$file.astId];
      if (fileLines !== undefined) {
        fileCode = lineInserter.add(fileCode, fileLines);
      }

      // Get path for writing file
      var outputFilepath = $file.destinationFilepath(outputFolder);

      // Write file
      Io.writeFile(outputFilepath, fileCode);
    }
  }

  /*** PRIVATE FUNCTIONS ***/

  _getFileLines(astId) {
    var fileLines = this._linesToInserts[astId];

    if (fileLines === undefined) {
      fileLines = {};
      this._linesToInserts[astId] = fileLines;
    }

    return fileLines;
  }

  _addContent(fileLines, line, content) {
    var lineStrings = fileLines[line];
    if (lineStrings === undefined) {
      lineStrings = [];
      fileLines[line] = lineStrings;
    }

    lineStrings.push(content);
  }
}
