laraImport("weaver.Query");

for (const $decl of Query.search("decl")) {
    console.log($decl.type.ast);
}

console.log(Query.root().code);
