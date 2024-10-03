import clava.ClavaJoinPoints;

aspectdef Launcher

	var $floatType = ClavaJoinPoints.builtinType("float");

	select function{"cstyle_cast"}.expr end
	apply
		if($expr.astName === "CStyleCastExpr") {
			 $expr.setType($floatType); 
		}
	end

	select function{"static_cast_foo"}.expr end
	apply
		if($expr.astName === "CXXStaticCastExpr") {
			 $expr.setType($floatType); 
		}
	end
	
	select typedefDecl end
	apply
		$typedefDecl.type = $floatType;
		$typedefDecl.name = "x_float";
	end
	
	select program end
	apply
		println($program.code);
	end
	
end
