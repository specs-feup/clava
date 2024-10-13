import Clava from "@specs-feup/clava/api/clava/Clava.js";
import SingleReturnFunction from "@specs-feup/clava/api/clava/pass/SingleReturnFunction.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const pass = new SingleReturnFunction();
for (const $function of Query.search("function")) {
  pass.apply($function);
}

Clava.rebuild();
println(Query.root().code);
