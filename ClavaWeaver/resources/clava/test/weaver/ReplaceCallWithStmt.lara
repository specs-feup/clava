import clava.ClavaJoinPoints;

aspectdef ReplaceCallWithStmt

	select function{"main"}.stmt.stmtCall end
	apply
		var varName = $stmtCall.name + "Result";
	
		var $varDecl = ClavaJoinPoints.varDecl(varName, $stmtCall);
		$stmt.exec replaceWith($varDecl);
	end
	
	
	//Clava.rebuild();

	select function{"main"} end
	apply
		print($function.code);
	end


	
end