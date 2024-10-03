import clava.ClavaJoinPoints;

aspectdef Setters

	select function{"testSetQualifiedName"}.call{"now"} end
	apply
		println("Original qualified name: " + $call.declaration.qualifiedName);
		$call.declaration.qualifiedName = "now";
		println("Changed qualified name 1: " + $call.declaration.qualifiedName);
		$call.declaration.qualifiedName = "std::now";
		println("Changed qualified name 2: " + $call.declaration.qualifiedName);
		$call.declaration.qualifiedPrefix = "std::chrono::_V2::system_clock";
		println("Changed qualified name 3: " + $call.declaration.qualifiedName);
	end


	select function{"testIf"}.if end
	apply
		$if.then = ClavaJoinPoints.stmtLiteral("a = 3;");
		println("Changed then:\n" + $if.code);
		$if.else = ClavaJoinPoints.stmtLiteral("a = 4;");
		println("Changed else:\n" + $if.code);		
		$if.cond = ClavaJoinPoints.exprLiteral("a == 3");
		println("Changed condition:\n" + $if.code);				
	end
	
	
	select function{"testFunctionType"} end
	apply
		$function.returnType = ClavaJoinPoints.builtinType("double");
		$function.setParamType(0, ClavaJoinPoints.builtinType("int"));		
		println("Changed Function:\n"+$function.code);
		println("Changed FunctionType:\n"+$function.functionType.code);
	end
end
