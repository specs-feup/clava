import Clava from "@specs-feup/clava/api/clava/Clava.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

// Add file
const $newFile = ClavaJoinPoints.fileWithSource(
    "addedFile.cpp",
    "int foo() {return 0;}"
).rebuild();
Clava.addFile($newFile);
console.log("Add file:\n" + Query.search("file", "addedFile.cpp").first().code);
