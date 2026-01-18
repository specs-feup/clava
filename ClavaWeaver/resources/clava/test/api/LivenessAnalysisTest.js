import LivenessAnalysis from "@specs-feup/clava/api/clava/liveness/LivenessAnalysis.js";
import ControlFlowGraph from "@specs-feup/clava/api/clava/graphs/ControlFlowGraph.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const $fooFunction = Query.search("function", "foo").first();
const cfgOptions = {splitInstList: true};
const cfg = ControlFlowGraph.build($fooFunction, true, cfgOptions);

console.log("Liveness results for foo:")

const analysisResult = LivenessAnalysis.analyse(cfg.graph);
for (const node of cfg.graph.nodes()) {
    const def = analysisResult.defs.get(node.id());
    const use = analysisResult.uses.get(node.id());
    const liveIn = analysisResult.liveIn.get(node.id());
    const liveOut = analysisResult.liveOut.get(node.id());

    console.log("Node id: " + node.id());
    console.log("Node stmt: " + node.data().toString());
    console.log("Def: "+  [...sortSet(def)]);
    console.log("Use: " + [...sortSet(use)]);
    console.log("Live in: " + [...sortSet(liveIn)]);
    console.log("Live out: " + [...sortSet(liveOut)]);
    console.log();
}

function sortSet(set) {
    const sortedArray = Array.from(set).sort();
    return new Set(sortedArray);
}