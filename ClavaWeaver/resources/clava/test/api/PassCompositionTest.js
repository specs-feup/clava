laraImport("clava.pass.DecomposeDeclStmt");
laraImport("clava.pass.SimplifySelectionStmts");
laraImport("clava.code.SimplifyAssignment");
laraImport("clava.code.StatementDecomposer");

laraImport("lara.pass.composition.Passes");

laraImport("weaver.Query");

// This type of pass is no longer supported. It is now mandatory to create a class for the pass
/*
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
*/

const passes = [
  //SimplifyAssignments,
  new SimplifySelectionStmts(new StatementDecomposer()),
  new DecomposeDeclStmt(),
  //[DummyPass, { foo: "bar" }],
];

const results = Passes.apply(Query.root(), passes);

println(Query.root().code);
