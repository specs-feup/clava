import clava.ClavaJoinPoints;
import clava.ClavaType;

aspectdef ClavaTypeTest

	var intType = ClavaJoinPoints.builtinType("int");
	var intExpr = ClavaJoinPoints.exprLiteral("2 + 3", intType);

	// Transform the expression into a statement
	println("Expr to Stmt type: " + ClavaType.asStatement(intExpr).joinPointType);

	// Transform the expression into a scope
	println("Expr to Scope type: " + ClavaType.asScope(intExpr).joinPointType);
end
