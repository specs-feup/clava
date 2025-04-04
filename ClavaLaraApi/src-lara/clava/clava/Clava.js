import { wrapJoinPoint } from "@specs-feup/lara/api/LaraJoinPoint.js";
import Io from "@specs-feup/lara/api/lara/Io.js";
import JavaInterop from "@specs-feup/lara/api/lara/JavaInterop.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import Weaver from "@specs-feup/lara/api/weaver/Weaver.js";
import WeaverOptions from "@specs-feup/lara/api/weaver/WeaverOptions.js";
import ClavaJavaTypes from "./ClavaJavaTypes.js";
import ClavaDataStore from "./util/ClavaDataStore.js";
export default class Clava {
    /**
     * Enables/disables library SpecsLogger for printing.
     * <p>
     * By default, is disabled.
     */
    static useSpecsLogger = false;
    /**
     * Returns the standard being used for compilation.
     */
    static getStandard() {
        return Clava.getProgram().standard;
    }
    static isCxx() {
        return Clava.getProgram().isCxx;
    }
    static rebuild() {
        return Clava.getProgram().rebuild();
    }
    static rebuildFuzzy() {
        Clava.getProgram().rebuildFuzzy();
    }
    /**
     * @returns The folder of the first input source element, either itself, if a folder, or the parent folder, if it is a file.
     */
    static getBaseFolder() {
        return Clava.getProgram().baseFolder;
    }
    /**
     * @returns The folder where the code represented by the AST will be written at the end of execution.
     */
    static getWeavingFolder() {
        return Clava.getProgram().weavingFolder;
    }
    /**
     * @param $file - The file to add to the AST.
     */
    static addFile($file) {
        Clava.getProgram().addFile($file);
    }
    /**
     * @param path - Path to an existing source file that will be added to the AST. If the file does not exists, throws an exception.
     */
    static addExistingFile(path) {
        const file = Io.getPath(path);
        if (!file.isFile()) {
            throw new Error("Clava.addExistingFile(): path " +
                path.toString() +
                " does not represent an existing file");
        }
        const $file = wrapJoinPoint(ClavaJavaTypes.AstFactory.file(file.getAbsolutePath(), ""));
        Clava.addFile($file);
    }
    static cLinkageBegin = `#ifdef __cplusplus
extern "C" {
#endif`;
    static cLinkageEnd = `#ifdef __cplusplus
}
#endif`;
    /**
     * Launches a Clava weaving session.
     * @param args - The arguments to pass to the weaver, as if it was launched from the command-line
     * @returns True if the weaver execution without problems, false otherwise
     */
    static runClava(args) {
        // If string, separate arguments
        if (typeof args === "string") {
            args = ClavaJavaTypes.ArgumentsParser.newCommandLine().parse(args);
        }
        return ClavaJavaTypes.ClavaWeaverLauncher.execute(args);
    }
    /**
     * Launches several Clava weaving sessions in parallel.
     *
     * @param argsLists - An array where each element is an array with the arguments to pass to the weaver, as if it was launched from the command-line
     * @param threads - Number of threads to use
     * @param clavaCommand -  The command we should use to call Clava (e.g., /usr/local/bin/clava)
     *
     * @returns The results of each execution
     */
    static runClavaParallel(argsLists, threads = -1, clavaCommand = ["clava"]) {
        if (typeof clavaCommand === "string") {
            clavaCommand = [clavaCommand];
        }
        const jsonStrings = ClavaJavaTypes.ClavaWeaverLauncher.executeParallel(argsLists, threads, JavaInterop.arrayToStringList(clavaCommand), Clava.getData().getContextFolder().getAbsolutePath());
        // Read each json file into its own object
        const results = jsonStrings.map((jsonString) => JSON.parse(jsonString));
        return results;
    }
    /**
     * Creates a clone of the current AST and pushes the clone to the top of the current AST stack. If a $program join point is passed, that join point is added to the top of the stack instead.
     *
     * @param $program - program to push to the AST.
     */
    static pushAst($program) {
        if ($program === undefined) {
            Clava.getProgram().push();
            return;
        }
        Weaver.getWeaverEngine().pushAst($program.node);
    }
    /**
     * Discards the AST at the top of the current AST stack.
     */
    static popAst() {
        Clava.getProgram().pop();
    }
    /**
     * Clears all ASTs except for the one at the top of the stack. If there is one of none AST on the stack does nothing.
     */
    static clearAstHistory() {
        Weaver.getWeaverEngine().clearAppHistory();
    }
    /**
     * The current number of elements in the AST stack.
     */
    static getStackSize() {
        return Weaver.getWeaverEngine().getStackSize();
    }
    /**
     * Looks for a join point in the current AST.
     *
     * @param $jp - A join point from any AST
     * @returns The equivalent join point from the AST at the top of the current AST stack
     */
    static findJp($jp) {
        // Get file
        const $file = $jp.getAncestor("file");
        if ($file === undefined) {
            console.error("Could not find a file for '" + $jp.joinPointType + "'", "Clava.findJp");
            return undefined;
        }
        const $newJp = ClavaJavaTypes.CxxWeaverApi.findJp($file.filepath, $jp.astId);
        if ($newJp === null) {
            console.error("Could not find the given '" +
                $jp.joinPointType +
                "' in the current AST. Please verify if a rebuild was done", "Clava.findJp");
            return undefined;
        }
        return $newJp;
    }
    /**
     * Writes the code of the current AST to the given folder.
     */
    static writeCode(outputFoldername) {
        const outputFolder = Io.mkdir(outputFoldername);
        ClavaJavaTypes.CxxWeaverApi.writeCode(outputFolder);
        return outputFolder;
    }
    /**
     * @returns DataStore with the data of the current weaver
     */
    static getData() {
        return new ClavaDataStore(WeaverOptions.getData());
    }
    /**
     * @returns The join point $program.
     */
    static getProgram() {
        return Query.root();
    }
    /**
     *
     * @returns A list of join points representing available user includes
     */
    static getAvailableIncludes() {
        return ClavaJavaTypes.CxxWeaverApi.getAvailableUserIncludes();
    }
    /**
     *
     * @returns {J#Set<String>} A set with paths to the include folders of the current configuration.
     */
    static getIncludeFolders() {
        return ClavaJavaTypes.CxxWeaverApi.getIncludeFolders();
    }
}
//# sourceMappingURL=Clava.js.map