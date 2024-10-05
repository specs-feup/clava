laraImport("weaver.Query");
laraImport("clava.Clava");

for (const $loop of Query.search("function", "foo").search("loop")) {
    console.log("Data:");
    printObject($loop.data);
    console.log("");
}

for (const $loop of Query.search("function", "noData").search("loop")) {
    console.log("Empty Object:");
    printlnObject($loop.data);
}

// Insert preserves pragma
const $loopWithPragma = Query.search("function", "insertPreservesPragma")
    .search("loop")
    .first();
console.log("Data before insert: " + $loopWithPragma.data.a);
$loopWithPragma.insertBefore("int a;");
console.log("Data after insert: " + $loopWithPragma.data.a);

// ...also before functions
const $loopWithPragma2 = Query.search(
    "function",
    "insertPreservesPragma2"
).first();
console.log("Data before insert: " + $loopWithPragma2.data.a);
$loopWithPragma2.insertBefore("int global_a;");
console.log("Data after insert: " + $loopWithPragma2.data.a);

// Setting data without pragma creates a pragma
const $loopWithoutPragma = Query.search(
    "function",
    "insertWithoutPragma"
).first();
let obj = $loopWithoutPragma.data;
obj.a = 42;
obj.b = 43;
$loopWithoutPragma.setData(obj);
console.log(
    "Loop without pragma after setting data: " +
        $loopWithoutPragma.pragmas.map((p) => p.code)
);

// Setting already existing data updates pragma
const $loopWithUpdatedPragma = Query.search("function", "updatePragma").first();
obj = $loopWithUpdatedPragma.data;
obj.a = 200;
$loopWithUpdatedPragma.setData(obj);
console.log(
    "Loop with updated pragma: " +
        $loopWithUpdatedPragma.pragmas.map((p) => p.code)
);

// Information persists after rebuilds
const $loopBeforeRebuild = Query.search("function", "rebuild").first();
console.log(
    "Is parallel before set and rebuild: " + $loopBeforeRebuild.data.isParallel
);
obj = $loopBeforeRebuild.data;
obj.isParallel = true;
$loopBeforeRebuild.setData(obj);

const $loopAfterRebuild = Query.search("function", "rebuild").first();
console.log("Is parallel after rebuild: " + $loopAfterRebuild.data.isParallel);

// Data on expression nodes
const $op = Query.search("function", "dataInExpressions")
    .search("binaryOp")
    .first();
obj = $op.data;
obj.a = 1000;
obj.b = 2000;
$op.setData(obj);
console.log("ExprData before push");
printlnObject($op.data);
Clava.pushAst();
const $opAfterPush = Query.search("function", "dataInExpressions")
    .search("binaryOp")
    .first();
obj = $opAfterPush.data;
obj.c = 3000;
$opAfterPush.setData(obj);
Clava.popAst();
const $opAfterPop = Query.search("function", "dataInExpressions")
    .search("binaryOp")
    .first();
console.log("ExprData after pop");
printlnObject($op.data);

const $fileRebuildOp = Query.search("function", "fileRebuild")
    .search("binaryOp")
    .first();
obj = $fileRebuildOp.data;
obj.opRebuild = true;
$fileRebuildOp.setData(obj);

const $fileRebuildLoop = Query.search("function", "fileRebuild2")
    .search("loop")
    .first();
obj = $fileRebuildLoop.data;
obj.loopRebuild = true;
$fileRebuildLoop.setData(obj);

//from nothing adds pragma; changes reflected in pragma; push/pop preserve; rebuild preserves for ones with pragma
