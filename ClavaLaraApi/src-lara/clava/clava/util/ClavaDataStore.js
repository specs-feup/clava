import WeaverDataStore from "lara-js/api/weaver/util/WeaverDataStore.js";
import ClavaJavaTypes from "../ClavaJavaTypes.js";
import JavaTypes from "lara-js/api/lara/util/JavaTypes.js";
import Io from "lara-js/api/lara/Io.js";
import { arrayFromArgs } from "lara-js/api/lara/core/LaraCore.js";
/**
 * DataStore used in Clava.
 *
 */
export default class ClavaDataStore extends WeaverDataStore {
    constructor(data = "LaraI Options", definition = ClavaJavaTypes.CxxWeaver.getWeaverDefinition()) {
        super(data, definition);
        this.addAlias("Disable Clava Info", "disable_info");
    }
    /**
     * Wraps a Java DataStore around a Lara DataStore.
     */
    dataStoreWrapper(javaDataStore) {
        return new ClavaDataStore(javaDataStore, this.definition);
    }
    /**
     * @returns A string with the current C/C++ compiler flags.
     */
    getFlags() {
        return this.get("Compiler Flags");
    }
    /**
     * @param flags - A string with C/C++ compiler flags.
     *
     */
    setFlags(flags) {
        this.put("Compiler Flags", flags);
    }
    /**
     * @returns A list with the current extra system includes.
     */
    getSystemIncludes() {
        return this.get("library includes").getFiles();
    }
    /**
     * @returns A list with the current user includes.
     */
    getUserIncludes() {
        return this.get("header includes").getFiles();
    }
    /**
     * @param arguments - A variable number of strings with the extra system includes.
     *
     */
    setSystemIncludes(...args) {
        const filenames = arrayFromArgs(args);
        const files = filenames.map((filename) => Io.getPath(filename));
        this.put("library includes", JavaTypes.FileList.newInstance(files));
    }
    /**
     * @param arguments - A variable number of strings with the user includes.
     *
     */
    setUserIncludes(...args) {
        const filenames = arrayFromArgs(args);
        const files = filenames.map((filename) => Io.getPath(filename));
        this.put("header includes", JavaTypes.FileList.newInstance(files));
    }
    /**
     * @returns A string with the current compilation standard.
     */
    getStandard() {
        return this.get("C/C++ Standard").toString();
    }
    /**
     * @param flags - A string with a C/C++/OpenCL compilation standard.
     *
     */
    setStandard(standard) {
        const stdObject = ClavaJavaTypes.Standard.getEnumHelper().fromValue(standard);
        this.put("C/C++ Standard", stdObject);
    }
}
//# sourceMappingURL=ClavaDataStore.js.map