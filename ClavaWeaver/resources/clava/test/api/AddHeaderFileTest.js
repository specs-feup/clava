import Clava from "@specs-feup/clava/api/clava/Clava.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Io from "@specs-feup/lara/api/lara/Io.js";
import path from "path";

const referenceFolder = Clava.getWeavingFolder();
const clavaFolder = path.dirname(path.dirname(path.dirname(referenceFolder)));
const headerFilePath = path.join(
    clavaFolder,
    "ClavaWeaver",
    "resources",
    "clava",
    "test",
    "api",
    "cpp",
    "src",
    "add_header_file.h"
);

const headerFile = Io.getPath(headerFilePath);

if (!Io.isFile(headerFile)) {
    throw new Error("Could not find header file " + headerFile);
}

// Manually add header file
const $file = ClavaJoinPoints.file(headerFile);
Clava.addFile($file);

console.log($file.code);
