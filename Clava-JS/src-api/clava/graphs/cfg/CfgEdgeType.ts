/**
 * Enumeration of CFG edge types.
 */
class CfgEdgeType {
    
    static TRUE = new CfgEdgeType("TRUE")
    static FALSE = new CfgEdgeType("FALSE")
    static UNCONDITIONAL = new CfgEdgeType("UNCONDITIONAL")
    
    #name;
    
    /**
     * Creates a new instance of the CfgEdgeType class
     * @param {String} name the name of the CFG edge type
     */
    constructor(name) {
        this.#name = name;
    }
    
    /**
     * @returns {String} the name of the CFG edge type
     */
    get name() {
        return this.#name;
    }

    /** 
     * @returns {String} string representation of the CFG edge type, which corresponds to its name
     */
    toString() {
        return this.name;
    }
}