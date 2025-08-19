import Query from "@specs-feup/lara/api/weaver/Query.js";

for(const $if of Query.search("function", "scope_test").search("if")) {
    console.log("then", $if.then?.joinPointType);
    console.log("else", $if.else?.joinPointType);    
}

console.log(Query.root().code);