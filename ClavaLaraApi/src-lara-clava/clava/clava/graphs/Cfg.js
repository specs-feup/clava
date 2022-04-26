class Cfg {
	
	#graph;
	
	#startNode;
	#endNode;
	
	// FunctionDecl?
	constructor(graph, startNode, endNode) {
		this.#graph = graph;
		this.#startNode = startNode;
		this.#endNode = endNode;		
	}
}