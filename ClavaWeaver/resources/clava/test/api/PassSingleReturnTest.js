laraImport("clava.Clava");
laraImport("clava.pass.SingleReturnFunction");
laraImport("weaver.Query");

const pass = new SingleReturnFunction();
for (const $function of Query.search("function")) {
  pass.apply($function);
}

Clava.rebuild();
println(Query.root().code);
