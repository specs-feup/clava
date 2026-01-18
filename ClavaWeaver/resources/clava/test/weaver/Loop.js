import Query from "@specs-feup/lara/api/weaver/Query.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

for (const $loop of Query.search("function", "main").search("loop")) {
    console.log("loop control var: " + $loop.controlVar);
    console.log("is innermost? " + $loop.isInnermost);
    console.log("is outermost? " + $loop.isOutermost);
    console.log("nested level: " + $loop.nestedLevel + "\n");
    console.log("rank: " + $loop.rank + "\n");
}

// Test iterationsExpr
for (const $loop of Query.search("function", "iterationsExpr").search("loop")) {
    if ($loop.iterationsExpr === undefined) {
        console.log("iterations expr: undefined");
        continue;
    }
    console.log("iterations expr: " + $loop.iterationsExpr.code);
    console.log("iterations: " + $loop.iterations);
}

// Test insertion in for header
const $headerInsert1 = Query.search("function", "headerInsert1")
    .search("loop")
    .first();
if ($headerInsert1) {
    testHeaderInsert($headerInsert1, true);
}

const $headerInsert2 = Query.search("function", "headerInsert2")
    .search("loop")
    .first();
if ($headerInsert2) {
    testHeaderInsert($headerInsert2, false);
}

function testHeaderInsert($loop, isDeclaration) {
    const newVarName1 = "newVar1";
    const newVarName2 = "newVar2";

    // Declare new variable
    const $varDecl1 = ClavaJoinPoints.varDeclNoInit(
        newVarName1,
        ClavaJoinPoints.builtinType("int")
    );
    const $varDecl2 = ClavaJoinPoints.varDeclNoInit(
        newVarName2,
        ClavaJoinPoints.builtinType("int")
    );

    $loop.insertBefore($varDecl1);
    $loop.insertBefore($varDecl2);

    const initCodePrefix1 = isDeclaration ? "int " + newVarName1 : newVarName1;
    const initCodePrefix2 = isDeclaration ? "int " + newVarName2 : newVarName2;

    // Add inserts
    $loop.init.insertBefore(initCodePrefix1 + " = 10");
    $loop.init.insertAfter(initCodePrefix2 + " = 20");

    //console.log($loop.cond);
    $loop.cond.insertBefore(newVarName1 + "< 100");
    $loop.cond.insertAfter(newVarName2 + "< 200");
    //$loop.cond.replaceWith("i < 1000;");

    $loop.step.insertBefore(newVarName1 + "++");
    $loop.step.insertAfter(newVarName2 + "--");

    console.log("After header insert: " + $loop.parent.code);
}
