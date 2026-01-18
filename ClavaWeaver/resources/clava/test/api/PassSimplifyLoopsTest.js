import SimplifyLoops from "@specs-feup/clava/api/clava/pass/SimplifyLoops.js";
import StatementDecomposer from "@specs-feup/clava/api/clava/code/StatementDecomposer.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const statementDecomposer = new StatementDecomposer();
const pass = new SimplifyLoops(statementDecomposer);

pass.apply(Query.root());

console.log(Query.root().code);
