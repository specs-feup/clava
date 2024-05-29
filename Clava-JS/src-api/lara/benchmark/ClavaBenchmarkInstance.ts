import Io from "lara-js/api/lara/Io.js";
import BenchmarkInstance from "lara-js/api/lara/benchmark/BenchmarkInstance.js";
import { JavaClasses } from "lara-js/api/lara/util/JavaTypes.js";
import Query from "lara-js/api/weaver/Query.js";
import Weaver from "lara-js/api/weaver/Weaver.js";
import Clava from "../..//clava/Clava.js";
import { Pragma, Program } from "../../Joinpoints.js";
import CMaker from "../../clava/cmake/CMaker.js";

/**
 * Instance of a Clava benchmark.
 *
 * Implements _compilePrivate and .getKernel().
 */
export default class ClavaBenchmarkInstance extends BenchmarkInstance {
  /**
   * The output folder for this BenchmarkInstance.
   */
  private getOutputFolder() {
    return Io.mkdir(this.getBaseFolder(), this.getName()).getAbsolutePath();
  }

  protected compilationEngineProvider(name: string): CMaker {
    return new CMaker(name);
  }

  protected compilePrivate(): JavaClasses.File | undefined {
    const folder = this.getOutputFolder();
    Clava.writeCode(folder);

    const cmaker = this.getCompilationEngine() as CMaker;

    cmaker.addCurrentAst();

    const exe = cmaker.build(folder);

    if (exe !== undefined) {
      this.setExecutable(exe);
    }

    return exe;
  }

  protected loadPrivate(): void {}

  protected closePrivate(): void {}

  protected loadCached(astFile: JavaClasses.File) {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-call
    console.log(`Loading cached AST from file ${astFile.getAbsolutePath()}...`);

    // Load saved AST
    const $app = Weaver.deserialize(Io.readFile(astFile)) as Program;

    // Push loaded AST
    Clava.pushAst($app);
  }

  /**
   * Looks for #pragma kernel, returns target of that pragma
   */
  getKernel() {
    const $pragma = Query.search(Pragma, "kernel").first();

    if ($pragma === undefined) {
      throw `ClavaBenchmarkInstance.getKernel: Could not find '#pragma kernel' in benchmark ${this.getName()}`;
    }

    return $pragma.target;
  }
}
