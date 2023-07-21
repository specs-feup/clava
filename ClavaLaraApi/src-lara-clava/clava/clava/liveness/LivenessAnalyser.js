laraImport("weaver.Query")
laraImport("clava.graphs.ControlFlowGraph"); 
laraImport("clava.liveness.LivenessUtils");
laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("lara.graphs.Graphs");


class LivenessAnalyser {
    /**
	 * A Cytoscape graph representing the CFG
	 */
    #cfg;

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
     * Creates a new instance of the LivenessAnalyser class
     * @param {joinpoint} $jp 
     */
    constructor($jp) {
        this.#cfg = ControlFlowGraph.build($jp, true, true).graph;
        this.#defs = new Map();
        this.#uses = new Map();
        this.#liveIn = new Map();
        this.#liveOut = new Map();
    }

    /**
     * Computes the def, use, live in and live out sets of each CFG node
     * @returns {Array<Map>} an array that contains the def, use, live in and live out of each CFG node.
     */
    analyse() {
        this.#computeDefs();
        this.#computeUses();
        this.#computeLiveInOut();

        return [this.#defs, this.#uses, this.#liveIn, this.#liveOut];
    }

    /**
     * 
     * @param {joinpoint} $jp 
     * @returns {Set} the def set for the given joinpoint 
     */
    #computeDef($jp) {
        const declaredVars = LivenessUtils.getVarDeclsWithInit($jp);
        const assignedVars = LivenessUtils.getAssignedVars($jp);
        return LivenessUtils.unionSets(declaredVars, assignedVars);
    }

    /**
     * 
     * @param {joinpoint} $jp 
     * @returns {Set} the use set for the given joinpoint 
     */
    #computeUse($jp) {
        return LivenessUtils.getVarRefs($jp);
    }

    /**
     * Computes the def set of each CFG node according to its type
     */
    #computeDefs() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;
            const nodeType = node.data().type;
            
            let def;
            switch (nodeType) { 
                case CfgNodeType.START:
                case CfgNodeType.END:
                case CfgNodeType.SCOPE:
                case CfgNodeType.THEN:   
                case CfgNodeType.ELSE:
                case CfgNodeType.LOOP:
                case CfgNodeType.SWITCH:
                case CfgNodeType.CASE:
                    def = new Set();
                    break;
                case CfgNodeType.IF:
                    def = this.#computeDef($nodeStmt.cond);
                    break;
                default:
                    def = this.#computeDef($nodeStmt);
            }
            this.#defs.set(node.id(), def);
        }
    }

     /**
     * Computes the use set of each CFG node according to its type
     */
    #computeUses() {
        for (const node of this.#cfg.nodes()) {
            const $nodeStmt = node.data().nodeStmt;
            const nodeType = node.data().type;

            let use;
            switch (nodeType) { 
                case CfgNodeType.START:
                case CfgNodeType.END:
                case CfgNodeType.SCOPE:
                case CfgNodeType.THEN:   
                case CfgNodeType.ELSE:
                case CfgNodeType.LOOP:
                    use = new Set();
                    break;
                case CfgNodeType.IF:
                    use = this.#computeUse($nodeStmt.cond);
                    break;
                case CfgNodeType.SWITCH:
                    use = this.#computeUse($nodeStmt.condition);
                    break;
                case CfgNodeType.CASE:
                    const $switchCondition = $nodeStmt.ancestor("switch").condition;
                    use = $nodeStmt.isDefault ? new Set() : this.#computeUse($switchCondition);
                    break;
                default:
                    use = this.#computeUse($nodeStmt);
            }
            this.#uses.set(node.id(), use);
        }
    }

     /**
     * Computes the LiveIn and LiveOut set of each CFG node
     */
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
