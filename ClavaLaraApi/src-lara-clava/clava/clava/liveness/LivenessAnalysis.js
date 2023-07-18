laraImport("weaver.Query")
laraImport("clava.graphs.ControlFlowGraph"); 
laraImport("lara.graphs.Graphs");


class LivenessAnalysis {

    #jp;

    /**
	 * A Cytoscape graph representing the CFG
	 */
    #cfg;

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
    liveOut;

    constructor($jp) {
        this.#jp = $jp;
        this.#cfg = ControlFlowGraph.build($jp, true, true).graph;

        this.#computeDefsAndUses();
        this.#computeLiveInOut();
    }

    #computeDefsAndUses() {
        for (const node of this.#cfg.nodes()) {
            //TODO: compute def of node
            //TODO: compute use of node
            println (node.data().type);
        }
    }

    #computeLiveInOut() {
        for (const node of this.#cfg.nodes()) {
            //TODO: set liveIn = {}
            //TODO: set liveOut = {}
        }

        let liveChanged = true;

        while (liveChanged) {
            for (const node of this.#cfg.nodes()) {
                // TODO: copy node Live in 
                // TODO: copy node Live out
                // TODO: compute new Live in
                // TODO: compute new Live out
                // TODO: update liveChanged
            }
            
        }

    }

    getLiveIn($stmt) {
        return this.#liveIn.get($stmt.astId);
    }

    getLiveOut($stmt) {
        return this.liveOut.get($stmt.astId);
    }

}