import weaver.Query;
import clava.ClavaJoinPoints;

aspectdef Loop

	select function{"main"}.loop end
	apply
		println("loop control var: " + $loop.controlVar);
		println("is innermost? " + $loop.isInnermost);
		println("is outermost? " + $loop.isOutermost);
		println("nested level: " + $loop.nestedLevel + "\n");
		println("rank: " + $loop.rank + "\n");
		//println("Loop inc:" + $loop.incrementValue);
	end
	
	
	// Test iterationsExpr
	select function{"iterationsExpr"}.loop end
	apply
		if($loop.iterationsExpr === undefined) {
			println("iterations expr: undefined");
			continue;
		}
		println("iterations expr: " + $loop.iterationsExpr.code);
		println("iterations: " + $loop.iterations);		
	end
	
	// Test insertion in for header
	var $headerInsert1 = Query.search("function", "headerInsert1").search("loop").first();
	testHeaderInsert($headerInsert1, true);
	
	var $headerInsert2 = Query.search("function", "headerInsert2").search("loop").first();
	testHeaderInsert($headerInsert2, false);

/*	
	// interchange all pairs of innermost loops and their parent loop
	select ($l1=loop).($l2=loop) end
	apply
		$l1.interchange($l2);
	end
	condition
		$l2.isInnermost
	end
	
	// interchange all pairs of innermost loops and their parent loop
	// but test if interchangeable first
	select ($l1=loop).($l2=loop) end
	apply
		$l1.interchange($l2);
	end
	condition
		$l2.isInnermost &&
		$l2.isInterchangeable($1)
	end
	
	// try to interchange two loops which are not in the same nest
	select loop end
	apply
		var $loop1 = $loop;
	end
	condition rank == '1' end
	
	select loop end
	apply
		var $loop2 = $loop;
	end
	condition rank == '2' end
	
	$loop1.interchange($loop2);
	/**/
end


function testHeaderInsert($loop, isDeclaration) {

	var newVarName1 = "newVar1";
	var newVarName2 = "newVar2";	

	// Declare new variable
	var $varDecl1 = ClavaJoinPoints.varDeclNoInit(newVarName1, ClavaJoinPoints.builtinType("int"));
	var $varDecl2 = ClavaJoinPoints.varDeclNoInit(newVarName2, ClavaJoinPoints.builtinType("int"));
	
	$loop.insertBefore($varDecl1);
	$loop.insertBefore($varDecl2);	
	
	var initCodePrefix1 = isDeclaration ? "int " + newVarName1 : newVarName1;
	var initCodePrefix2 = isDeclaration ? "int " + newVarName2 : newVarName2;	
	
	//println("Insert1: " + initCodePrefix1 + " = 10");
	//println("Insert2: " + initCodePrefix2 + " = 20");


	// Add inserts
	$loop.init.insertBefore(initCodePrefix1 + " = 10");
	$loop.init.insertAfter(initCodePrefix2 + " = 20");	

	//println($loop.cond);
	$loop.cond.insertBefore(newVarName1 + "< 100");
	$loop.cond.insertAfter(newVarName2 + "< 200");
	//$loop.cond.replaceWith("i < 1000;");
	
	$loop.step.insertBefore(newVarName1 + "++");
	$loop.step.insertAfter(newVarName2 + "--");	
	
	println("After header insert: " + $loop.parent.code);

}
