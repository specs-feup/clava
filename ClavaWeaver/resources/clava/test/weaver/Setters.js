import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $call of Query.search("function", "testSetQualifiedName").search(
    "call",
    "now"
)) {
    console.log("Original qualified name: " + $call.declaration.qualifiedName);
    $call.declaration.qualifiedName = "now";
    console.log("Changed qualified name 1: " + $call.declaration.qualifiedName);
    $call.declaration.qualifiedName = "std::now";
    console.log("Changed qualified name 2: " + $call.declaration.qualifiedName);
    $call.declaration.qualifiedPrefix = "std::chrono::_V2::system_clock";
    console.log("Changed qualified name 3: " + $call.declaration.qualifiedName);
}

for (const $if of Query.search("function", "testIf").search("if")) {
    $if.then = ClavaJoinPoints.stmtLiteral("a = 3;");
    console.log("Changed then:\n" + $if.code);
    $if.else = ClavaJoinPoints.stmtLiteral("a = 4;");
    console.log("Changed else:\n" + $if.code);
    $if.cond = ClavaJoinPoints.exprLiteral("a == 3");
    console.log("Changed condition:\n" + $if.code);
}

for (const $function of Query.search("function", "testFunctionType")) {
    $function.returnType = ClavaJoinPoints.builtinType("double");
    $function.setParamType(0, ClavaJoinPoints.builtinType("int"));
    console.log("Changed Function:\n" + $function.code);
    console.log("Changed FunctionType:\n" + $function.functionType.code);
}
