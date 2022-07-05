laraImport("clava.graphs.cfg.CfgBuilder");
laraImport("clava.graphs.cfg.CfgUtils");
laraImport("clava.graphs.cfg.CfgEdgeType");
laraImport("weaver.Query");

const $fooFunction = Query.search("function", "foo").first();
const cfg = CfgBuilder.buildGraph($fooFunction);
verifyGraph(cfg)
println("Verification done")
//println(Graphs.toDot(cfg.graph));

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