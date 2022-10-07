/**
 * Enumeration of CFG edge types.
 */
class CfgEdgeType {
	
	static TRUE = new CfgEdgeType("TRUE")
	static FALSE = new CfgEdgeType("FALSE")
	static UNCONDITIONAL = new CfgEdgeType("UNCONDITIONAL")
	
	#name;
	
	constructor(name) {
	    this.#name = name;
	}
	
	get name() {
		return this.#name;
	}

	toString() {
		return this.name;
	}
}