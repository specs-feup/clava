import Query from "@specs-feup/lara/api/weaver/Query.js";

function printExprDecl($expr) {
    const $exprDecl = $expr.decl;
    if ($exprDecl === undefined) {
        return;
    }

    console.log("Expr: " + $expr.node.getClass());
    console.log("Decl: " + $exprDecl.node.getClass());
}

for (const $expr of Query.search("function", "expressionDecls").search(
    "expression"
)) {
    printExprDecl($expr);
}

for (const $expr of Query.search("function", "temporaryExpressionDecl").search(
    "expression"
)) {
    printExprDecl($expr);
}
