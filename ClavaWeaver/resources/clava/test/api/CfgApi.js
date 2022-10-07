laraImport("clava.graphs.ControlFlowGraph");
laraImport("clava.graphs.cfg.CfgUtils");
laraImport("clava.graphs.cfg.CfgEdgeType");
laraImport("weaver.Query");

const $fooFunction = Query.search("function", "foo").first();
const cfg = ControlFlowGraph.build($fooFunction, true);
println("Graph for foo:");
println(Graphs.toDot(cfg.graph));
verifyGraph(cfg)
println("Verification done")

// Stress test
for(const $stmt of Query.search("function", "foo").search("statement")) {
//	println("CFG for stmt:\n" + $stmt.code)
//	const smallCfg = 
	ControlFlowGraph.build($stmt, true);	
//	println(Graphs.toDot(smallCfg.graph));
}



function verifyGraph(cfg) {
	for(const node of cfg.graph.nodes()) {

		// Verify if all stmts have a mapping in nodes
		for(const stmt of node.data().stmts) {
			const graphNode= cfg.getNode(stmt);
			if(graphNode === undefined) {
				println("Stmt "+stmt.astId+" " + stmt.joinPointType + "@" + stmt.location + " does not have a graph node");
				continue;
			}

			if(!node.equals(graphNode)) {
				println("Stmt " + stmt.joinPointType + "@" + stmt.location + " has a graph node but is not the same");
				continue;
			}
		}
	}	
}    