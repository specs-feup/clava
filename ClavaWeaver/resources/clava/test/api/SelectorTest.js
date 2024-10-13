import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $chain of Query.search("function", "loops")
    .search("loop")
    .children("scope")
    .children("loop")
    .chain()) {
    console.log("Loop:\n" + $chain["loop"].init.code);
}

// Test iterator
let iteratorsStmts = 0;
for (const _ of Query.search("function", "iterators").search("statement")) {
    iteratorsStmts++;
}
console.log("Stmts in iterators: " + iteratorsStmts);
