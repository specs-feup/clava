import Query from "@specs-feup/lara/api/weaver/Query.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

const $file = Query.search("file").first();

const $program = Query.root();
$program.addFile(ClavaJoinPoints.file("user_include.h"));
$program.addFile(ClavaJoinPoints.file("system_include.h"));
$program.addFile(ClavaJoinPoints.file("c_include.h"));

// Includes
$file.addInclude("user_include.h");
$file.addInclude("system_include.h", true);
$file.addCInclude("c_include.h");

console.log($file.code);
