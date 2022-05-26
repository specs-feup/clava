laraImport("lara.graphs.NodeData");

class ScgNodeData extends NodeData {
	
	// The function represented by this node
	#function;

	constructor($function) {
		super();
		this.#function = $function;
	}	

	get function() {
		return this.#function;
	}	
	
	toString() {
		return this.#function.signature;
	}
}