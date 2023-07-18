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

        this.#computeDefs();
        this.#computeUses();
        this.#computeLiveInOut();
    }

    #computeDefs() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;
            let def = [];
            
            const $varDecls = Query.searchFromInclusive($nodeStmt, "vardecl");
            for (const decl of $varDecls) {
                if(decl.hasInit)
                    def.push(decl.name);
            }

            const $binaryOps = Query.searchFromInclusive($nodeStmt, "binaryOp");
            for (const $binOp of $binaryOps) {
                if ($binOp.isAssignment && $binOp.left.instanceOf("varref"))
                    def.push($binOp.left.name);
            }

            this.#defs.set($nodeStmt.astId, def);
        }
    }

    #computeUses() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;
            let use = [];
            
            const $varRefs = Query.searchFromInclusive($nodeStmt, "varref");
            for (const $var of $varRefs) {
                const $parent = $var.astParent;

                if ($parent.instanceOf("binaryOp") && $parent.isAssignment && $parent.left.astId === $var.astId)
                    continue;

                use.push($var.name);
            }

            this.#uses.set($nodeStmt.astId, use);
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