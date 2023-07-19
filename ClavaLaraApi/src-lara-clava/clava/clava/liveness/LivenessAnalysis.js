laraImport("clava.liveness.LivenessAnalyser");


class LivenessAnalysis {
    /**
     * 
     */
    #defs;

    /**
     *
     */
    #uses;

    /**
     * 
     */
    #liveIn;

    /**
     * 
     */
    #liveOut;

    constructor(defs, uses, liveIn, liveOut) {
        this.#defs = defs;
        this.#uses = uses;
        this.#liveIn = liveIn;
        this.#liveOut = liveOut;
    }

    static analyse($jp) {
        const analyser = new LivenessAnalyser($jp).analyse();
        return new LivenessAnalysis(...analyser);
    }

    get defs() {
        return this.#defs;
    }

    get uses() {
        return this.#uses;
    }

    get liveIn() {
        return this.#liveIn;
    }

    get liveOut() {
        return this.#liveOut;
    }
}
