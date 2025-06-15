import Clava from "@specs-feup/clava/api/clava/Clava.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Io from "@specs-feup/lara/api/lara/Io.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// Manually add header file
const headerFile = Io.getPath("cxx_weaver_output/rebuild.h");

const $file = ClavaJoinPoints.file(headerFile);
Clava.addFile($file, "cxx_weaver_output");

console.log("Stack size before push: " + Clava.getStackSize());
Clava.pushAst();
console.log("Stack size after push: " + Clava.getStackSize());

for (const $function of Query.search("function", "main")) {
    $function.insertBefore("// Hello");
}

Clava.rebuild();
console.log("Temporary code:\n" + Clava.getProgram().code);

Clava.popAst();
console.log("Stack size after pop: " + Clava.getStackSize());

// Rebuild two times, to stress test rebuild
Clava.rebuild();
Clava.rebuild();

console.log("Original code:\n" + Clava.getProgram().code);
