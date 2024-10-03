laraImport("weaver.Query");

for (const $loop of Query.search("loop")) {
    for (const $childExpr of Query.searchFrom($loop.cond, "expression")) {
        if ($childExpr.joinPointType !== "binaryOp") {
            continue;
        }

        console.log("Condition Kind:" + $childExpr.kind);
    }
}
