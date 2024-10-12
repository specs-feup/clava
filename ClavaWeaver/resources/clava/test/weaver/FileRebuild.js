laraImport("weaver.Query");

// Add literal stmt
Query.search("file", "file_rebuild.h").first().insert("after", "void foo2();");

console.log("file_rebuild.h before:");
for (const $function of Query.search("file", "file_rebuild.h").search(
    "function"
)) {
    console.log("Function: " + $function.signature);
}

// Rebuild file
Query.search("file", "file_rebuild.h").first().rebuild();

console.log("file_rebuild.h after:");
for (const $function of Query.search("file", "file_rebuild.h").search(
    "function"
)) {
    console.log("Function: " + $function.signature);
}
