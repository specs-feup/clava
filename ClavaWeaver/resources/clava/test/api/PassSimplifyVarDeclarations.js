import Query from "@specs-feup/lara/api/weaver/Query.js";
import DecomposeVarDeclarations from "@specs-feup/clava/api/clava/pass/DecomposeVarDeclarations.js";

const result = new DecomposeVarDeclarations().apply();
console.log("Result: " + result);
console.log(Query.root().code);
