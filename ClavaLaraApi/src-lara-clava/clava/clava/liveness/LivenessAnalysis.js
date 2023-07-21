laraImport("clava.liveness.LivenessAnalyser");


class LivenessAnalysis {
    /**
     * Maps each CFG node ID to the corresponding def set
     */
    #defs;

    /**
     * Maps each CFG node ID to the corresponding use set
     */
    #uses;

    /**
     * Maps each CFG node ID to the corresponding LiveIn set
     */
    #liveIn;

    /**
     * Maps each CFG node ID to the corresponding LiveOut set
     */
    #liveOut;

    /**
     * Creates a new instance of the LivenessAnalysis class
     * @param {Map} defs a map with CFG node IDs as keys and their corresponding def set as value
     * @param {Map} uses a map with CFG node IDs as keys and their corresponding use set as value
     * @param {Map} liveIn a map with CFG node IDs as keys and their corresponding live in set as value
     * @param {Map} liveOut a map with CFG node IDs as keys and their corresponding live out set as value
     */
    constructor(defs, uses, liveIn, liveOut) {
        this.#defs = defs;
        this.#uses = uses;
        this.#liveIn = liveIn;
        this.#liveOut = liveOut;
    }

    /**
     * 
     * @param {joinpoint} $jp 
     * @returns {LivenessAnalysis} a new instance of the LivenessAnalysis class
     */
    static analyse($jp) {
        const analyser = new LivenessAnalyser($jp).analyse();
        return new LivenessAnalysis(...analyser);
    }

    /**
     * @returns {Map} a map with CFG node IDs as keys and their corresponding def set as value
     */
    get defs() {
        return this.#defs;
    }

    /**
     * @returns {Map} a map with CFG node IDs as keys and their corresponding use set as value
     */
    get uses() {
        return this.#uses;
    }

    /**
     * @returns {Map} a map with CFG node IDs as keys and their corresponding liveIn set as value
     */
    get liveIn() {
        return this.#liveIn;
    }

    /**
     * @returns {Map} a map with CFG node IDs as keys and their corresponding liveOut set as value
     */
    get liveOut() {
        return this.#liveOut;
    }
}
