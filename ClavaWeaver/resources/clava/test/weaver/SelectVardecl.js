import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $expr of Query.search("function", "main")
    .search("statement", {line: 4})
    .search("expression")) {
    console.log("#" + $expr.line + "  expr -> " + $expr.code);
    exractExprVardecl($expr);
}

function exractExprVardecl($expr) {
    console.log("\tIn exractExprVardecl  expr : " + $expr);
    console.log(
        "\tIn exractExprVardecl  expr.joinPointType : " + $expr.joinPointType
    );
    console.log("\tIn exractExprVardecl  expr.selects : " + $expr.selects);
    console.log("\tIn exractExprVardecl  expr.vardecl : " + $expr.vardecl);

    if ($expr.vardecl) {
        console.log("\t>>>> vardecl#" + $expr.line + "  " + $expr.vardecl.code);
    }

    console.log();
}
