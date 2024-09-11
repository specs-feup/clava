import Io from "lara-js/api/lara/Io.js";
import BenchmarkInstance from "lara-js/api/lara/benchmark/BenchmarkInstance.js";
import Query from "lara-js/api/weaver/Query.js";
import Weaver from "lara-js/api/weaver/Weaver.js";
import Clava from "../..//clava/Clava.js";
import CMaker from "../../clava/cmake/CMaker.js";
import ClavaJoinPoints from "../../clava/ClavaJoinPoints.js";
/**
 * Instance of a Clava benchmark.
 *
 * Implements _compilePrivate and .getKernel().
 */
export default class ClavaBenchmarkInstance extends BenchmarkInstance {
    cmaker;
    cmakerProvider;
    constructor(name) {
        super(name);
        this.cmaker = undefined;
        this.cmakerProvider = () => new CMaker(name);
    }
    setCMakerProvider(cmakerProvider) {
        this.cmakerProvider = cmakerProvider;
        // New provider set, remove CMaker
        this.cmaker = undefined;
    }
    /**
     * The output folder for this BenchmarkInstance.
     */
    getOutputFolder() {
        return Io.mkdir(this.getBaseFolder(), this.getName()).getAbsolutePath();
    }
    compilationEngineProvider(name) {
        return new CMaker(name);
    }
    /**
     * Allows to customize the CMake options used during compilation.
     *
     * @param name
     * @returns
     */
    getCMaker() {
        if (this.cmaker === undefined) {
            this.cmaker = this.cmakerProvider();
        }
        return this.cmaker;
    }
    compilePrivate() {
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
    loadPrivate() {
        // Execute configuration for current instance
        this.loadPrologue();
        // Pust an empty AST to the top of the stack
        Clava.pushAst(ClavaJoinPoints.program());
        // Add code
        this.addCode();
        // Rebuild
        Clava.rebuild();
    }
    closePrivate() {
        // Restore any necessary configurations
        this.closeEpilogue();
        // Restore previous AST
        Clava.popAst();
    }
    loadCached(astFile) {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        console.log(`Loading cached AST from file ${astFile.getAbsolutePath()}...`);
        // Load saved AST
        const $app = Weaver.deserialize(Io.readFile(astFile));
        // Push loaded AST
        Clava.pushAst($app);
    }
    /**
     * Looks for #pragma kernel, returns target of that pragma
     */
    getKernel() {
        const $pragma = Query.search("pragma", "kernel").first();
        if ($pragma === undefined) {
            throw `ClavaBenchmarkInstance.getKernel: Could not find '#pragma kernel' in benchmark ${this.getName()}`;
        }
        return $pragma.target;
    }
}
//# sourceMappingURL=ClavaBenchmarkInstance.js.map