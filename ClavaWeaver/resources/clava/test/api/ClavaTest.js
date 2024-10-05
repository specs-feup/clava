laraImport("clava.Clava");
laraImport("weaver.Query");
laraImport("clava.ClavaJoinPoints");

// Add file
const $newFile = ClavaJoinPoints.fileWithSource(
    "addedFile.cpp",
    "int foo() {return 0;}"
).rebuild();
Clava.addFile($newFile);
console.log("Add file:\n" + Query.search("file", "addedFile.cpp").first().code);
