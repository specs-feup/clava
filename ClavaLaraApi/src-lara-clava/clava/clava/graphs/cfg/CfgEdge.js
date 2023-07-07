laraImport("lara.graphs.EdgeData")

/**
 * An edge of the CFG
 */
class CfgEdge extends EdgeData {
	
	#type
	
	/**
	 * Creates a new instance of the CfgEdge class
	 * @param {CfgEdgeType} type edge type
	 */
	constructor(type) {
		super();
		
		this.#type = type;
	}
	
	/**
	 * @returns {CfgEdgeType} the edge type
	 */
	get type() {
		return this.#type;
	}
	
	/**
	 * 
	 * @returns string representation of the edge. If it a unconditional edge, an empty string is returned
	 */
	toString() {
		// If unconditional jump, do not print a label
		if(this.#type === CfgEdgeType.UNCONDITIONAL) {
			return "";
		}
		
		// Otherwise, return the type name
		return this.#type.name;
	}
			
}