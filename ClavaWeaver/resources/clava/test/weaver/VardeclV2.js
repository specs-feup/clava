import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const chain of Query.search("function", "removeInit").search(
    "vardecl",
    "array1"
).chain()) {
    const $function = chain["function"];
    const $vardecl = chain["vardecl"];

    $vardecl.removeInit();
    console.log("After removing init:\n" + $function.code);
}

const globalRef = Query.search("function", "externalVar")
    .search("varref")
    .first();
console.log(globalRef.decl.definition.location);

const $vardeclToVarref = Query.search("function", "varref")
    .search("vardecl")
    .first();
console.log("Is varref: " + $vardeclToVarref.varref().instanceOf("varref"));
