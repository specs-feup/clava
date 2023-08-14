laraImport("clava.liveness.LivenessAnalysis");
laraImport("clava.graphs.ControlFlowGraph");
laraImport("weaver.Query");

const $fooFunction = Query.search("function", "foo").first();
const cfgOptions = {splitInstList: true};
const cfg = ControlFlowGraph.build($fooFunction, true, cfgOptions);

println("Liveness results for foo:")

const analysisResult = LivenessAnalysis.analyse(cfg.graph);
for (const node of cfg.graph.nodes()) {
    const def = analysisResult.defs.get(node.id());
    const use = analysisResult.uses.get(node.id());
    const liveIn = analysisResult.liveIn.get(node.id());
    const liveOut = analysisResult.liveOut.get(node.id());

    println("Node id: " + node.id());
    println("Node stmt: " + node.data().toString());
    println("Def: "+  [...sortSet(def)]);
    println("Use: " + [...sortSet(use)]);
    println("Live in: " + [...sortSet(liveIn)]);
    println("Live out: " + [...sortSet(liveOut)]);
    println();
}

function sortSet(set) {
    const sortedArray = Array.from(set).sort();
    return new Set(sortedArray);
}