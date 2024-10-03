laraImport("weaver.Weaver");
laraImport("clava.Clava");
laraImport("clava.ClavaJoinPoints");

const descendants = Clava.getProgram().descendants;
// descendants() does not work in newer versions of GraalVM
// For some reason, laraGetter is not being used in that case
//var descendants2 = Clava.getProgram().getDescendants('vardecl');

const $app = Clava.getProgram();
const $file = ClavaJoinPoints.file("dummy.c", "lib");
$file.insertBefore("// Hello");

$app.addFile($file);

$app.firstChild.relativeFolderpath = $app.firstChild.relativeFolderpath;
$app.firstChild.getFirstJp("comment").text = "hello 2";

const obj = { i: 10 };
obj.i++;
obj.i--;

console.log($app.code);
