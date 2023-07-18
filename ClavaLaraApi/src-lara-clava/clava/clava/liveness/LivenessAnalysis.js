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
            
            const $vardecls = Query.searchFromInclusive($nodeStmt, "vardecl");
            for (const decl of $vardecls){
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