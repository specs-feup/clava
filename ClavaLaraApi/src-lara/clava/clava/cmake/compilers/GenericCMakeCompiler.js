import CMakeCompiler from "./CMakeCompiler.js";
/**
 * Iterates over a list of values.
 *
 */
export default class GenericCMakeCompiler extends CMakeCompiler {
    c;
    cxx;
    constructor(c, cxx) {
        super();
        this.c = c;
        this.cxx = cxx;
    }
    getCxxCommand() {
        return this.cxx;
    }
    getCCommand() {
        return this.c;
    }
}
//# sourceMappingURL=GenericCMakeCompiler.js.map