laraImport("weaver.Query");

function testSwitch($switch) {
    console.log("hasDefaultCase: " + $switch.hasDefaultCase);
    console.log("getDefaultCase: " + $switch.getDefaultCase);
    console.log("condition: " + $switch.condition.code);
    for (const $caseStmt of $switch.cases) {
        console.log("case is default: " + $caseStmt.isDefault);
        console.log("case is empty: " + $caseStmt.isEmpty);
        console.log("values: " + $caseStmt.values.map((v) => v.code));
        console.log(
            "next case: " +
                ($caseStmt.nextCase !== undefined
                    ? $caseStmt.nextCase.code
                    : "undefined")
        );
        console.log("case next instruction: " + $caseStmt.nextInstruction.code);
        console.log("case instructions:");
        for (const $caseInst of $caseStmt.instructions) {
            console.log($caseInst.code);
        }
    }
}

console.log("Test foo1");
for (const chain of Query.search("function", "foo1")
    .search("switch")
    .search("case")
    .chain()) {
    console.log("Switch line: " + chain["switch"].line);
    console.log("Case line: " + chain["case"].line);
}

// Switch attributes
console.log("foo1");
testSwitch(Query.search("function", "foo1").search("switch").first());

console.log("foo2");
testSwitch(Query.search("function", "foo2").search("switch").first());
