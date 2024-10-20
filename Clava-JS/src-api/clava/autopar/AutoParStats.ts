import Io from "@specs-feup/lara/api/lara/Io.js";

/**
 * Collects stats about AutoPar execution.
 */
export default class AutoParStats {
    name = "default";
    inlinedCalls = 0;
    inlineAnalysis: Record<string, number> = {};
    inlineAnalysisFunctions: Record<string, string[]> = {};
    inlineExcludedFunctions: string[] = [];
    inductionVariableReplacements = 0;

    /*** STATIC FUNCTIONS ***/

    private static _STATS: AutoParStats | undefined = undefined;

    static readonly EXCLUDED_RECURSIVE = "excluded_recursive";
    static readonly EXCLUDED_IS_UNSAFE = "excluded_is_unsafe";
    static readonly EXCLUDED_GLOBAL_VAR = "excluded_global_var";
    static readonly EXCLUDED_CALLS_UNSAFE = "excluded_has_unsafe_calls";

    static get() {
        if (AutoParStats._STATS === undefined) {
            AutoParStats._STATS = new AutoParStats();
        }

        return AutoParStats._STATS;
    }

    static reset() {
        AutoParStats._STATS = new AutoParStats();
    }

    static save() {
        if (AutoParStats._STATS === undefined) {
            console.log(
                "AutoParStats.saveStats: stats have not been initialized, use AutoParStats.resetStats()"
            );
            return;
        }

        AutoParStats._STATS.write();
    }

    /*** INSTANCE FUNCTIONS ***/

    /**
     * Sets the name of these stats (will reflect on the name of the output file)
     */
    setName(name: string) {
        this.name = name;
    }

    /**
     * Writes the current stats to the given output folder
     */
    write(outputFolder = Io.getWorkingFolder()) {
        Io.writeJson(
            Io.getPath(outputFolder, "AutoParStats-" + this.name + ".json").getAbsolutePath(),
            this
        );
    }

    incInlineCalls() {
        this.inlinedCalls++;
    }

    incInlineAnalysis(field?: string, functionName?: string) {
        if (field === undefined) {
            console.log("AutoParStats.incInlineAnalysis: field is undefined");
            return;
        }

        if (this.inlineAnalysis[field] === undefined) {
            this.inlineAnalysis[field] = 0;
        }

        this.inlineAnalysis[field]++;

        if (functionName !== undefined) {
            if (this.inlineAnalysisFunctions[field] === undefined) {
                this.inlineAnalysisFunctions[field] = [];
            }

            this.inlineAnalysisFunctions[field].push(functionName);
        }
    }

    setInlineExcludedFunctions(excludedFunctions: string[]) {
        this.inlineExcludedFunctions = excludedFunctions;
    }

    incIndunctionVariableReplacements() {
        this.inductionVariableReplacements++;
    }
}