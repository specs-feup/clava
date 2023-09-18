import Io from "lara-js/api/lara/Io.js";
import LineInserter from "lara-js/api/lara/util/LineInserter.js";
import Clava from "../Clava.js";
/**
 * Writes the original code of the application, with the possibility of inserting new lines of code.
 */
export default class CodeInserter {
    /**
     * Maps $file AST ids to an object that maps line numbers to strings to insert.
     */
    linesToInserts;
    constructor() {
        this.linesToInserts = {};
    }
    add($file, line, content) {
        const astId = $file.astId;
        const fileLines = this.getFileLines(astId);
        this.addContent(fileLines, line, content);
    }
    /**
     * Writes the code of the current tree, plus the lines to insert, to the given folder.
     */
    write(outputFolder) {
        const lineInserter = new LineInserter();
        // Write each file, inserting lines if needed
        for (const $jp of Clava.getProgram().getDescendants("file")) {
            const $file = $jp;
            if (!Io.isFile($file.filepath)) {
                console.log("CodeInserter.write: skipping file, could not find path '" +
                    $file.filepath +
                    "'");
                continue;
            }
            // Original code
            let fileCode = Io.readFile($file.filepath);
            // Intrument code, if needed
            const fileLines = this.linesToInserts[$file.astId];
            if (fileLines !== undefined) {
                fileCode = lineInserter.add(fileCode, fileLines);
            }
            // Get path for writing file
            const outputFilepath = $file.getDestinationFilepath(outputFolder);
            Io.writeFile(outputFilepath, fileCode);
        }
    }
    getFileLines(astId) {
        let fileLines = this.linesToInserts[astId];
        if (fileLines === undefined) {
            fileLines = {};
            this.linesToInserts[astId] = fileLines;
        }
        return fileLines;
    }
    addContent(fileLines, line, content) {
        const lineStrings = fileLines[line];
        if (lineStrings === undefined) {
            fileLines[line] = [];
        }
        fileLines[line].push(content);
    }
}
//# sourceMappingURL=CodeInserter.js.map