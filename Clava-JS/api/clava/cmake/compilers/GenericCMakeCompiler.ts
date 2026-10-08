import CMakeCompiler from "./CMakeCompiler.ts";

/**
 * Iterates over a list of values.
 *
 */
export default class GenericCMakeCompiler extends CMakeCompiler {
  c: string;
  cxx: string;

  constructor(c: string, cxx: string) {
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
