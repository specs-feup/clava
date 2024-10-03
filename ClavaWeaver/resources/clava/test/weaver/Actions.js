import clava.ClavaJoinPoints;

aspectdef Actions

	select function.stmt.vardecl end
	apply
		if($vardecl.type.code === "double") {
			$vardecl.exec setType(ClavaJoinPoints.builtinType("float"));
		} else if($vardecl.type.code === "int") {
			$vardecl.exec setType(ClavaJoinPoints.builtinType("unsigned int"));
		}
	end
	
	
	select file end
	apply 
		println($file.code);
	end
	/*
	select function.decl end
	apply
		println("BEFORE:" + $decl.type.code);
		def type = ClavaJoinPoints.builtinType("int");
		println("AFTER:" + $decl.type.code);
	end
		select file end
	apply 
		println($file.code);
	end
	*/
	
	/*
	var aStmt = undefined;
	var $float = ClavaJoinPoints.builtinType("double");
	println("Float: " + $float.code);
	select expr end
	apply

		println("Equals of float :" + ($float == $float));
		println("String equals of float :" + ($float === $float));
		println("Equals of same object :" + ($expr.type == $expr.type));
		println("Strict equals of same object :" + ($expr.type === $expr.type));
		
		println("Expr type:" + $expr.type.code);
		println("==: " + ($float == $expr.type));
		println("===: " + ($float === $expr.type));
		println("equals: " + ($float.equals($expr.type)));
		println("same: " + ($float.same($expr.type)));

	end
	*/
	
		/*
			if(aStmt === undefined) {
				aStmt = $stmt;
			} else {
				println("Compare ==: " + (aStmt == $stmt));
				println("Compare ===: " + (aStmt === $stmt));
				break;
			}
			*/
	
end