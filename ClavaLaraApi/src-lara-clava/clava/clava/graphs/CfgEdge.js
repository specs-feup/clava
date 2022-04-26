laraImport("lara.graphs.GraphEdge")

/**
 * An edge of the CFG
 */
class CfgEdge extends GraphEdge{
	
	#type
	
	constructor(type) {
		super();
		
		this.#type = type;
	}
	
	get type() {
		return this.#type;
	}
	
	toString() {
		// If unconditional jump, do not print a label
		if(this.#type === CfgEdgeType.UNCONDITIONAL) {
			return "";
		}
		
		// Otherwise, return the type name
		return this.#type.name;
	}
			
}