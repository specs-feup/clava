laraImport("clava.pass.DecomposeDeclStmt");
laraImport("clava.pass.SimplifySelectionStmts");
laraImport("clava.code.SimplifyAssignment");
laraImport("clava.code.StatementDecomposer");

laraImport("lara.pass.composition.Passes");

laraImport("weaver.Query");

function SimplifyAssignments($startJp) {
  for (const $op of Query.searchFromInclusive($startJp, "binaryOp")) {
    SimplifyAssignment($op);
  }
}

function DummyPass($startJp, options) {
  println(
    "Dummy pass that has received an option object with the value '" +
      options["foo"] +
      "' for the key 'foo'"
  );
}

const statementDecomposer = new StatementDecomposer();

/*
const simplifyIfs = new SimplifySelectionStmts(statementDecomposer);
const declStmt = new DecomposeDeclStmt();	
SimplifyAssignments(Query.root());
simplifyIfs.apply(Query.root());
declStmt.apply(Query.root());
*/

const passes = [
  SimplifyAssignments,
  [SimplifySelectionStmts, statementDecomposer],
  new DecomposeDeclStmt(),
  [DummyPass, { foo: "bar" }],
];

const results = Passes.apply(Query.root(), passes);

//println("Results: " + results)

println(Query.root().code);
