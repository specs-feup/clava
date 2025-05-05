import Query from "@specs-feup/lara/api/weaver/Query.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import { FunctionJp } from "@specs-feup/clava/api/Joinpoints.js";

const $d = Query.search(FunctionJp, { isImplementation: true }).first();
if (!$d) {
    throw new Error("No function found");
}

const $fn = $d.clone($d.name + "_");
$d.setReturnType(ClavaJoinPoints.typeLiteral("void"));

$d.body.replaceWith(ClavaJoinPoints.scope());

$fn.addParam(
    "ret",
    ClavaJoinPoints.typeLiteral($fn.type.code + ($fn.type.isPointer ? "" : "*"))
);
console.log($d.params.map(($param) => $param.name));
console.log($fn.params.map(($param) => $param.name));

console.log(Query.root().code);
