import ToolUtils from "lara-js/api/lara/tool/ToolUtils.js";
import GenericCMakeCompiler from "./compilers/GenericCMakeCompiler.js";
export default class CMakerUtils extends ToolUtils {
    static compilerTable = {
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
    static getCompiler(compilerName) {
        return CMakerUtils.compilerTable[compilerName]();
    }
}
//# sourceMappingURL=CMakerUtils.js.map