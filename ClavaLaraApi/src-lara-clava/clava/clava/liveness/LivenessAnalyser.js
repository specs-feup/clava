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
        this.#defs = new Map();
        this.#uses = new Map();
        this.#liveIn = new Map();
        this.#liveOut = new Map();
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
            let def;

            if (node.id() === "start" || node.id() === "end" || $nodeStmt.instanceOf("scope"))
                def = new Set();
            else {
                const declaredVars = LivenessUtils.getVarDeclsWithInit($nodeStmt);
                const assignedVars = LivenessUtils.getAssignedVars($nodeStmt);
                def = LivenessUtils.unionSets(declaredVars, assignedVars);
            }
            this.#defs.set(node.id(), def);
        }
    }

    #computeUses() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;
            let use;

            if (node.id() === "start" || node.id() === "end" || $nodeStmt.instanceOf("scope"))
                use = new Set();
            else
                use = LivenessUtils.getVarRefs($nodeStmt);

            this.#uses.set(node.id(), use);
        }
    }

    #computeLiveInOut() {
        for (const node of this.#cfg.nodes()) {
            this.#liveIn.set(node.id(), new Set());
            this.#liveOut.set(node.id(), new Set());
        }

        let liveChanged;
        do {
            liveChanged = false;

            for (const node of this.#cfg.nodes()) {
                const def = this.#defs.get(node.id());
                const use = this.#uses.get(node.id());

                // Save current liveIn and liveOut
                const oldLiveIn = this.#liveIn.get(node.id());
                const oldLiveOut = this.#liveOut.get(node.id());

                // Compute and save new liveIn
                const diff = LivenessUtils.differenceSets(oldLiveOut, def);
                const newLiveIn = LivenessUtils.unionSets(use, diff);
                this.#liveIn.set(node.id(), newLiveIn);

                // Compute and save new liveOut
                let newLiveOut = new Set();
                for(const child of LivenessUtils.getChildren(node)) {
                    const childId = child.id();
                    const childLiveIn = this.#liveIn.get(childId);

                    newLiveOut = LivenessUtils.unionSets(newLiveOut, childLiveIn);
                }
                this.#liveOut.set(node.id(), newLiveOut);

                // Update liveChanged
                if(!LivenessUtils.isSameSet(oldLiveIn, newLiveIn) || !LivenessUtils.isSameSet(oldLiveOut, newLiveOut))
                    liveChanged = true;
            }
        } while(liveChanged);
    }
}
