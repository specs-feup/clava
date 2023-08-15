laraImport("clava.graphs.ControlFlowGraph");
laraImport("clava.graphs.cfg.CfgUtils");
laraImport("clava.graphs.cfg.CfgEdgeType");
laraImport("weaver.Query");

const $functions = Query.search("function");
for (const $function of $functions) {
	buildAndVerifyCfg($function);
}

const $gotoLabelFunction = Query.search("function", "gotoAndLabelExample").first();
const cfgOptions = {splitInstList: true, 
					removeLabelNodes: true, 
					removeGotoNodes: true, 
					keepTemporaryScopeStmts: true};
buildAndVerifyCfg($gotoLabelFunction, cfgOptions);

// Stress test
for(const $stmt of Query.search("function", "foo").search("statement")) {
//	println("CFG for stmt:\n" + $stmt.code)
//	const smallCfg = 
	ControlFlowGraph.build($stmt, true);	
//	println(Graphs.toDot(smallCfg.graph));
}

function buildAndVerifyCfg($function, options) {
	const cfg = ControlFlowGraph.build($function, true, options);

	if (options !== undefined) {
		println(`Options used and Graph for ${$function.name}:`)
		println(`${JSON.stringify(options, null, 2)}`);
	}
	else	
		println(`Graph for ${$function.name}:`)

	println(Graphs.toDot(cfg.graph));
	verifyGraph(cfg)
	println(`Verification completed for ${$function.name}\n`)
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