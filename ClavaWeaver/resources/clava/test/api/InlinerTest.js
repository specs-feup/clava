"use strict";

laraImport("clava.opt.NormalizeToSubset");
laraImport("clava.code.Inliner");
laraImport("clava.opt.PrepareForInlining");

laraImport("weaver.WeaverJps");
laraImport("weaver.Query");

//setDebug(true);

// Normalize all code
NormalizeToSubset(Query.root());

// Inline calls in main
/*
for (const $call of Query.search("function", "main").search("call")) {
  const inliner = new Inliner();
  // inline() accepts an exprStmt. All calls must be inside exprStmt now
  const $callParent = $call.ancestor("exprStmt");
  if (!$callParent.instanceOf("exprStmt")) {
    println(
      `Could not inline call ${$call.name}@${$call.location}, ancestor is ${$callParent.joinPointType}`
    );
    continue;
  }
  inliner.inline($callParent);  
}
*/

const inliner = new Inliner();
inliner.inlineFunctionTree(Query.search("function", "main").first());

println(Query.search("function", "main").first().code);

// Multile successive inlines of the same function
println("# Test inline of successive calls");
for (const $call of Query.search("function", "inlineTest2").search("call")) {
  PrepareForInlining($call.function);
}

new Inliner().inlineFunctionTree(
  Query.search("function", "inlineTest2").first()
);
println(Query.search("function", "inlineTest2").first().code);

new Inliner().inlineFunctionTree(
  Query.search("function", "arrayParam").first()
);
println(Query.search("function", "arrayParam").first().code);

new Inliner().inlineFunctionTree(
  Query.search("function", "functionThatCallsFunctionThatUsesGlobal").first()
);

println(
  Query.search("function", "functionThatCallsFunctionThatUsesGlobal").first()
    .code
);

/*
const callFiltezL1 = Query.search("function", "callFiltezL1")
  .search("call")
  .first();

println("Bef:\n" + callFiltezL1.function.code);
PrepareForInlining(callFiltezL1.function);
println("Aft:\n" + callFiltezL1.function.code);

const stmtFiltezL1 = callFiltezL1.ancestor("exprStmt");

inliner.inline(stmtFiltezL1);

println(Query.search("function", "callFiltezL1").first().code);
*/
//PrepareForInlining(Query.search("function", "filtez").first());
//PrepareForInlining(Query.search("function", "callFiltezL1").first());
//PrepareForInlining(Query.search("function", "callFiltezL2").first());

/*
new Inliner().inlineFunctionTree(
  Query.search("function", "callFiltezL2").first()
);
println(Query.search("function", "callFiltezL2").first().code);
*/
