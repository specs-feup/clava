laraImport("clava.Clava");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.Io");

// Manually add header file
const headerFile = Io.getPath("cxx_weaver_output/add_header_file.h");
if (!Io.isFile(headerFile)) {
    throw new Error("Could not find header file " + headerFile);
}

const $file = ClavaJoinPoints.file(headerFile);
Clava.addFile($file, "cxx_weaver_output");

console.log($file.code);
