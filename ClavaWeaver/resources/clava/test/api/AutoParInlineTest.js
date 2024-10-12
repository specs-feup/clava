laraImport("clava.autopar.RunInlineFunctionCalls");
laraImport("weaver.Query");
laraImport("clava.Clava");

const $call = Query.search("call", { name: "bar" }).first();
if ($call === undefined) {
    throw new Error("Could not find call to 'bar'");
}

const exprStmt = $call.getAstAncestor("ExprStmt");

const o = inlinePreparation("bar", $call, exprStmt);

if (o.$newStmts.length > 0) {
    const replacedCallStr =
        "// ClavaInlineFunction : " +
        exprStmt.code +
        "  countCallInlinedFunction : " +
        countCallInlinedFunction;
    exprStmt.insertBefore(replacedCallStr);

    for (const $newStmt of o.$newStmts) {
        exprStmt.insertBefore($newStmt);
    }

    exprStmt.detach();
}

console.log("Code:\n" + Clava.getProgram().code);

Clava.rebuild();
