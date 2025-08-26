import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $loop of Query.search("function", "loopExprStmt").search("loop")) {
  console.log(`Loop ${$loop.line} cond: ${$loop.cond.code}`);
  if ($loop.step !== undefined) {
    console.log(`Loop ${$loop.line} step: ${$loop.step.code}`);
  }
}
