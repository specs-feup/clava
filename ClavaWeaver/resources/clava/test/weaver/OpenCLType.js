import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $decl of Query.search("decl")) {
    console.log($decl.type.ast);
}

console.log(Query.root().code);
