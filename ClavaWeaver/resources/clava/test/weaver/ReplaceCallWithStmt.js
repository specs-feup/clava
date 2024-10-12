laraImport("clava.ClavaJoinPoints");
laraImport("weaver.Query");

const $main = Query.search("function", "main").first();
for (const chain of Query.searchFrom($main.body, "statement")
    .search("call").chain()) {

    const $call = chain["call"];
    const $stmt = chain["statement"];

    const varName = $call.name + "Result";
    const $varDecl = ClavaJoinPoints.varDecl(varName, $call);
    $stmt.replaceWith($varDecl);
}

console.log(Query.search("function", "main").first().code);
