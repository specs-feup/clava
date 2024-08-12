import Io from "lara-js/api/lara/Io.js";
import BenchmarkInstance from "lara-js/api/lara/benchmark/BenchmarkInstance.js";
import { JavaClasses } from "lara-js/api/lara/util/JavaTypes.js";
import Query from "lara-js/api/weaver/Query.js";
import Weaver from "lara-js/api/weaver/Weaver.js";
import Clava from "../..//clava/Clava.js";
import { Joinpoint, Pragma, Program } from "../../Joinpoints.js";
import CMaker from "../../clava/cmake/CMaker.js";
import ClavaJoinPoints from "../../clava/ClavaJoinPoints.js";

/**
 * Instance of a Clava benchmark.
 *
 * Implements _compilePrivate and .getKernel().
 */
export default abstract class ClavaBenchmarkInstance extends BenchmarkInstance {
  cmaker: CMaker | undefined;
  cmakerProvider: () => CMaker;

  constructor(name: string) {
    super(name);

    this.cmaker = undefined;
    this.cmakerProvider = () => new CMaker(name);
  }

  setCMakerProvider(cmakerProvider: () => CMaker): void {
    this.cmakerProvider = cmakerProvider;

    // New provider set, remove CMaker
    this.cmaker = undefined;
  }

  /**
   * The output folder for this BenchmarkInstance.
   */
  private getOutputFolder() {
    return Io.mkdir(this.getBaseFolder(), this.getName()).getAbsolutePath();
  }

  protected compilationEngineProvider(name: string): CMaker {
    return new CMaker(name);
  }

  /**
   * Allows to customize the CMake options used during compilation.
   *
   * @param name
   * @returns
   */
  protected getCMaker(): CMaker {
    if (this.cmaker === undefined) {
      this.cmaker = this.cmakerProvider();
    }

    return this.cmaker;
  }

  protected compilePrivate(): JavaClasses.File | undefined {
    const folder = this.getOutputFolder();
    Clava.writeCode(folder);

    //const cmaker = this.getCompilationEngine() as CMaker;
    const cmaker = this.getCMaker();

    cmaker.addCurrentAst();

    const exe = cmaker.build(folder);

    if (exe !== undefined) {
      this.setExecutable(exe);
    }

    return exe;
  }

  /**
   * Speciallized implementation for Clava that automatically saves and restores the AST, extending classes just need to implement addCode() and loadPrologue().
   */
  protected loadPrivate(): void {
    // Execute configuration for current instance
    this.loadPrologue();

    // Pust an empty AST to the top of the stack
    Clava.pushAst(ClavaJoinPoints.program());

    // Add code
    this.addCode();

    // Rebuild
    Clava.rebuild();
  }

  protected closePrivate(): void {
    // Restore any necessary configurations
    this.closeEpilogue();

    // Restore previous AST
    Clava.popAst();
  }

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

  /*** FUNCTIONS TO IMPLEMENT ***/

  /**
   * Configuration that is required by the benchmarks (e.g., setting the standard)
   */
  protected abstract loadPrologue(): void;

  /**
   * Adds the code to the AST, can assume the AST is empty.
   */
  protected abstract addCode(): void;

  /**
   *
   */
  protected abstract closeEpilogue(): void;
}
