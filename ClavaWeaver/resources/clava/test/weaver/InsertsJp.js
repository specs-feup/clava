import clava.ClavaJoinPoints;

aspectdef InsertsJp

	
	var $bDecl = ClavaJoinPoints.varDecl("b", ClavaJoinPoints.integerLiteral(10));
	var $cDecl = ClavaJoinPoints.varDecl("c", ClavaJoinPoints.integerLiteral("20"));

	select function{"fooStmtBeforeAfter"}.stmt end
	apply
		$stmt.exec insertBefore($bDecl);
		$stmt.exec insertAfter($cDecl);
	end
	
	select function{"fooStmtReplace"}.stmt end
	apply
		$stmt.exec replaceWith($bDecl);
	end

	select function{"fooBodyBeforeAfter"}.body end
	apply
		$body.exec insertBegin($bDecl);
		$body.exec insertEnd($cDecl);
	end
	
	select function{"fooBodyReplace"}.body end
	apply
		$body.exec replaceWith($bDecl);
	end
	
	select function{"fooBodyEmptyBeforeAfter"}.body end
	apply
		$body.exec insertBegin($bDecl);
		$body.exec insertEnd($cDecl);
	end
	
	select function{"fooBodyEmptyReplace"}.body end
	apply
		$body.exec replaceWith($bDecl);
	end
	
	select function{"fooCallBeforeAfter"}.stmt.call end
	apply
		$call.exec insertBefore($bDecl);
		$call.exec insertAfter($cDecl);
	end
	

	var $double2 = ClavaJoinPoints.doubleLiteral(2.0);
	var $double3 = ClavaJoinPoints.doubleLiteral("3.0");
	var $doubleType = ClavaJoinPoints.builtinType("double");
	var $powCall = ClavaJoinPoints.callFromName("pow", $doubleType, $double2, $double3);
	
	select function{"fooCallReplace"}.stmt.call end
	apply
		$call.exec replaceWith($powCall);
	end
	
	select function{"fooBeforeAfter"} end
	apply
		$function.exec insertBefore($bDecl);
		$function.exec insertAfter($cDecl);
	end
	
	var $dDecl = ClavaJoinPoints.varDecl("d", ClavaJoinPoints.integerLiteral(30));
	select function{"fooReplace"} end
	apply
		$function.exec replaceWith($dDecl);
	end
	
	
	
	// Output code
	select file end
	apply
		println($file.code);
	end
end