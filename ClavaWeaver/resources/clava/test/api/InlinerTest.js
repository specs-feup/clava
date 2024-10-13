"use strict";

import NormalizeToSubset from "@specs-feup/clava/api/clava/opt/NormalizeToSubset.js";
import Inliner from "@specs-feup/clava/api/clava/code/Inliner.js";
import PrepareForInlining from "@specs-feup/clava/api/clava/opt/PrepareForInlining.js";

import Query from "@specs-feup/lara/api/weaver/Query.js";

//setDebug(true);

// Normalize all code
NormalizeToSubset(Query.root());

// Inline calls in main
/*
for (const $call of Query.search("function", "main").search("call")) {
  const inliner = new Inliner();
  // inline() accepts an exprStmt. All calls must be inside exprStmt now
  const $callParent = $call.getAncestor("exprStmt");
  if (!$callParent.instanceOf("exprStmt")) {
    console.log(
      `Could not inline call ${$call.name}@${$call.location}, ancestor is ${$callParent.joinPointType}`
    );
    continue;
  }
  inliner.inline($callParent);  
}
*/

const inliner = new Inliner();
inliner.inlineFunctionTree(Query.search("function", "main").first());

console.log(Query.search("function", "main").first().code);

// Multile successive inlines of the same function
console.log("# Test inline of successive calls");
for (const $call of Query.search("function", "inlineTest2").search("call")) {
  PrepareForInlining($call.function);
}

new Inliner().inlineFunctionTree(
  Query.search("function", "inlineTest2").first()
);
console.log(Query.search("function", "inlineTest2").first().code);

new Inliner().inlineFunctionTree(
  Query.search("function", "arrayParam").first()
);
console.log(Query.search("function", "arrayParam").first().code);

new Inliner().inlineFunctionTree(
  Query.search("function", "functionThatCallsFunctionThatUsesGlobal").first()
);

console.log(
  Query.search("function", "functionThatCallsFunctionThatUsesGlobal").first()
    .code
);

for (const $call of Query.search(
  "function",
  "functionThatCallsFunctionWithReturnButsDoesNotUseResult"
).search("call")) {
  PrepareForInlining($call.function);
}

new Inliner().inlineFunctionTree(
  Query.search(
    "function",
    "functionThatCallsFunctionWithReturnButsDoesNotUseResult"
  ).first()
);

console.log(
  Query.search(
    "function",
    "functionThatCallsFunctionWithReturnButsDoesNotUseResult"
  ).first().code
);

new Inliner().inlineFunctionTree(
  Query.search("function", "functionWithNakedIf").first()
);

console.log(Query.search("function", "functionWithNakedIf").first().code);

new Inliner().inline(
  Query.search("function", "functionWhichCallIsNotDeclared")
    .search("call")
    .first()
    .getAncestor("exprStmt")
);

console.log(
  Query.search("function", "functionWhichCallIsNotDeclared").first().code
);

new Inliner().inlineFunctionTree(
  Query.search("function", "functionThatCallsOtherWithVarInStruct").first()
);

console.log(
  Query.search("function", "functionThatCallsOtherWithVarInStruct").first().code
);

new Inliner().inlineFunctionTree(
  Query.search("function", "functionThatCallsFunctionWithStruct").first()
);

console.log(
  Query.search("function", "functionThatCallsFunctionWithStruct").first().code
);

new Inliner().inlineFunctionTree(
  Query.search("function", "functionThatCallFunctionWith2DimPointer").first()
);

console.log(
  Query.search("function", "functionThatCallFunctionWith2DimPointer").first()
    .code
);

new Inliner().inlineFunctionTree(
  Query.search("function", "functionWithCallWithStatic").first()
);

console.log(Query.search("function", "functionWithCallWithStatic").first().code);

new Inliner().inlineFunctionTree(
  Query.search("function", "callsFunctionWithLabels").first()
);

console.log(Query.search("function", "callsFunctionWithLabels").first().code);

//console.log(Query.search("function", "functionWithStatic").first().ast);

/*
console.log(
  Query.search("function", "functionWith2DimPointer").search("exprStmt").first()
    .expr.type.pointee.innerType.elementType.elementType
);
*/

/*
console.log(
  Query.search("function", "functionThatCallsFunctionWithStruct")
    .search("vardecl", "__inline_0_x")
    .first().type.ast
);
*/

//console.log(Query.search("function", "functioWithStruct").search("vardecl", "x").first().type.ast)
//console.log(Query.search("function", "functioWithStruct").search("vardecl", "x").first().type.code)

//console.log(
//  Query.search("function", "functionThatCallsOtherWithVarInStruct").first().ast
//);

//console.log(Query.root().code);

/*
const callFiltezL1 = Query.search("function", "callFiltezL1")
  .search("call")
  .first();

console.log("Bef:\n" + callFiltezL1.function.code);
PrepareForInlining(callFiltezL1.function);
console.log("Aft:\n" + callFiltezL1.function.code);

const stmtFiltezL1 = callFiltezL1.getAncestor("exprStmt");

inliner.inline(stmtFiltezL1);

console.log(Query.search("function", "callFiltezL1").first().code);
*/
//PrepareForInlining(Query.search("function", "filtez").first());
//PrepareForInlining(Query.search("function", "callFiltezL1").first());
//PrepareForInlining(Query.search("function", "callFiltezL2").first());

/*
new Inliner().inlineFunctionTree(
  Query.search("function", "callFiltezL2").first()
);
console.log(Query.search("function", "callFiltezL2").first().code);
*/
