laraImport("lara.graphs.EdgeData");

class ScgEdgeData extends EdgeData {
	
	// The calls that contributed to this edge
	#calls;

	constructor() {
		super();
		this.#calls = [];
	}	

	get calls() {
		return this.#calls;
	}	
	
	inc($call) {
		this.#calls.push($call);
	}
	
	toString() {
		return this.#calls.length.toString();
	}
}