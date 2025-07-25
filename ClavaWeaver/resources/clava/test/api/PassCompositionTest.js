import DecomposeDeclStmt from "@specs-feup/clava/api/clava/pass/DecomposeDeclStmt.js";
import SimplifySelectionStmts from "@specs-feup/clava/api/clava/pass/SimplifySelectionStmts.js";
import StatementDecomposer from "@specs-feup/clava/api/clava/code/StatementDecomposer.js";

import Passes from "@specs-feup/lara/api/lara/pass/composition/Passes.js";

import Query from "@specs-feup/lara/api/weaver/Query.js";

// This type of pass is no longer supported. It is now mandatory to create a class for the pass
/*
function SimplifyAssignments($startJp) {
  for (const $op of Query.searchFromInclusive($startJp, "binaryOp")) {
    SimplifyAssignment($op);
  }
}

function DummyPass($startJp, options) {
  console.log(
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

console.log(Query.root().code);
