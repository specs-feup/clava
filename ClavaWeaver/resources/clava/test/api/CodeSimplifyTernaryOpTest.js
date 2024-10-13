import Clava from "@specs-feup/clava/api/clava/Clava.js";
import SimplifyTernaryOp from "@specs-feup/clava/api/clava/code/SimplifyTernaryOp.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $stmt of Query.search("exprStmt")) {
  SimplifyTernaryOp($stmt);
}

Clava.rebuild();
println(Query.root().code);
