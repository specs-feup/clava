import Query from "@specs-feup/lara/api/weaver/Query.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import HLSAnalysis from "@specs-feup/clava/api/clava/hls/HLSAnalysis.js";
import Clava from "@specs-feup/clava/api/clava/Clava.js";

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
