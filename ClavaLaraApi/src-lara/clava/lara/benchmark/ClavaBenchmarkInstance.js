import Io from "lara-js/api/lara/Io.js";
import BenchmarkInstance from "lara-js/api/lara/benchmark/BenchmarkInstance.js";
import Query from "lara-js/api/weaver/Query.js";
import Weaver from "lara-js/api/weaver/Weaver.js";
import Clava from "../..//clava/Clava.js";
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
    getOutputFolder() {
        return Io.mkdir(this.getBaseFolder(), this.getName()).getAbsolutePath();
    }
    compilationEngineProvider(name) {
        return new CMaker(name);
    }
    /**
     * For compatibility reasons.
     *
     * @param name
     * @returns
     */
    getCMaker(name) {
        return this.compilationEngineProvider(name);
    }
    compilePrivate() {
        const folder = this.getOutputFolder();
        Clava.writeCode(folder);
        const cmaker = this.getCompilationEngine();
        cmaker.addCurrentAst();
        const exe = cmaker.build(folder);
        if (exe !== undefined) {
            this.setExecutable(exe);
        }
        return exe;
    }
    loadPrivate() { }
    closePrivate() { }
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