import WeaverDataStore from "lara-js/api/weaver/util/WeaverDataStore.js";
import ClavaJavaTypes from "../ClavaJavaTypes.js";
import JavaTypes, { JavaClasses } from "lara-js/api/lara/util/JavaTypes.js";
import Io from "lara-js/api/lara/Io.js";
import DataStore from "lara-js/api/lara/util/DataStore.js";
import { arrayFromArgs } from "lara-js/api/lara/core/LaraCore.js";

/**
 * DataStore used in Clava.
 *
 */
export default class ClavaDataStore extends WeaverDataStore {
  constructor(
    data: string | DataStore | JavaClasses.DataStore = "LaraI Options",
    definition: any = ClavaJavaTypes.CxxWeaver.getWeaverDefinition()
  ) {
    super(data, definition);

    this.addAlias("Disable Clava Info", "disable_info");
  }

  /**
   * Wraps a Java DataStore around a Lara DataStore.
   */
  protected dataStoreWrapper(javaDataStore: JavaClasses.DataStore) {
    return new ClavaDataStore(javaDataStore, this.definition);
  }

  /**
   * @returns A string with the current C/C++ compiler flags.
   */
  getFlags(): string {
    return this.get("Compiler Flags");
  }

  /**
   * @param flags - A string with C/C++ compiler flags.
   *
   */
  setFlags(flags: string) {
    this.put("Compiler Flags", flags);
  }

  /**
   * @returns {J#java.util.List<String>} A list with the current extra system includes.
   */
  getSystemIncludes(): string[] {
    return this.get("library includes").getFiles();
  }

  /**
   * @return {J#java.util.List<String>} A list with the current user includes.
   */
  getUserIncludes(): string[] {
    return this.get("header includes").getFiles();
  }

  /**
   * @param arguments - A variable number of strings with the extra system includes.
   *
   */
  setSystemIncludes(...args: string[]) {
    const filenames = arrayFromArgs(args);
    const files = filenames.map((filename: string) => Io.getPath(filename));

    this.put("library includes", JavaTypes.FileList.newInstance(files));
  }

  /**
   * @param arguments - A variable number of strings with the user includes.
   *
   */
  setUserIncludes(...args: string[]) {
    const filenames = arrayFromArgs(args);
    const files = filenames.map((filename: string) => Io.getPath(filename));

    this.put("header includes", JavaTypes.FileList.newInstance(files));
  }

  /**
   * @returns A string with the current compilation standard.
   */
  getStandard(): string {
    return this.get("C/C++ Standard").toString();
  }

  /**
   * @param flags - A string with a C/C++/OpenCL compilation standard.
   *
   */
  setStandard(standard: string) {
    const stdObject =
      ClavaJavaTypes.Standard.getEnumHelper().fromValue(standard);

    this.put("C/C++ Standard", stdObject);
  }
}
