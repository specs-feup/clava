import ToolUtils from "lara-js/api/lara/tool/ToolUtils.js";
import CMakeCompiler from "./compilers/CMakeCompiler.js";
import GenericCMakeCompiler from "./compilers/GenericCMakeCompiler.js";

export default class CMakerUtils extends ToolUtils {
  private static compilerTable = {
    gcc: function () {
      return new GenericCMakeCompiler("gcc", "g++");
    },
    clang: function () {
      return new GenericCMakeCompiler("clang", "clang++");
    },
    icc: function () {
      return new GenericCMakeCompiler("icc", "icpc");
    },
  };

  /**
   * Creates a CMakerCompiler object based on a string with the name.
   *
   * @param compilerName - Name of the compiler. Currently supported names:  'gcc', 'clang', 'icc'.
   *
   */
  static getCompiler(
    compilerName: keyof typeof this.compilerTable
  ): CMakeCompiler {
    return CMakerUtils.compilerTable[compilerName]();
  }
}
