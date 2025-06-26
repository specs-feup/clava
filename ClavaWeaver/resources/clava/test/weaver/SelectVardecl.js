import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp, Statement, Expression } from "@specs-feup/clava/api/Joinpoints.js";

for (const $expr of Query.search(FunctionJp, "main")
    .search(Statement, {line: 4})
    .search(Expression)) {
    console.log("#" + $expr.line + "  expr -> " + $expr.code);

    console.log("\tIn exractExprVardecl  expr : " + $expr);
    console.log(
        "\tIn exractExprVardecl  expr.joinPointType : " + $expr.joinPointType
    );
    console.log("\tIn exractExprVardecl  expr.vardecl : " + $expr.vardecl);
    
    if ($expr.vardecl) {
        console.log("\t>>>> vardecl#" + $expr.line + "  " + $expr.vardecl.code);
    }
    
    console.log();
}