laraImport("lara.graphs.Graphs");
laraImport("weaver.Query");
laraImport("lara.util.StringSet");

class StaticCallGraph {
	
	// The static call graph
	#graph;
	
	// Maps functions to graph nodes
	#functions;
	
	constructor(graph, functions) {
		this.#graph = graph;
		this.#functions = functions;
	}
	
	static build($jp, visitCalls = false) {
		const builder = new StaticCallGraphBuilder();
		
		const graph = builder.build($jp, visitCalls);

		return new StaticCallGraph(graph, builder.nodes);
	}
	
	get graph() {
		return this.#graph;
	}
	
	get functions() {
		return this.#functions;
	}
	
	getNode($function) {
		return this.#functions[$function.signature];
	}	

}