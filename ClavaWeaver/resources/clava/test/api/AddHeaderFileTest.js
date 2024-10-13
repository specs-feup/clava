import Clava from "@specs-feup/clava/api/clava/Clava.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Io from "@specs-feup/lara/api/lara/Io.js";

// Manually add header file
const headerFile = Io.getPath("cxx_weaver_output/add_header_file.h");
if (!Io.isFile(headerFile)) {
    throw new Error("Could not find header file " + headerFile);
}

const $file = ClavaJoinPoints.file(headerFile);
Clava.addFile($file, "cxx_weaver_output");

console.log($file.code);
