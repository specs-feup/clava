import weaver.Query;

aspectdef SwitchTest

	println("Test foo1");
	for(var chain of Query.search("function", "foo1").search("switch").search("case").chain()) {
		println("Switch line: " + chain["switch"].line);
		println("Case line: " + chain["case"].line);
	}

	// Switch attributes
	println("foo1");
	testSwitch(Query.search("function", "foo1").search("switch").first());
/*
	for(var $switch of Query.search("function", "foo1").search("switch")) {
		println("hasDefaultCase: " + $switch.hasDefaultCase);
		println("getDefaultCase: " + $switch.getDefaultCase);		
		for(var $caseStmt of $switch.cases) {
			println("case is default: " + $caseStmt.isDefault);
			println("case is empty: " + $caseStmt.isEmpty);
			println("case next instruction: " + $caseStmt.nextInstruction.code);		
			println("case instructions:");
			for(var $caseInst of $case.instructions) {
				println($caseInst.code);
			}
		}
	}
*/

	println("foo2");
	testSwitch(Query.search("function", "foo2").search("switch").first());
/*	
	for(var $switch of Query.search("function", "foo2").search("switch")) {
		println("hasDefaultCase: " + $switch.hasDefaultCase);
		println("getDefaultCase: " + $switch.getDefaultCase);		
		for(var $caseStmt of $switch.cases) {
			println("case is default: " + $caseStmt.isDefault);
			println("case is empty: " + $caseStmt.isEmpty);
			println("case next instruction: " + $caseStmt.nextInstruction.code);		
			println("case instructions:");
			for(var $caseInst of $case.instructions) {
				println($caseInst.code);
			}
		}
	}
*/
end

function testSwitch($switch) {
	println("hasDefaultCase: " + $switch.hasDefaultCase);
	println("getDefaultCase: " + $switch.getDefaultCase);		
	println("condition: " + $switch.condition.code);			
	for(var $caseStmt of $switch.cases) {
		println("case is default: " + $caseStmt.isDefault);
		println("case is empty: " + $caseStmt.isEmpty);
		println("values: " + $caseStmt.values.map(v => v.code));		
		println("next case: " + ($caseStmt.nextCase !== undefined ? $caseStmt.nextCase.code : "undefined"));
		println("case next instruction: " + $caseStmt.nextInstruction.code);		
		println("case instructions:");
		for(var $caseInst of $caseStmt.instructions) {
			println($caseInst.code);
		}
	}
}
