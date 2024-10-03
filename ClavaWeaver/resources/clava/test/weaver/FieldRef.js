laraImport("weaver.Query");

const cl = Query.search("class", "Foo3").first();
console.log("CLASS: " + cl.ast);
console.log("Number of Fields:");
console.log(Query.searchFrom(cl, "field").get().length);
console.log(Query.searchFromInclusive(cl, "field").get().length);
console.log("Number of Classes:");
console.log(Query.searchFrom(cl, "class").get().length);
console.log(Query.searchFromInclusive(cl, "class").get().length);
