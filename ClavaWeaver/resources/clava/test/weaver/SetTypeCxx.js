import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";


const $floatType = ClavaJoinPoints.builtinType("float");

for (const $expr of Query.search("function", "cstyle_cast").search("expression")) {
    if($expr.astName === "CStyleCastExpr") {
            $expr.setType($floatType); 
    }
}

for (const $expr of Query.search("function", "static_cast_foo").search("expression")) {
    if($expr.astName === "CXXStaticCastExpr") {
            $expr.setType($floatType); 
    }
}

for (const $typedefDecl of Query.search("typedefDecl")) {
    $typedefDecl.type = $floatType;
    $typedefDecl.name = "x_float";
}

console.log(Query.root().code);

