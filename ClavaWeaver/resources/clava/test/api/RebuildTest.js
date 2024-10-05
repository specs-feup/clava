laraImport("clava.Clava");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.Io");
laraImport("weaver.Query");

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
