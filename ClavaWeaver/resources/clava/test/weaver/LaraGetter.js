import Clava from "@specs-feup/clava/api/clava/Clava.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

const descendants = Clava.getProgram().descendants;
// descendants() does not work in newer versions of GraalVM
// For some reason, laraGetter is not being used in that case
//var descendants2 = Clava.getProgram().getDescendants('vardecl');

const filename = Clava.isCxx() ? "dummy.cpp" : "dummy.c";

const $app = Clava.getProgram();
const $file = ClavaJoinPoints.file(filename, "lib");
$file.insertBefore("// Hello");

$app.addFile($file);

$app.firstChild.relativeFolderpath = $app.firstChild.relativeFolderpath;
$app.firstChild.getFirstJp("comment").text = "hello 2";

const obj = { i: 10 };
obj.i++;
obj.i--;

console.log($app.code);
