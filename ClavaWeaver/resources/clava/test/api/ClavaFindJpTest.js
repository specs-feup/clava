import Clava from "@specs-feup/clava/api/clava/Clava.js";
import Io from "@specs-feup/lara/api/lara/Io.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import JoinPoints from "@specs-feup/lara/api/weaver/JoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";


function test() {
    let $aLoop;

    // Get a join point
    for (const $loop of Query.search("loop")) {
        $aLoop = $loop;
    }

    let $aScope;
    for (const $scope of Query.search("scope")) {
        $aScope = $scope;
    }

    // Push AST
    Clava.pushAst();

    // Find equivalent join point
    const $eqLoop = Clava.findJp($aLoop);
    if ($eqLoop === undefined) {
        console.log("Could not find loop");
        return;
    }

    // Find equivalent join point scope
    const $eqScope = Clava.findJp($aScope);
    if ($eqScope === undefined) {
        console.log("Could not find scope");
        return;
    }

    $eqLoop.insertBefore("// Pushed AST");

    console.log("Pushed AST:\n" + Query.root().code);

    Clava.popAst();

    console.log("Original AST:\n" + Query.root().code);

    //////

    JpCreateTest();
}


function JpCreateTest() {
    // New line
    console.log("JpCreateTest");

    // Create join point
    let astNode;
    let $loopJp;
    for (const $loop of Query.search("loop")) {
        $loopJp = $loop;
        astNode = $loop.node;
        // Finish after first loop
    }

    // Check if loop has node
    console.log("Has node? " + $loopJp.hasNode(astNode));

    // Create join point
    const $newLoopJp = ClavaJoinPoints.toJoinPoint(astNode);

    console.log("Are JPs equal? " + $newLoopJp.equals($loopJp));
}

test();
