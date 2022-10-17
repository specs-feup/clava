"use strict";

laraImport("clava.opt.NormalizeToSubset");
laraImport("clava.code.Inliner");

laraImport("weaver.WeaverJps");
laraImport("weaver.Query");

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
