import weaver.Query;

aspectdef ExpressionDecls

	for(var $expr of Query.search("function", "expressionDecls").search("expression")) {
		printExprDecl($expr);
	}
	
	for(var $expr of Query.search("function", "temporaryExpressionDecl").search("expression")) {
		printExprDecl($expr);
	}	

end

function printExprDecl($expr) {
	var $exprDecl = $expr.decl;
	if($exprDecl === undefined) {
		return;
	}
	
	println("Expr: " + $expr.node.getClass());
	println("Decl: " + $exprDecl.node.getClass());	

}
