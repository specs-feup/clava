laraImport("clava.pass.DecomposeDeclStmt");
laraImport("clava.pass.SimplifySelectionStmts");
laraImport("clava.code.SimplifyAssignment");
laraImport("clava.code.StatementDecomposer");

laraImport("lara.pass.Passes");

laraImport("weaver.Query");

function SimplifyAssignments($startJp) {
	for(const $op of Query.searchFromInclusive($startJp, "binaryOp")) {
		SimplifyAssignment($op);
	}	


}


const statementDecomposer = new StatementDecomposer();


/*
const simplifyIfs = new SimplifySelectionStmts(statementDecomposer);
const declStmt = new DecomposeDeclStmt();	
SimplifyAssignments(Query.root());
simplifyIfs.apply(Query.root());
declStmt.apply(Query.root());
*/


const passes = [SimplifyAssignments, SimplifySelectionStmts, [statementDecomposer], new DecomposeDeclStmt()];
const results = Passes.apply(Query.root(), passes);    

println("Results: " + results)

println(Query.root().code);    


