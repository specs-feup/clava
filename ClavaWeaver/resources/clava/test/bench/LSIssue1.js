laraImport("weaver.Query");
laraImport("clava.ClavaJoinPoints");
laraImport("clava.hls.HLSAnalysis");
laraImport("clava.Clava");

const $t = Query.search("function", {
    qualifiedName: (name) => name.endsWith("_Kernel"),
}).first();
const $nt = $t.clone($t.name + "C", false);
$t.insertBefore($nt);
const $callArgs = $t.params.map(($param) =>
    ClavaJoinPoints.varRefFromDecl($param)
);
const $call = $nt.newCall($callArgs);
$t.body.replaceWith($call);

const $newCall = Query.search("call", "matrix_mult_KernelC").first();
console.log("Def: " + $newCall.definition.name);
