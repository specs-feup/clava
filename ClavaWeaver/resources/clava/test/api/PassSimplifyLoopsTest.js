import Clava from "@specs-feup/clava/api/clava/Clava.js";
import SimplifyLoops from "@specs-feup/clava/api/clava/pass/SimplifyLoops.js";
import StatementDecomposer from "@specs-feup/clava/api/clava/code/StatementDecomposer.js";
import DecomposeVarDeclarations from "@specs-feup/clava/api/clava/pass/DecomposeVarDeclarations.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const statementDecomposer = new StatementDecomposer();
const pass = new SimplifyLoops(statementDecomposer);

pass.apply(Query.root());

println(Query.root().code);
