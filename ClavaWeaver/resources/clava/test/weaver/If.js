laraImport("weaver.Query");

for(const $if of Query.search("function", "scope_test").search("if")) {
    console.log("then", $if.then?.joinPointType);
    console.log("else", $if.else?.joinPointType);    
}

console.log(Query.root().code);