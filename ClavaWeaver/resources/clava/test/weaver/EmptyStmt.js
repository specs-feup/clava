import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $emptyStmt of Query.search("function", "main").search("emptyStmt")) {
    console.log("EmptyStmt: " + $emptyStmt.line);
}

console.log(Query.root().code);
