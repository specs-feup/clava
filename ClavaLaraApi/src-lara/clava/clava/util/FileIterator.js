import Io from "lara-js/api/lara/Io.js";
import { debug } from "lara-js/api/lara/core/LaraCore.js";
import Clava from "../Clava.js";
/**
 * Given a folder, collects sources in that folder, parses and returns one each time next() is called.
 *
 * Pushes an empty Clava AST. Parsed files are added one at a time, and the AST contains at most one file at any given time.
 *
 * @param srcFoldername - Name of the folder with the source files to iterate.
 * @param sourceExt - Extensions of the source files.
 * @param headerExt - Extensions of the header files.
 */
export default class FileIterator {
    files = [];
    currentFile = 0;
    isInit = false;
    isClosed = false;
    pushedAst = false;
    srcFoldername;
    sourceExt;
    headerExt;
    constructor(srcFoldername, sourceExt = ["c", "cpp"], headerExt = ["h", "hpp"]) {
        this.srcFoldername = srcFoldername;
        this.sourceExt = sourceExt;
        this.headerExt = headerExt;
    }
    /**
     * @returns $file join point, if there are still files to iterate over, or undefined otherwise
     */
    next() {
        // Initialized, in case it has not been initialized yet
        this.init();
        // Check if finished
        if (!this.hasNext()) {
            return undefined;
        }
        // Next file
        const sourceFile = this.files[this.currentFile];
        // Increment
        this.currentFile++;
        debug("FileIterator.next: Processing file " + sourceFile.toString());
        // Ensure program tree is empty before adding file
        Clava.getProgram().removeChildren();
        Clava.getProgram().addFileFromPath(sourceFile);
        // Rebuild
        Clava.rebuild();
        const $firstChild = Clava.getProgram().getDescendants("file")?.[0];
        return $firstChild;
    }
    /**
     * @returns True if there are still files to iterate over, false otherwise.
     */
    hasNext() {
        // Init, if not yet initalized
        this.init();
        if (this.currentFile < this.files.length) {
            return true;
        }
        // Close, if not yet closed
        this.close();
        return false;
    }
    init() {
        if (this.isInit) {
            // Already initialized
            return;
        }
        this.isInit = true;
        const srcFolder = Io.getPath(Clava.getData().getContextFolder(), this.srcFoldername);
        this.addIncludes(srcFolder, this.headerExt);
        this.files = this.getFiles(srcFolder, this.sourceExt);
        // Sort files
        this.files.sort();
        debug("FileIterator: found " + this.files.length + " files");
        // Work on new AST tree
        Clava.pushAst();
        this.pushedAst = true;
    }
    close() {
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
     *
     */
    addIncludes(srcFolder, headerExt) {
        // TODO: If needed, add a 'nestingLevel' parameter, which includes up to X ancestors for each header,
        // cutting off if ancestors go before srcFolder
        // Current user includes
        const data = Clava.getData();
        const userIncludes = data.getUserIncludes();
        debug("FileIterator._addIncludes: User includes before " +
            userIncludes.join(", "));
        // Populate initial set with user includes
        const parents = new Set();
        for (const userInclude of userIncludes) {
            parents.add(Io.getAbsolutePath(userInclude));
        }
        // Get folders of hFiles
        const headerFiles = this.getFiles(srcFolder, headerExt);
        for (const hFile of headerFiles) {
            parents.add(Io.getAbsolutePath(hFile.getParentFile()));
        }
        // Build new value
        const includeFolders = [];
        for (const parent of parents.values()) {
            // Converting to File
            includeFolders.push(Io.getPath(parent));
        }
        data.setUserIncludes(...includeFolders.map((folder) => folder.getAbsolutePath()));
        debug("FileIterator._addIncludes: User includes after " +
            data.getUserIncludes().join(", "));
    }
    getFiles(folder, extensions) {
        let files = [];
        for (const extension of extensions) {
            const sourceFiles = Io.getFiles(folder, "*." + extension, true);
            files = files.concat(sourceFiles);
        }
        return files;
    }
}
//# sourceMappingURL=FileIterator.js.map