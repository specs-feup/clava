import Clava from "@specs-feup/clava/api/clava/Clava.js";
import SimplifyAssignment from "@specs-feup/clava/api/clava/code/SimplifyAssignment.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $op of Query.search("binaryOp")) {
  SimplifyAssignment($op);
}

Clava.rebuild();
console.log(Query.root().code);
