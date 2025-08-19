import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $vardecl of Query.search("function", "constantArrays").search(
    "vardecl"
)) {
    console.log($vardecl.name + " dims: " + $vardecl.type.arrayDims);
    console.log($vardecl.name + "array size: " + $vardecl.type.arraySize);
}
