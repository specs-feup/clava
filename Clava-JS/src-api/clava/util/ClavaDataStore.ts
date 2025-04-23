import WeaverDataStore from "@specs-feup/lara/api/weaver/util/WeaverDataStore.js";
import ClavaJavaTypes from "../ClavaJavaTypes.js";
import JavaTypes, {
  JavaClasses,
} from "@specs-feup/lara/api/lara/util/JavaTypes.js";
import Io from "@specs-feup/lara/api/lara/Io.js";
import DataStore from "@specs-feup/lara/api/lara/util/DataStore.js";
import { arrayFromArgs } from "@specs-feup/lara/api/lara/core/LaraCore.js";

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
   * @returns A list with the current extra system includes.
   */
  getSystemIncludes(): string[] {
    return this.get("library includes").getFiles().toArray();
  }

  /**
   * @returns A list with the current user includes.
   */
  getUserIncludes(): string[] {
    return this.get("header includes").getFiles().toArray();
  }

  /**
   * @param arguments - A variable number of strings with the extra system includes.
   *
   */
  setSystemIncludes(...args: string[]) {
    const filenames = arrayFromArgs(args);
    const files = filenames.map((filename: string) => Io.getPath(filename));

    if (files.length === 0) {
      this.put("library includes", JavaTypes.FileList.newInstance());
    } else {
      this.put("library includes", JavaTypes.FileList.newInstance(...files));
    }
  }

  /**
   * @param arguments - A variable number of strings with the user includes.
   *
   */
  setUserIncludes(...args: string[]) {
    const filenames = arrayFromArgs(args);
    const files = filenames.map((filename: string) => Io.getPath(filename));
    if(files.length === 0) {
      this.put("header includes", JavaTypes.FileList.newInstance());
    } else {
      this.put("header includes", JavaTypes.FileList.newInstance(...files));
    }

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
