laraImport("weaver.Query");
laraImport("clava.ClavaJoinPoints");

const $d = Query.search("function", { isImplementation: true }).first();
const $fn = $d.clone($d.name + "_");
$d.setReturnType(ClavaJoinPoints.typeLiteral("void"));
//$fn.type = ClavaJoinPoints.typeLiteral("void");
$d.body.replaceWith(ClavaJoinPoints.scope());

$fn.addParam(
    "ret",
    ClavaJoinPoints.typeLiteral($fn.type.code + ($fn.type.isPointer ? "" : "*"))
);
console.log($d.params.map(($param) => $param.name));
console.log($fn.params.map(($param) => $param.name));

console.log(Query.root().code);