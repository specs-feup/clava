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
    #liveOut;

    constructor($jp) {
        this.#jp = $jp;
        this.#cfg = ControlFlowGraph.build($jp, true, true).graph;

        this.#computeDefs();
        this.#computeUses();
        this.#computeLiveInOut();
    }

    #computeDefs() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;
            let def = new Set();
            
            const $varDecls = Query.searchFromInclusive($nodeStmt, "vardecl", {hasInit: true});
            for (const $var of $varDecls)
                    def.add($var.name);

            const $assignments = Query.searchFromInclusive($nodeStmt, "binaryOp", {isAssignment: true});
            for (const $assign of $assignments) {
                if ($assign.left.instanceOf("varref"))
                    def.add($assign.left.name);
            }

            this.#defs.set($nodeStmt.astId, def);
        }
    }

    #computeUses() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;
            let use = new Set();
            
            const $varRefs = Query.searchFromInclusive($nodeStmt, "varref");
            for (const $var of $varRefs) {
                const $parent = $var.parent;

                if ($parent.isAssignment && $parent.left.astId === $var.astId)
                    continue;

                use.add($var.name);
            }

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
                const diff = new Set([...oldLiveOut].filter(_var => !def.has(_var))); // Difference between liveOut and def
                const newLiveIn =  new Set([...use, ...diff]);
                this.#liveIn.set(nodeId, newLiveIn);

                // Compute and save new liveOut
                let newLiveOut = new Set();
                for(const child of node.children()) {
                    const childId = child.data().nodeStmt.astId;
                    const childLiveIn = this.#liveIn.get(childId);

                    newLiveOut = new Set(...newLiveOut, ...childLiveIn);
                }
                this.#liveOut.set(nodeId, newLiveOut);

                // Update liveChanged
                const inEqual = oldLiveIn.size === newLiveIn.size && [...oldLiveIn].every(newLiveIn.has, newLiveIn);
                const outEqual = oldLiveOut.size === newLiveOut.size && [...oldLiveOut].every(newLiveOut.has, newLiveOut);
                if (!inEqual || !outEqual)
                    liveChanged = true;
            }
        } while(liveChanged);
    }

    getLiveIn($stmt) {
        return this.#liveIn.get($stmt.astId);
    }

    getLiveOut($stmt) {
        return this.#liveOut.get($stmt.astId);
    }
}
