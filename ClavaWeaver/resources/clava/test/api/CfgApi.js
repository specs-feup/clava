import ControlFlowGraph from "@specs-feup/clava/api/clava/graphs/ControlFlowGraph.js";
import CfgUtils from "@specs-feup/clava/api/clava/graphs/cfg/CfgUtils.js";
import CfgEdgeType from "@specs-feup/clava/api/clava/graphs/cfg/CfgEdgeType.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import Graphs from "@specs-feup/lara/api/lara/graphs/Graphs.js";

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
//	console.log("CFG for stmt:\n" + $stmt.code)
//	const smallCfg = 
	ControlFlowGraph.build($stmt, true);	
//	console.log(Graphs.toDot(smallCfg.graph));
}

function buildAndVerifyCfg($function, options) {
	const cfg = ControlFlowGraph.build($function, true, options);

	if (options !== undefined) {
		console.log(`Options used and Graph for ${$function.name}:`)
		console.log(`${JSON.stringify(options, null, 2)}`);
	}
	else	
		console.log(`Graph for ${$function.name}:`)

	console.log(Graphs.toDot(cfg.graph));
	verifyGraph(cfg)
	console.log(`Verification completed for ${$function.name}\n`)
}

function verifyGraph(cfg) {
	for(const node of cfg.graph.nodes()) {

		// Verify if all stmts have a mapping in nodes
		for(const stmt of node.data().stmts) {
			const graphNode= cfg.getNode(stmt);
			if(graphNode === undefined) {
				console.log("Stmt "+stmt.astId+" " + stmt.joinPointType + "@" + stmt.location + " does not have a graph node");
				continue;
			}

			if(!node.equals(graphNode)) {
				console.log("Stmt " + stmt.joinPointType + "@" + stmt.location + " has a graph node but is not the same");
				continue;
			}
		}
	}	
}    