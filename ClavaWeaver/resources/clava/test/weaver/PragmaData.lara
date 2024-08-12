import weaver.Query;
import clava.Clava;

aspectdef PragmaData

	select function{"foo"}.loop end
	apply
		println("Data:");
		printObject($loop.data);
		println("");
	end

	select function{"noData"}.loop end
	apply
		println("Empty Object:");
		printlnObject($loop.data);
	end
	
	// Insert preserves pragma
	var $loopWithPragma = Query.search("function", "insertPreservesPragma").search("loop").first();
	println("Data before insert: " + $loopWithPragma.data.a);
	$loopWithPragma.insertBefore("int a;");
	println("Data after insert: " + $loopWithPragma.data.a);	
	
	// ...also before functions
	var $loopWithPragma2 = Query.search("function", "insertPreservesPragma2").first();
	println("Data before insert: " + $loopWithPragma2.data.a);
	$loopWithPragma2.insertBefore("int global_a;");
	println("Data after insert: " + $loopWithPragma2.data.a);	

	// Setting data without pragma creates a pragma
	var $loopWithoutPragma = Query.search("function", "insertWithoutPragma").first();
	var obj = $loopWithoutPragma.data;
	obj.a = 42;
	obj.b = 43;
	$loopWithoutPragma.setData(obj);
	println("Loop without pragma after setting data: " + $loopWithoutPragma.pragmas.map(p => p.code));

	// Setting already existing data updates pragma
	var $loopWithUpdatedPragma = Query.search("function", "updatePragma").first();
	obj = $loopWithUpdatedPragma.data;
	obj.a = 200;
	$loopWithUpdatedPragma.setData(obj);
	println("Loop with updated pragma: " + $loopWithUpdatedPragma.pragmas.map(p => p.code));

	// Information persists after rebuilds
	var $loopBeforeRebuild = Query.search("function", "rebuild").first();
	println("Is parallel before set and rebuild: " + $loopBeforeRebuild.data.isParallel);
	obj = $loopBeforeRebuild.data;
	obj.isParallel = true;
	$loopBeforeRebuild.setData(obj);

/*
	var $op = Query.search("function", "dataInExpressions").search("binaryOp").first();
	$op.data.a = 1000;
	$op.data.b = 2000;	
	println("Cache before:");
	printlnObject(_CLAVA_DATA_CACHE);
	Clava.rebuild();
	println("Cache after:");
	printlnObject(_CLAVA_DATA_CACHE);	
*/

	var $loopAfterRebuild = Query.search("function", "rebuild").first();	
	println("Is parallel after rebuild: " + $loopAfterRebuild.data.isParallel);

	// Data on expression nodes
	var $op = Query.search("function", "dataInExpressions").search("binaryOp").first();
	obj = $op.data;
	obj.a = 1000;
	obj.b = 2000;
	$op.setData(obj);
	println("ExprData before push");
	printlnObject($op.data);
	Clava.pushAst();
	var $opAfterPush = Query.search("function", "dataInExpressions").search("binaryOp").first();
	obj = $opAfterPush.data;
	obj.c = 3000;
	$opAfterPush.setData(obj);
	Clava.popAst();
	var $opAfterPop = Query.search("function", "dataInExpressions").search("binaryOp").first();
	println("ExprData after pop");
	printlnObject($op.data);
	
	var $fileRebuildOp = Query.search("function", "fileRebuild").search("binaryOp").first();
	obj = $fileRebuildOp.data;
	obj.opRebuild = true;
	$fileRebuildOp.setData(obj);
	
	var $fileRebuildLoop = Query.search("function", "fileRebuild2").search("loop").first();
	obj = $fileRebuildLoop.data;
	obj.loopRebuild = true;
	$fileRebuildLoop.setData(obj);

//from nothing adds pragma; changes reflected in pragma; push/pop preserve; rebuild preserves for ones with pragma
end
