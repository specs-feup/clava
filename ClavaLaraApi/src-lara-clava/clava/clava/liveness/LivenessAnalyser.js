laraImport("weaver.Query")
laraImport("clava.graphs.ControlFlowGraph"); 
laraImport("clava.liveness.LivenessUtils");
laraImport("lara.graphs.Graphs");


class LivenessAnalyser {
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
    #liveOut;

    constructor($jp) {
        this.#cfg = ControlFlowGraph.build($jp, true, true).graph;
    }

    analyse() {
        this.#computeDefs();
        this.#computeUses();
        this.#computeLiveInOut();

        return [this.#defs, this.#uses, this.#liveIn, this.#liveOut];
    }

    #computeDefs() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;

            const declaredVars = LivenessUtils.getVarDeclsWithInit($nodeStmt);
            const assignedVars = LivenessUtils.getAssignedVars($nodeStmt);
            const def = LivenessUtils.unionSets(declaredVars, assignedVars);

            this.#defs.set($nodeStmt.astId, def);
        }
    }

    #computeUses() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;

            const use = LivenessUtils.getVarRefs($nodeStmt);
            this.#uses.set($nodeStmt.astId, use);
        }
    }

    #computeLiveInOut() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;

            this.#liveIn.set($nodeStmt.astId, new Set());
            this.#liveOut.set($nodeStmt.astId, new Set());
        }

        let liveChanged;
        do {
            liveChanged = false;

            for (const node of this.#cfg.nodes()) {
                const nodeId = node.data().nodeStmt.astId;
                const def = this.#defs.get(nodeId);
                const use = this.#uses.get(nodeId);

                // Save current liveIn and liveOut
                const oldLiveIn = this.#liveIn.get(nodeId);
                const oldLiveOut = this.#liveOut.get(nodeId);

                // Compute and save new liveIn
                const diff = LivenessUtils.differenceSets(oldLiveOut, def);
                const newLiveIn = LivenessAnalysis.unionSets(use, diff);
                this.#liveIn.set(nodeId, newLiveIn);

                // Compute and save new liveOut
                let newLiveOut = new Set();
                for(const child of node.children()) {
                    const childId = child.data().nodeStmt.astId;
                    const childLiveIn = this.#liveIn.get(childId);

                    newLiveOut = LivenessUtils.unionSets(newLiveOut, childLiveIn);
                }
                this.#liveOut.set(nodeId, newLiveOut);

                // Update liveChanged
                if(!LivenessUtils.isSameSet(oldLiveIn, newLiveIn) || !LivenessAnalysis.isSameSet(oldLiveOut, newLiveOut))
                    liveChanged = true;
            }
        } while(liveChanged);
    }
}
