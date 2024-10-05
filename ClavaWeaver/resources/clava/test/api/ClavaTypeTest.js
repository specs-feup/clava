laraImport("clava.ClavaJoinPoints");
laraImport("clava.ClavaType");

const intType = ClavaJoinPoints.builtinType("int");
const intExpr = ClavaJoinPoints.exprLiteral("2 + 3", intType);

// Transform the expression into a statement
console.log(
    "Expr to Stmt type: " + ClavaType.asStatement(intExpr).joinPointType
);

// Transform the expression into a scope
console.log("Expr to Scope type: " + ClavaType.asScope(intExpr).joinPointType);
